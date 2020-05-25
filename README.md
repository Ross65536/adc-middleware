# ADC auth extensions middleware

[![pipeline status](https://gitlab.com/Ross65536/adc-middleware/badges/master/pipeline.svg)](https://gitlab.com/Ross65536/adc-middleware/commits/master)

Check out Docker image [here](https://hub.docker.com/repository/docker/ros65536/adc-middleware)

Middleware server for handling UMA authorization and access control.

Project runs on java 11, with (modified) google java style guide.

Features:
- Support for all of the AIRR ADC API functionalities except for: `tsv` format on POST endpoints, `include_fields` ADC query field and deprecation of Rearrangement's `rearrangement_id` in favour of `sequence_id`. 
- Response fields filtering based on provided token access level
- Emission of UMA tickets restricted to the fields requested

## Deployment

### Example Deployment

1. Load a resource server backend:

    - Start turnkey backend, [from here](https://github.com/sfu-ireceptor/turnkey-service-php):
    
        ```shell script
        # folder name should be 'turnkey-service-php', important for finding correct docker network. 
        git clone https://github.com/sfu-ireceptor/turnkey-service-php.git  
        cd turnkey-service-php
        echo "API_TAG=master" > .env # this should load the latest api
        scripts/install_turnkey.sh
        ```
        
        > If you use a different folder or network for the backend (docker network ls) you need to update the file's `./data/config/docker-compose.example.yml` values `turnkey-service_default` to the network used.
        
        > If you are using a different service name than `ireceptor-api` for the API backend you need to update `data/config/example.properties`'s `adc.resourceServerUrl` property.

    - Load some data, based [on](https://github.com/sfu-ireceptor/dataloading-curation):
    
        follow the instructions to load some data.
        > TODO add specific instructions

2. Either build or download adc-middleware image:
    
    To download skip this step
    
    ```shell script
    # build
    docker build -t ros65536/adc-middleware:latest .
    ```

3. Setup and configure keycloak server:

    ```shell script
    docker-compose --file docker-compose.example.yml up keycloak_db
    docker-compose --file docker-compose.example.yml up keycloak
    ```

    Keycloak is accessible at `http://localhost:8082/`.

    See instructions below on how to configure or skip setup if keycloak was already configured for development. 
    
    Make note of the generated client secret (`$MIDDLEWARE_UMA_CLIENT_SECRET`).

4. Run the middleware:

    ```shell script
    docker-compose --file docker-compose.example.yml up middleware-db middleware-redis
    MIDDLEWARE_UMA_CLIENT_SECRET=<the client secret from previous step> docker-compose --file docker-compose.example.yml up middleware 
    ```
    
    You can now make requests to `http://localhost:8080/airr/v1/`. Try with `http://localhost:8080/airr/v1/info` to see if there is a connection to the backend. 
    
    On boot the middleware server automatically connects to the DB.
    
5. Synchronize middleware cache:

    ```shell script
    # '12345abcd' is the password
    curl --location --request POST 'localhost:8080/airr/v1/synchronize' --header 'Authorization: Bearer 12345abcd'
    ```
    
    See below for a discussion on when to re-synchronize.

#### Deployment Notes

> **Important**: When deploying it's very important to make the backend's API unavailable to the public (for the turnkey example, delete the exposed ports in the `scripts/docker-compose.yml` file's `ireceptor-api` service)

> **Important**: You must generate a new password and hash for the `app.synchronizePasswordHash` property variable, see below how. 

> **Important**: The middleware APIs should be under a SSL connection in order not to leak user credentials or synchronization password.

### Docker image

The docker image for the middleware accepts the following environment variables:

- `CLIENT_SECRET`: The UMA client secret for the middleware.
- `DB_PASSWORD`: The DB password for the middleware
- `PROPERTIES_PATH`: The path for the java properties configuration file. 

The remaining configuration is done using java properties (for example see `data/config/example.properties`, for explanation see below).

## Instructions

### Dev Setup:

> If using OpenJDK, use minimum of v11.0.7

#### Configuring keycloak

You need to setup and configure a keycloak server.

Run:
```shell script
docker-compose --file docker-compose.dev.yml up keycloak_db
docker-compose --file docker-compose.dev.yml up keycloak
```

Then configure keycloak:

1. Go to `http://localhost:8082`. Login as admin with `admin:admin`. 
2. Go to `master` realm settings and enable `User-Managed Access` in the `General` tab.
3. Create a new client in the `Clients` tab: load (import) and save the client from the file `./keycloak/adc-middleware.json`. Go to credentials tab in the client and note the generated `Secret` value which is the client secret while `adc-middleware` is the client ID.
4. In the `Users` tab create user with username `owner`, this is the resource owner. Create user with username `user`, this is the user that will access resources. For each created user in the user's `Credentials` tab create the password (equal to username). 
A user can then login on `http://localhost:8082/auth/realms/master/account` (for example login as owner to grant accesses to users).

You can use different values for these strings, but you would need to update the configuration variables.


#### To install, build and run for development:

```shell script
./gradlew bootRun
```

With arguments:

```shell script
./gradlew bootRun --args='--server.port=9999' # --server.port equivalent to java's -Dserver.port 
```

#### Dev properties configuration example

file `dev.properties` (You need to update `uma.clientSecret`):

```
adc.resourceServerUrl=http://localhost:80/airr/v1
server.port=8080

# password 'master'
app.synchronizePasswordHash=$2a$10$qr81MrxWblqZlMAt5kf/9.xdBPubtDMuoV3DRKgTk2bu.SPazsaTm

# UMA
uma.wellKnownUrl=http://localhost:8082/auth/realms/master/.well-known/uma2-configuration
uma.clientId=adc-middleware
uma.clientSecret=<the generated client secret from keycloak>
uma.resourceOwner=owner

# Postgres
spring.datasource.url=jdbc:postgresql://localhost:5432/middleware_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.platform=postgres

#redis
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```


#### Dev run example

```
docker-compose --file docker-compose.dev.yml up
./gradlew bootRun --args='--spring.config.location=classpath:/application.properties,./dev.properties'
```


### Build deployable jar

The jar uses java 11

```shell script
./gradlew bootJar # jar will be placed in ./build/libs/ 
```

### Style checks

To run style checker run:

```shell script
./gradlew clean
./gradlew checkstyleMain
```

#### Tests

```shell script
./gradlew test
```

#### Pushing docker image

Dockerhub has setup a hook to automatically pull and build images from repository commits that are tagged like `v1.0.1` using semantic versioning.

```shell script
git tag -a v<VERSION> -m <MESSAGE> # tak latest commit
git push origin --tags # This should trigger a build in dockerhub
```

### Configuration

#### Middleware Configuration

You can set these by either adding a custom properties file (using `--spring.config.location` to inject the file, see example below) or by passing them as CLI options (with `-D<property>=<value>`). In the properties files you can use the field names directly as displayed here, for the CLI prepend `-D`, for the gradle CLI prepend `--` (see above example).

Required:
- `adc.resourceServerUrl`: The url to the underlying resource server (ADC backend) including base path (example `http://localhost:80/airr/v1`).
- `uma.wellKnownUrl`: The url to the keycloak server's UMA well known document (example: `http://localhost:8082/auth/realms/master/.well-known/uma2-configuration`)
- `uma.clientId`: Client ID for this middleware in keycloak
- `uma.clientSecret`: Client Secret for the client ID
- `uma.resourceOwner`: The Keycloak username who will be the owner of the created resources.
- `spring.datasource.url`: The url to the DB
- `spring.datasource.username`: DB username
- `spring.datasource.password`: DB password
- `spring.datasource.platform`: The platform. Omit for H2 DB, set to `postgres` for PostgreSQL DB.
- `app.synchronizePasswordHash`: The sha256 hash of the password protecting the synchronization endpoint. See below how to generate.

Optional:
- `server.servlet.context-path`: The base path of the middleware API. Defaults to: `/airr/v1`
- `server.port`: The middleware server port, defaults to `80`
- `app.adcCsvConfigPath`: The path for the CSV config file containing the custom fields configuration. Example `./field-mapping.csv`. Defaults to the file `src/main/resources/field-mapping.csv`. See below for structure of file.
- `app.facetsEnabled`: Boolean, indicates whether the resource server supports `facets` (and by extension ADC `filters`). Defaults to `true`.
- `app.publicEndpointsEnabled`: Boolean, indicates whether the resource server supports the public ADC endpoints (`/`, `/info`, `/swagger`). Defaults to `true`.

Optional Dev:
- (H2 only) `spring.h2.console.enabled`: Will enable H2 web console on `http://localhost:8080/airr/v1/h2-console` (default with url `jdbc:h2:file:./data/db` account `sa:password`). Defaults to false.

> Pay attention to spaces, a space at the end of a property value line will be included in the string

Running with custom properties file (using deployment jar):

```shell script
# ./config.properties is the custom file, in the current working directory
# MAKE sure to also include the MANDATORY default properties file 'classpath:/application.properties' 
java -jar ./build/libs/adc-auth-middleware-0.0.1-SNAPSHOT.jar \ 
--spring.config.location=classpath:/application.properties,./config.properties 
```

##### DB & Cache configuration

- Example config for H2 DB

```
spring.datasource.url=jdbc:h2:file:./data/h2/db
spring.datasource.username=sa
spring.datasource.password=password
```

- Example config for PostgreSQL DB

See `data/config/example.properties` for example

```
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.platform=postgres
```

- Using Redis as Cache (Optional)

If these values are not set the default spring cache will be used

```
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

To use simple in-memory cache set:

```
spring.cache.type=simple
```


#### CSV field config

Value for the `app.adcCsvConfigPath` config param. You can use the default provided `./field-mapping.csv` or extend it.
The CSV must have header:
- `class`: Specifies whether the field is a `Repertoire` or `Rearrangement`
- `field`: The field. For nested objects demark with `.`. Example `subject.age_unit.value` or `repertoire_id`.
- `protection`: Whether the field is publicly access or protected. Public means any user can access this information, protected means only users that were given access to with the specific scope can access the field information. Valid values are `public` and `protected`. 
- `access_scope`: The UMA scope required to be able to access the field. Must be blank if `protection` is `public`, cannot be blank if it is not. Values can be any user defined string of pattern `[\w_]+`. 
**IMPORTANT**: make sure that you make no typos here, the values used here are the UMA scopes stored in keycloak and used for access control.
- `field_type`: The type of the field. User for input validation. Valid values are `string`, `boolean`, `number`, `integer`, `array_string`.

The CSV is comma separated.
Example:

```csv
class,field,protection,access_scope,field_type,description
Repertoire,repertoire_id,public,,string,"Identifier for the ..."
Repertoire,repertoire_name,public,,string,Short generic display name for the repertoire
Repertoire,repertoire_description,public,,string,Generic repertoire description
...
Repertoire,study.study_type.value,protected,statistics,string,Type of study design
```

The CSV can include other columns after these which are ignored.

## Notes

### Synchronization

The middleware needs to synchronize with the backend periodically. No automatic synchronization is performed so you must invoke synchronization when data in the resource server changes, namely when: a repertoire or rearrangement ir added, deleted or updated (study, repertoire_id and rearrangement_id fields).

To synchronize you can make the following request to the `/airr/v1/synchronize` endpoint using the password as Bearer token:

```shell script
curl --location --request POST "$MIDDLEWARE_HOST/airr/v1/synchronize" --header "Authorization: Bearer $THE_PASSWORD"
```


#### Generating password

You need to hash a BCrypt password with 10 rounds to use the synchronization endpoint

```shell script
sdk install springboot # need https://sdkman.io/ installed
PASSWORD=$(xxd -l 32 -c 100000 -p /dev/urandom) # or use a different password
spring encodepassword -a bcrypt $PASSWORD # $THE_PASSWORD
# example acceptable password: 'master' for '$2a$10$qr81MrxWblqZlMAt5kf/9.xdBPubtDMuoV3DRKgTk2bu.SPazsaTm'
```

### Public fields 

You can use the public endpoint: 

```
curl --location --request GET 'localhost:8080/airr/v1/public_fields'
```

to obtain the public fields for each class of resources.

### Minimal AIRR ADC compliance necessary

To be able to make use of this middleware the backend **MUST** implement the following AIRR ADC API endpoints (the URL base-path is configurable):

1. GET /repertoire/{repertoire_id}
2. GET /rearrangement/{rearrangement_id} 
3. POST /repertoire
4. POST /rearrangement

For endpoints 3. and 4. the backend must be able to accept a string body (that can be discarded).

The following public endpoints are not mandatory and access to them can be disabled in the middleware:

- GET /
- GET /info
- GET /swagger

The `Repertoire`s responses (1. and 3.) must be of (minimal) format:
```yaml
{
  "Info": {
    // put arbitrary data here, that can pass to the user unfiltered
  },
  "Repertoire": { // can put any extra fields in here
    "repertoire_id": "123adc", // string type, must be the id in endpoint 1.
    "study": {
      "study_id": "12", // string type, multiple repertoires can have the same study, in which case the study id MUST be the same
      "study_title": "Research thingy" // string type, while this is optional it is used for UI purposes for keycloak 
    }
  }
}
```

The `Rearrangement`s responses (2. and 4.) must be of (minimal) format:
```yaml
{
  "Info": {
    // put arbitrary data here, that can pass to the user unfiltered
  },
  "Rearrangement": { // can put any extra fields in here
    "repertoire_id": "123adc", // string type, must be the id of the repertoire to which this rearrangement belongs to
    "rearrangement_id": "234" // string type, must be the id in endpoint 2.
  }
}
```

Any extra fields used for Repertoire or Rearrangement can be used if they are set in the CSV config file.

#### Facets 

To use facets the backend **MUST** support the ADC `filters` query feature, otherwise this feature **MUST** be disabled in the middleware's config.

More specifically the `in` `filters` operator must be supported (and the `and` operator for chaining with user requests). If a user makes a Repertoires search request like:

```yaml
{
    "filters":{
        "op":"=",
        "content": {
            "field": "rearrangement_id",
            "value": "5e53dead4d808a03178c7891"
        } 
  }
}
```

The middleware modifies the request and sends:
```yaml
{
	"filters":{
        "op": "and",
        "content": [
          {
            "op":"=",
            "content": {
              "field": "rearrangement_id",
              "value": "5e53dead4d808a03178c7891"
            } 
          },
          {
            "op":"in",
            "content": {
              "field": "study_id",
              "value": ["123", "456"] // example values
            }
          }
        ]   
    }
}
```

Likewise for Rearrangements but with the `repertoire_id` value for `in`'s `field`.

It is assumed that like in the AIRR ADC API, an empty `in`:
```yaml
{
    "op":"in",
    "content": {
      "field": "study_id",
      "value": []
    }
}
```
would make the backend return an empty `Facet` response. 

If there are values for the array sent the ids **MUST** be matched against the response, otherwise an information leak is created. 

## Profilling

### Flamegraphs

1. Install JavaFX
2. Follow https://blog.codecentric.de/en/2017/09/jvm-fire-using-flame-graphs-analyse-performance/

> TODO add profiling instructions