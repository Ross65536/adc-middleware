# ADC auth extensions middleware

[![pipeline status](https://gitlab.com/Ross65536/adc-middleware/badges/master/pipeline.svg)](https://gitlab.com/Ross65536/adc-middleware/commits/master)

Middleware server for handling access control for [ADC v1 HTTP API](https://docs.airr-community.org/en/latest/api/adc_api.html) compliant repositories.

Check out Docker image [here](https://hub.docker.com/repository/docker/irpinesctec/adc-middleware)

Project runs on java 11, with (modified) google java style guide.

Features:
- Support for all of the AIRR ADC API functionalities except for: `tsv` format on POST endpoints. 
- Response fields filtering based on provided token access level.
- Emission of UMA tickets restricted to the fields requested.

You can also checkout this simple [front-end](https://github.com/Ross65536/adc-middleware-frontend) for testing the access control capabilities of this middleware.

## Instructions

### Example Deployment (for testing)

Example deployment for testing in localhost.

0. Clone the repository:

  ```shell script
git clone https://github.com/ireceptorplus-inesctec/adc-middleware
  ```

1. Load repository test data:

  iReceptor Turnkey is used in this example.

  Download backend data (for testing):
  ```shell script
cd example
curl -L https://github.com/Ross65536/adc-middleware/releases/download/data/repository-data.tar.gz > repository-data.tar.gz
tar -xvzf repository-data.tar.gz -C data/
sudo chown -R $(whoami) data/mongodb/
  ```

2. Load all components:

  (Optional) build middleware docker image locally and push it to Docker Hub:
  ```shell script
docker login registry-1.docker.io --username=yourhubusername --password=yourpassword
docker build -t irpinesctec/adc-middleware:$IMG_TAG .
docker push irpinesctec/adc-middleware:$IMG_TAG
  ```

  Load components
  ```shell script
  cd example

  # load all components
  MIDDLEWARE_UMA_CLIENT_SECRET=12 docker-compose up # MIDDLEWARE_UMA_CLIENT_SECRET is not important in this step but must be set
````

  You can now make requests to `http://localhost/airr/v1/`. Try with `http://localhost/airr/v1/info` to see if there is a connection to the backend. 

  On boot the middleware server automatically connects to the DB.

3. Configure keycloak:

  Keycloak admin login page is now accessible at `http://localhost/auth`. Regular user login at `http://localhost/auth/realms/master/account`.
  Then see below how to [configure keycloak for first use](./README.md#keycloak-configuration).
  Make note of the generated client secret (`$MIDDLEWARE_UMA_CLIENT_SECRET`) for the created `adc-middleware` client.
  You also need to create an additional client in the Keycloak's `Clients` side bar tab, load (import) and save the client from the file `./example/config/keycloak/front-end.json`.

4. Re-Load the server with the client secret

  ```shell script
  # first stop docker containers, e.g.
  docker-compose stop

  # then
  MIDDLEWARE_UMA_CLIENT_SECRET=<the client secret from step 3> docker-compose up
  ```

5. Synchronize middleware and Keycloak state with Repository:

  ```shell script
  # '12345abcd' is the password
  curl --location --request POST 'localhost/airr/v1/synchronize' --header 'Authorization: Bearer 12345abcd'
  ```

  See below for a discussion on when to re-synchronize.

  You should now be able to access the frontend on `http://localhost`, login, and access resources.

#### Deployment Notes

> **Important**: When deploying it's very important to make the backend's API unavailable to the public

> **Important**: If you want different user role to be able to call the /synchronize endpoint, make sure to define it in `app.synchronizeRole`. 

> **Important**: The middleware APIs should be under a SSL connection in order not to leak user credentials or the synchronization password. If it is not under SSL you need to disable SSL connections Keycloak (see below).

> **Important**: The host and port used by the middleware to access keycloak and the host and port used by the user to obtain an RPT token must be the exact same (must have the same `Host` header) otherwise the solution doesn't work. Check the nginx configuration to see how this was achieved. When deploying, if using the provided nginx config you must update the line marked as `VERY IMPORTANT`.

### Keycloak Configuration

#### Initial Keycloak Setup

1. Go to the keycloak login page (`$HOSTNAME/auth`). Login as admin with `admin:admin`. 
2. Go to `master`'s `Realm Settings` in the sidebar and enable `User-Managed Access` in the `General` tab.
3. Create a new client in the `Clients` side bar tab: load (import) and save the client from the file `./example/config/keycloak/adc-middleware.json`. Go to credentials tab in the client and note the generated `Secret` value which is the client secret while `adc-middleware` is the client ID.
4. In the `Users` tab create a user with username `owner`, this is the resource owner. Create a user with username `user`, this is the user that will access resources. For each created user in the user's `Credentials` tab create the password. 
A user can then login (`$HOSTNAME/auth/realms/master/account`), for example as an owner to grant accesses to users.
5. Go to `Roles` in the sidebar and press `Add Role`, naming it `synchronize`.
6. Go to the `Users` tab and press `admin`, in the `Role Mappings` tab select `synchronize` from `Available Roles` and press `Add selected >`, making sure the role is now shown in the `Assigned Roles` group.

You can use different values for these strings, but you would need to update a lot of configuration variables.

Alternatively instead of setting up from points ```2``` through ```6```, you can just select the import option from the sidebar and select the ```example/config/keycloak/realm-export.json```.
If the import method is used, the secrets for each client must be generated.

When running keycloak from the provided ```docker-compose.yml``` files the realm will be automatically imported

#### SSL

If you need to disable Keycloak's SSL for remote connections, for example when acessing a remote server without SSL enabled, you can:

- Disable using admin panel. You need to access the admin panel in the remote server locally (localhost) and go to `Realm Settings` -> `Login` -> `Require SSL` = `none`

- Disable using SQL script. You can connect to keycloak's database (after it has been loaded by keycloak) and the run:

  ```sql
  UPDATE realm set ssl_required = 'NONE' where id = 'master';
  ```

  You can connect, for example with:
  ```shell script
  # in remote server shell
  psql -U postgres -p 5432 -h localhost
  \c keycloak_db
  ```

### Docker image

The docker image for the middleware accepts the following environment variables:

- `CLIENT_SECRET`: The UMA client secret for the middleware.
- `DB_PASSWORD`: The DB password for the middleware
- `PROPERTIES_PATH`: The path for the java properties configuration file. 

The remaining configuration is done using java properties (for example see `data/config/example.properties`, for explanation see below).

### Dev Setup:

> If using OpenJDK, use minimum of v11.0.7

#### Configuring keycloak

You need to setup and configure a keycloak server.

Run:
```shell script
docker-compose --file docker-compose.dev.yml up keycloak_db
docker-compose --file docker-compose.dev.yml up keycloak
```

Keycloak is now hosted on `http://localhost:8082`.

Then see [above how to configure keycloak](./README.md#keycloak-configuration).


#### To install, build and run for development:

```shell script
./gradlew bootRun
```

With arguments:

```shell script
./gradlew bootRun --args='--server.port=9999' # --server.port equivalent to java's -Dserver.port 
```

With explicit configuration arguments:

```shell
./gradlew bootRun --args='--spring.config.location=classpath:/application.properties,./dev.properties'
```

#### Dev properties configuration example

file `dev.properties` (You need to update `uma.clientSecret`):

```
adc.resourceServerUrl=http://localhost:80/airr/v1
server.port=8080

# UMA
uma.wellKnownUrl=http://localhost:8082/auth/realms/master/.well-known/uma2-configuration
uma.clientId=adc-middleware
uma.clientSecret=<the generated client secret from keycloak>
uma.resourceOwner=owner

# Role of the user that's able to call /synchronize
app.synchronizeRole=admin

# Postgres
spring.datasource.url=jdbc:postgresql://localhost:5432/middleware_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.platform=postgres

spring.flyway.url=jdbc:postgresql://localhost:5432/middleware_db
spring.flyway.user=postgres
spring.flyway.password=password
spring.flyway.schemas=public
spring.flyway.locations=classpath:/db/postgresql
spring.flyway.enabled=true

# Redis
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

#### Creating and Migrating the Database
Database creation and migration is managed by [Flyway](https://flywaydb.org/) and is normally called automatically on the application's execution and you are not required to migrate and seed manually, provided that you configured your `.properties` file with similar Flyway settings as shown above. 

Migration scripts are located in `src/main/resources/db`. 

If you wish to trigger migrations manually without running the application, run the following command:

```shell
./gradlew clean build flywayMigrate -Dflyway.configFiles=dev.properties
```

If using IntelliJ IDEA, make sure to provide `-Dflyway.configFiles=dev.properties` as `VM options`.

Developer's note: **Avoid changing older/previous migration scripts at all costs**. Flyway is a revision-based system and the migration workflow follows all migration scripts sequentially. If an older existing migration script is changed, **it will cause a checksum error with any existing database** you try to migrate. If the existing data model requires changes, please create a new script. 

More details about Flyway can be found on:

- [Flyway's Official Documentation](https://flywaydb.org/documentation/)
- [Best Practices for Flyway and Hibernate with Spring Boot](https://rieckpil.de/howto-best-practices-for-flyway-and-hibernate-with-spring-boot/)

#### Dev run example

```shell script
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

Tests may be skipped by providing the argument `-x test` in any pipeline of commands.

#### Pushing docker image

Dockerhub has setup a hook to automatically pull and build images from repository commits that are tagged like `v1.0.1` using semantic versioning.

```shell script
git tag -a v<VERSION> -m <MESSAGE> # tag latest commit
git push origin --tags # This should trigger a build in dockerhub
```

#### Running Keycloak's extension API
```shell script
git clone https://github.com/ireceptorplus-inesctec/keycloak_extension_api # clone the repository
cd keycloak_extension_api # change directory to cloned repository
cp db_connection.env.example .env # creating .env file that should be edited
python3 api.py # run the API
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
- `app.synchronizeRole`: String that defines the role of the user that's able to call the /synchronize endpoint.

Optional:
- `server.servlet.context-path`: The base path of the middleware API. Defaults to: `/airr/v1`
- `server.port`: The middleware server port, defaults to `80`
- `app.adcCsvConfigPath`: The path for the CSV config file containing the custom fields configuration. Example `./field-mapping.csv`. Defaults to the file `src/main/resources/field-mapping.csv`. See below for structure of file.
- `app.facetsEnabled`: Boolean, indicates whether the resource server supports `facets` (and by extension ADC `filters`). Defaults to `true`.
- `app.publicEndpointsEnabled`: Boolean, indicates whether the resource server supports the public ADC endpoints (`/`, `/info`, `/swagger`). Defaults to `true`.
- `app.adcFiltersEnabled`: Setting this to true will disable POST endpoint's `"filters"` function which should make oracle attacks unfeasible. Defaults to `false`.
- `app.filtersOperatorsBlacklist`: A comma separated list of `"filters"` operators that are disabled. Disabling some operators helps mitigate timing attacks. Defaults to `` which disables this feature. 
- `app.requestDelaysPoolSize`: Positive integer, the delays pool size. Defaults to `10`. Set to `0` to disable request delaying when emitting permissions tickets. Mitigates timing attacks. 

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
- `field`: The field. For nested objects demark with `.`. Example `subject.age_unit.value` or `repertoire_id`. Supports arrays.
- `protection`: Whether the field is publicly access or protected. Public means any user can access this information, protected means only users that were given access to with the specific scope can access the field information. Valid values are `public` and `protected`. 
- `access_scope`: The UMA scope required to be able to access the field. Must be blank if `protection` is `public`, cannot be blank if it is not. Values can be any user defined string of pattern `[\w_]+`. 
**IMPORTANT**: make sure that you make no typos here, the values used here are the UMA scopes stored in keycloak and used for access control.
- `field_type`: The type of the field. User for input validation. Valid values are `string`, `boolean`, `number`, `integer`, `array_string`.
- `include_fields`: Can be one of `miairr`, `airr-core`, `airr-schema` or empty. Specifies to which type the field belongs to. A field that belongs to `airr-schema` belongs also to `airr-core` and `miairr` and a field of `airr-core` also belongs to `miairr`.  Matches with the ADC API query's `include_fields` JSON parameter.

The CSV is comma separated. For an example see `src/main/resources/field-mapping.csv`.

The CSV can include other columns after these which are ignored.

## Notes

### Synchronization

The middleware needs to synchronize with the backend periodically. No automatic synchronization is performed so you must invoke synchronization when data in the resource server changes, namely when: a repertoire or rearrangement ir added, deleted or updated (study, repertoire_id and sequence_id fields).

To synchronize you can make the following request to the `/airr/v1/synchronize` endpoint using an Access Token from a user that has the role defined in the `app.synchronizeRole` property.

```shell script
curl --location --request POST "$MIDDLEWARE_HOST/airr/v1/synchronize" --header "Authorization: Bearer $ACCESS_TOKEN"
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
2. GET /rearrangement/{sequence_id} 
3. POST /repertoire
4. POST /rearrangement

For endpoints 3. and 4. the backend can discard the other query fields **except** the `"facets"` field which must be processed and returned correctly.

The following public endpoints are not mandatory and access to them can be disabled in the middleware:

- GET /
- GET /info
- GET /swagger

The `Repertoire`s regular responses (1. and 3.) must be of (minimal) format:
```yaml
{
  "Repertoire": [ 
    { // can put any extra fields in here
      "repertoire_id": "123adc", // string type, must be the id in endpoint 1.
      "study": {
        "study_id": "12", // string type, multiple repertoires can have the same study, in which case the study id MUST be the same
        "study_title": "Research thingy" // string type, while this is optional it is used for UI purposes for keycloak 
      }
    }
  ]
}
```

The `Rearrangement`s regular responses (2. and 4.) must be of (minimal) format:
```yaml
{
  "Rearrangement": [
    { // can put any extra fields in here
      "repertoire_id": "123adc", // string type, must be the id of the repertoire to which this rearrangement belongs to
      "sequence_id": "234" // string type, must be the id in endpoint 2.
    }
  ]
}
```

Any extra fields used for Repertoire or Rearrangement can be used if they are set in the CSV config file.

As mentioned, facets must be supported. 
The middleware when receiving a user query for repertoires of the like:
```yaml
{
    "filters": {
        "op": "=",
        "content": {
            "field": "repertoire_id",
            "value": "5e53dead4d808a03178c7891"
        }
    },
    "from": 5,
    "size": 10
}
```

Will modify the request to before sending it to the repository:
```yaml
{
    "filters": {
        "op": "=",
        "content": {
            "field": "repertoire_id",
            "value": "5e53dead4d808a03178c7891"
        }
    },
    "from": 5,
    "size": 10,
    "facets": "study.study_id" // For rearrangements the field is "repertoire_id"
}
```

The middleware expects a minimal facets response of the like: 
```yaml
{
    "Facet": [
        {
            "study.study_id": "s1", // string. For rearrangements the field is "repertoire_id"
            "count": 130 // integer
        },
        ...
    ]
}
```

In the example above the `filters`, `from` and `size` parameters can be discarded by the repository.
If the `filters` support is not compliant enough as discussed below the facets feature **MUST** be disabled in the middleware to avoid security issues (even though the middleware makes use of the repository's facets function).
The official documentation (ADC v1) does not specify that the parameters `from` and `size` can be used with the `facets` parameter, and they can be safely discarded by the repository, but in order to improve security (by limitting the scope of the emitted tokens) and avoid UI clutter these should be supported and should perform the same query along with `filters` as done in a regular search without facets, that is, the set of field values returned by a regular search, and by using facets must be the same.

#### Facets 

To use facets the repository backend **MUST** support the ADC `filters` query feature as described here, otherwise this feature **MUST** be disabled in the middleware's config.

More specifically the `in` `filters` operator must be supported, and the `and` operator for chaining with user requests. If a user makes a Repertoires search request like:

```yaml
{
    "filters":{
        "op":"=",
        "content": {
            "field": "repertoire_id",
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
              "field": "repertoire_id",
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

It is assumed that, like in the AIRR ADC API, an empty `in`:
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

If there are values for the array sent in the `in` operator the ids **MUST** be matched against the response, otherwise an information leak is created. 

### TSV format

TSV format is supported for the `POST /v1/rearrangement` endpoint.

The user's requested fields cannot be nested documents/objects (in the default CSV configuration no rearrangement fields are nested objects). 

TSV support is implemented in the middleware itself by translating JSON to TSV.


### Adding OpenID Connect third-party Identity Providers

1. Login to keycloak's admin panel.
2. Go to `Identity Providers` in the side bar and add a OpenID Connect provider, set the `alias` which will be the display name (for example to `orcid`) and make note of the generated `Redirect URI`.
3. Add keycloak to third party OIDC IdP. 

  For ORCDID login as an account, go to developer tools, and add keycloak: set the `Your website URL` to keycloak's host (example `http://localhost:8082`) and put in `Redirect URIs` the url generated in keycloak from the previous step (example `http://localhost:8082/auth/realms/master/broker/orcid/endpoint`). Make note of the `Client ID` and `Client Secret`. Save.

  For EGI Checkin: In the dashboard from step 2, add generated info from previous step. 

  For ORCID put `https://orcid.org/oauth/authorize` in the `Authorization URL`, `https://orcid.org/oauth/token` in the token url, set `Client Authentication` to `Client secret sent as post` and input the client ID and client secret from the previous step in `Client ID` and `Client Secret`. Save

  For EGI Checkin put `https://aai-dev.egi.eu/oidc/authorize` in the `Authorization URL`, `https://aai-dev.egi.eu/oidc/token` in the token url, set `Client secret sent as post` and input client ID and secret. Save

## Notes

You can obtain the Postman folder that you can use to test the middleware solution's ADC features in more depth [here](./example/adc-middleware.postman_collection.json)
