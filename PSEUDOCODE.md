# Middleware Pseducode

This section describes in a python-like pseudo-code the full workings of this middleware server.

Valid as of commit `f35c923389aac8453d17e2d4e98535d261caa825`

- `filtering.py`:

```python 
fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))

def getClassProtectedFields(className, scopes):
  fields = fieldsCsv.select("field")
              .whereEquals("class", className)
              .whereEquals("protection", "protected")
              .whereIncluded("access_scope", scopes)
  return set(fields)

def getClassPublicFields(className):
  fields = fieldsCsv.select("field")
              .whereEquals("class", className)
              .whereEquals("protection", "public")
  return set(fields)

def umaResourcesToGrantedFields(umaResources, className):
  grantsUmaScopeMap = {
    resource["resource_id"]: set(resource["resource_scopes"])
    for resource in umaResources
  }

  umaGrantedFieldsMap = {
    umaId: getClassProtectedFields(className, scopes) + 
              getClassPublicFields(className)
    for umaId, scopes in enumerate(grantsUmaScopeMap)
  }
  # access only public fields when access is denied
  umaGrantedFieldsMap[None] = getClassPublicFields(className)

  return umaGrantedFieldsMap

def filterResourceRecursive(resource, scopes):
  splitScopes = [ 
    (s.split(".", 1)[0], s.split(".", 1)[1]) for s in scopes 
  ]
  collapsedScopes = {
    topField: set()
    for (topField, rest) in splitScopes
  }
  for (topField, rest) in splitScopes:
    collapsedScopes[topField].add(rest)
  
  for k, v in enumerate(resource):
    if not k in collapsedScopes:
      del resource[k]
      continue

    innerFields = collapsedScopes[k]
    if innerFields.empty():
      continue

    if type(v) is list:
      for nested in v:
        filterResourceRecursive(nested, innerFields) 
      for i, nested in v:
        if nested.empty():
          del resource[k][i]

    else if type(v) is dict:
      filterResourceRecursive(v, innerFields)
      if v.empty():
        del resource[k]

    else:
      log.error("bad CSV config")
      del resource[k]

def streamFilteredResources(adcResponseStream, umaGrantedFieldsMap, responseResourcesName, resourceUmaIdGetter):
  responseStream = createResponseJsonStream()
  responseStream.startObject()

  for field, valueStream in adcResponseStream.streamObject():
    if field == "Info":
      responseStream.sendField("Info", valueStream.read())

    if field == responseResourcesName:
      responseStream.startArray(responseResourcesName)

      for resource in valueStream.streamList():
        umaId = resourceUmaIdGetter(resource)
        validFields = umaGrantedFieldsMap[umaId] if umaId in umaGrantedFieldsMap else umaGrantedFieldsMap[None]
        filterResourceRecursive(resource, validFields)
        if not resource.empty():
          responseStream.sendElement(resource)

      responseStream.endArray()

  responseStream.endObject()
  responseStream.close()
```

- `synchronize.py`:

```python 

def getAllScopes():
  fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))
  scopes = fieldsCsv.select("access_scope")
  return set(scopes)

# POST /v1/synchronize
def synchronize(password):
  # step 1, check password
  syncPassHash = config.load("app.synchronizePasswordHash")
  if BCrypt.hash(password, 10) != syncPassHash:
    raise "Invalid password" 

  repository = AdcClient("http://repository:80/airr/v1")
  uma = UmaClient("http://keycloak:8082")
  db = DbRepository("http://postgres:5432")
  cache = Cache("http://redis:6379")
  allUmaScopes = getAllScopes()

  # step 2, delete cache
  cache.deleteAll()
  db.deleteStudyRepertoireMappings()

  # step 3, synchronize studies
  body = {
    "fields": [
      "repertoire_id", "study.study_id", "study.study_title"
    ]
  }
  repertoires = repository.getRepertoires(body)
  repositoryStudies = { 
    rep["study"]["study_id"]: rep["study"]["study_title"] for rep in repertoires
  }
  repositoryStudyIds = set(repositoryStudies.keys())
  umaIds = set(uma.listUmaResourceIds())
  
  # 3. (a), delete DB studies not in repository
  for studyId in set(db.studyIds()) - repositoryStudyIds:
    db.deleteUmaStudyMapping(studyId)

  # 3. (b), delete UMA resources not in DB
  for umaId in umaIds - set(db.umaIds()):
    uma.deleteResource(umaId)

  # 3. (c), delete DB studies not in UMA server
  for umaId in set(db.umaIds()) - umaIds:
    studyId = db.getStudy(umaId)
    db.deleteUmaStudyMapping(studyId)

  # 3. (d), create UMA and DB studies not in repository
  for studyId in repositoryStudyIds - set(db.studyIds()):
    umaResource = {
      "name": "study ID: " + studyId + "; title: " + repositoryStudies[studyId],
      "type": "study",
      "resource_scopes": allUmaScopes,
      "ownerManagedAccess" : True,
      "owner": config.load("uma.resourceOwner")
    }

    createdUmaId = uma.createResource(umaResource)["_id"]
    db.addUmaStudyMapping(createdUmaId, studyId)
  
  # 3. (e), validate UMA studies in repository
  for umaId in umaIds - set(db.umaIds()):
    umaResource = uma.getResource(umaId)
    actualScopes = set([
      e["name"] for e in umaResource["resource_scopes"] 
    ])
    
    if not actualScopes.contains(allUmaScopes):
      updateUmaResource = {
        "name": umaResource["name"],
        "resource_scopes": actualScopes + allUmaScopes # no duplicates
      }
      uma.updateResource(umaId, updateUmaResource)

  # step 4, synchronize repertoires
  for repertorie in repertoires:
    db.addStudyRepertoireMapping(
      repertoire["study"]["study_id"],
      repertoire["repertoire_id"]
    )
```

- `individual_endpoints.py`:

```python
from filtering import umaResourcesToGrantedFields, streamFilteredResources

repository = AdcClient("http://repository:80/airr/v1")
uma = UmaClient("http://keycloak:8082")
db = DbRepository("http://postgres:5432")
cache = Cache("http://redis:6379")

def getClassScopes(className):
  fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))
  scopes = fieldsCsv.select("access_scope")
              .whereEquals("class", className)
  return set(scopes)

def validateToken(rptToken, umaIdsProducer, scopes):
  if rptToken == None:
    resources = [
      {
        "resource_id": umaId, 
        "resource_scopes": scopes
      } for umaId in umaIdsProducer()
    ]

    ticket = uma.requestPermissionTicket(resources)['ticket']
    raise (401, { # emits permission ticket to the user
      'WWW-Authenticate:': 'UMA as_uri="http://keycloak:8082", ticket="' + ticket + '"'
    })
    
  introspect = uma.introspectToken(rptToken)
  if not introspect['active']:
    raise 401

  return resource["resource_scopes"]

def getRearrangementUmaId(rearrangementId):
  repertoireId = cache.getRearrangementRepertoireId(rearrangementId):
  if repertoireId == None:
    repertoireId = repository.getRearrangement(rearrangementId)["repertoire_id"]
    cache.storeRearrangementRepertoireMapping(rearrangementId, repertoireId)

  return db.getRepertoireIdUmaId(repertoireId)

# GET /v1/repertoire/:id
def getRepertoire(id, rptToken)
  umaId = db.getRepertoireIdUmaId(id)
  if umaId == None:
    raise 404

  tokenUmaResources = validateToken(rptToken, lambda: [umaId], getClassScopes("Repertoire"))
  streamFilteredResources(
    repository.getRepertoireStream(id), 
    umaResourcesToGrantedFields(tokenUmaResources, "Repertoire"), 
    "Repertoire", 
    lambda rep: db.getStudyIdUmaId(rep["study"]["study_id"]) 
  )

# GET /v1/rearrangement/:id
def getRearrangement(id, rptToken)
  umaId = getRearrangementUmaId(id)
  if umaId == None:
    raise 404

  tokenUmaResources = validateToken(rptToken, lambda: [umaId], getClassScopes("Repertoire"))
  streamFilteredResources(
    repository.getRearrangementStream(id), 
    umaResourcesToGrantedFields(tokenUmaResources, "Rearrangement"), 
    "Rearrangement", 
    lambda rearr: getRepertoireUmaId(rearr["repertoire_id"]) 
  )
```

- `search.py`:

```python
from filtering import umaResourcesToGrantedFields, streamFilteredResources
from individual_endpoints import validateToken

repository = AdcClient("http://repository:80/airr/v1")
uma = UmaClient("http://keycloak:8082")
db = DbRepository("http://postgres:5432")
cache = Cache("http://redis:6379")
fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))

def getIncludeFields(className, includeFields):
  includes = None
  if includeFields == "miairr":
    includes = set("miairr")
  else if includeFields == "airr-core":
    includes = set("miairr", "airr-core") 
  else if includeFields == "airr-schema":
    includes = set("miairr", "airr-core", "airr-schema")
  else 
    return set()

  fields = fieldsCsv.select("field")
              .whereEquals("class", className)
              .whereIncluded("include_fields", includes)
  return set(fields)

def getFieldsScopes(className, fields):
  scopes = fieldsCsv.select("access_scope")
              .whereEquals("class", className)
              .whereEquals("protection", "protected")
              .whereIncluded("field", fields)
  return set(scopes)

def getClassFields(className):
  fields = fieldsCsv.select("field")
              .whereEquals("class", className)
  return set(fields)

def buildSearchRequestFields(className, requestBody):
  allRequestedFields = set(requestBody["fields"]) + getIncludeFields(className, requestBody["include_fields"])
  if allRequestedFields.empty():
    allRequestedFields = getClassFields(className)

def postValidateToken(requestBody, allRequestedFields, rptToken, className, resourceIdField, resourceIdsProducer):
  requestedScopes = getFieldsScopes(className, allRequestedFields)
  if requestedScopes.empty(): # public fields access
    return []

  idsRequest = {
    "fields": [resourceIdField],
    "filters": requestBody["filters"],
    "from": requestBody["from"],
    "size": requestBody["size"],
  }

  return validateToken(rptToken, lambda: resourceIdsProducer(idsRequest), requestedScopes)

# requestBody and grantedFieldsMap are both modified by reference
def whitelistSearch(requestBody, grantedFieldsMap, allRequestedFields, resourceIdField, allRequestedFields):
  idFieldAlreadyRequested = allRequestedFields.contains(resourceIdField) or allRequestedFields.empty()
  if not idFieldAlreadyRequested:
    requestBody["fields"].add(resourceIdField)

  for _, fields in enumerate(grantedFieldsMap):
    fields.remove(fields - allRequestedFields) # set intersection

def getRepertoireUmaIds(requestBody):
  repertoires = repository.searchRepertoires(requestBody)["Repertoire"]
  studyIds = [ rep["study"]["study_id"] for rep in repertoires ]
  return [ db.getStudyUmaId(studyId) for studyId in studyIds ]

def getRearrangementUmaIds(requestBody):
  rearrangements = repository.searchRearrangements(requestBody)["Rearrangement"]
  repertoireIds = [ rea["repetoire_id"] for rea in rearrangements ]
  return [ db.getRepertoireUmaId(repId) for repId in repertoireIds ]

# POST /v1/repertoire, not including facets
def searchRepertoires(requestBody, rptToken):
  validateAdcRequest(requestBody)
  if "facets" in requestBody:
    raise "error"

  allRequestedFields = buildSearchRequestFields("Repertoire", requestBody)
  tokenUmaResources = postValidateToken(requestBody, allRequestedFields, rptToken, "Repertoire", "study.study_id", getRepertoireUmaIds)
  grantedFieldsMap = umaResourcesToGrantedFields(tokenUmaResources, "Repertoire")
  whitelistSearch(requestBody, grantedFieldsMap, allRequestedFields, "study.study_id", allRequestedFields)

  streamFilteredResources(
    repository.searchRepertoiresStream(requestBody), 
    grantedFieldsMap, 
    "Repertoire", 
    lambda rep: db.getStudyIdUmaId(rep["study"]["study_id"]) 
  )

# POST /v1/repertoire, not including facets
def searchRepertoires(requestBody, rptToken):
  validateAdcRequest(requestBody)
  if "facets" in requestBody:
    raise "error"

  allRequestedFields = buildSearchRequestFields("Rearrangement", requestBody)
  tokenUmaResources = postValidateToken(requestBody, allRequestedFields, rptToken, "Rearrangement", "repertoire_id", getRearrangementUmaIds)
  grantedFieldsMap = umaResourcesToGrantedFields(tokenUmaResources, "Rearrangement")
  whitelistSearch(requestBody, grantedFieldsMap, allRequestedFields, "repertoire_id", allRequestedFields)

  streamFilteredResources(
    repository.searchRearrangementsStream(requestBody), 
    grantedFieldsMap, 
    "Rearrangement", 
    lambda rearr: getRepertoireUmaId(rearr["repertoire_id"]) 
  )
```

- `facets.py`:

```python
from filtering import umaResourcesToGrantedFields, streamFilteredResources
from individual_endpoints import validateToken
from search import postValidateToken, getRepertoireUmaIds, getRearrangementUmaIds

repository = AdcClient("http://repository:80/airr/v1")
uma = UmaClient("http://keycloak:8082")
db = DbRepository("http://postgres:5432")
cache = Cache("http://redis:6379")
fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))

def facetsValidateToken(requestBody, rptToken, className, resourceIdField, resourceIdsProducer):
  idsRequest = {
    "fields": [resourceIdField],
    "filters": requestBody["filters"],
    "from": requestBody["from"],
    "size": requestBody["size"],
  }

  allRequestedFields = set(requestBody["fields"]) + getIncludeFields(className, requestBody["include_fields"])
  if allRequestedFields.empty():
    allRequestedFields = getClassFields(className)

  requestedScopes = getFieldsScopes(className, allRequestedFields)
  if requestedScopes.empty(): # public fields access
    return ([], allRequestedFields)

  tokenUmaResources = validateToken(rptToken, lambda: resourceIdsProducer(idsRequest), requestedScopes)
  return (tokenUmaResources, allRequestedFields)

# requestBody will be modified by reference
def addFacetsFilter(requestBody, resourceId, ids):
  inFilter = {
    "op": "in",
    "content": {
      "field": resourceId,
      "value": ids
    }
  }

  if "filters" in requestBody:
    andFilter = {
      "op": "and",
      "content": [
        inFilter,
        requestBody["filters"]    
      ]
    }
    requestBody["filters"] = andFilter
  else:
    requestBody["filters"] = inFilter

# POST /v1/repertoire, including facets
def repertoiresFacets(requestBody, rptToken):
  validateAdcRequest(requestBody)
  if "facets" not in requestBody or "fields" in requestBody or "include_fields" in requestBody:
    raise "error"

  requestedField = requestBody["facets"]
  tokenUmaResources = postValidateToken(requestBody, [ requestedField ], rptToken, "Repertoire", "study.study_id", getRepertoireUmaIds)
  if not tokenUmaResources.empty(): # non public field
    grantedFieldsMap = umaResourcesToGrantedFields(tokenUmaResources, "Repertoire")
    grantedUmaIds = [
      umaId for umaId, fields in enumerate(grantedFieldsMap) if 
          umaId != None and requestedField in fields
    ]

    nestedStudies = [db.getUmaResourceStudies(umaId) for umaId in grantedUmaIds]
    grantedStudies = [item for sublist in nestedStudies for item in sublist] # flatten

    addFacetsFilter(requestBody, "study.study_id", grantedStudies)

  return repository.repertoiresFacets(requestBody)

# POST /v1/rearrangement, including facets
def rearrangementsFacets(requestBody, rptToken):
  validateAdcRequest(requestBody)
  if "facets" not in requestBody or "fields" in requestBody or "include_fields" in requestBody:
    raise "error"

  requestedField = requestBody["facets"]
  tokenUmaResources = postValidateToken(requestBody, [ requestedField ], rptToken, "Rearrangement", "repertoire_id", getRearrangementUmaIds)
  if not tokenUmaResources.empty(): # non public field
    grantedFieldsMap = umaResourcesToGrantedFields(tokenUmaResources, "Rearrangement")
    grantedUmaIds = [
      umaId for umaId, fields in enumerate(grantedFieldsMap) if 
          umaId != None and requestedField in fields
    ]

    nestedRepertoires = [db.getUmaResourceRepertoire(umaId) for umaId in grantedUmaIds]
    grantedRepertoires = [item for sublist in nestedRepertoires for item in sublist] # flatten

    addFacetsFilter(requestBody, "repertoire_id", grantedRepertoires)

  return repository.rearrangementsFacets(requestBody)
```

- `public_fields.py`:

```python
from filtering import getClassPublicFields

fieldsCsv = csv.loadFile(config.load("app.adcCsvConfigPath"))

# GET /v1/public_fields
def public_fields():
  return {
    "Repertoire": getClassPublicFields("Repertoire"),
    "Rearrangement": getClassPublicFields("Rearrangement")
  }
```