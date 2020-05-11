# ADC auth extensions middleware

Middleware server for handling UMA authorization and access control.

Project runs on java 11, with (modified) google java style guide.

## Instructions

### Example Deployment

1. Load a resource server backend:

    - Start turnkey backend, based [on](https://github.com/sfu-ireceptor/turnkey-service-php):
    
        ```shell script
        # folder name should be 'turnkey-service-php', important for finding correct network. 
        git clone https://github.com/sfu-ireceptor/turnkey-service-php.git  
        cd turnkey-service-php
        echo "API_TAG=master" > .env # this should load the latest api
        scripts/install_turnkey.sh
        ```
        
        > If you use a different folder or network for the backend you need to update the file's `./data/config/docker-compose.example.yml` value `turnkey-service_default` and the `middleware` service's `RESOURCE_SERVER_BASE_URL` with the backend URL.
    
    - Load some data, based [on](https://github.com/sfu-ireceptor/dataloading-curation):
    
        follow the instructions to load some data.
        > TODO add specific instructions

2. Setup and configure keycloak server:

    ```shell script
    docker-compose --file docker-compose.example.yml build
    docker-compose --file docker-compose.example.yml up keycloak
    ```
    
    See instructions below on how to setup or skip setup if keycloak was already configured for development. Make note of the generated client secret.

3. Run the middleware:

    ```shell script
    docker-compose --file docker-compose.example.yml up middleware-db
    MIDDLEWARE_UMA_CLIENT_SECRET=<the client secret from previous step> docker-compose --file docker-compose.example.yml up middleware 
    ```
    
    You can now make requests to `http://localhost:8080/airr/v1/`. Try with `http://localhost:8080/airr/v1/info`.

4. Synchronize middleware cache:

    ```shell script
    # '12345abcd' is the password
    curl --location --request POST 'localhost:8080/airr/v1/synchronize' --header 'Authorization: Bearer 12345abcd'
    ```
    
    See below for a discussion on when to re-synchronize.

#### Deployment Notes

> **Important**: When deploying it's very important to make the backend's API unavailable to the public (for the turnkey example, delete the exposed ports in the `scripts/docker-compose.yml` file's `ireceptor-api` service)

> **Important**: You must generate a new password and hash for the `app.synchronizePasswordHash` variable, see below how. 

> **Important**: The middleware APIs should be under a SSL connection in order not to leak user credentials or synchronization password.

### First time setup (dev):

> If using OpenJDK, use minimum of v11.0.7

- Configuring keycloak

You need to setup and configure a keycloak server.

Run:
```shell script
docker-compose --file docker-compose.dev.yml build
docker-compose --file docker-compose.dev.yml up keycloak
```

Then configure keycloak:

1. Go to http://localhost:8081. Login as admin with `admin:admin`. 
2. Go to master realm settings and enable `User-Managed Access`.
3. Create a new client in the Clients tab: load (import) and save the client from the file `./keycloak/adc-middleware.json`. Go to credentials tab in the client and note the generated `Secret` value which is the client secret while `adc-middleware` is the client ID.
4. In the `Users` tab create user with username `owner`, this is the resource owner. Create user with username `user` and `user2`, these are the users that will access resources. For each user in the user's `Credentials` tab create the password (equal to username). A user can then login on `http://localhost:8082/auth/realms/master/account` (for example owner to grant accesses to users).

You can use different values for these strings, but you would need to update the some variables and example code.

### To install, build and run for development:

```shell script
./gradlew bootRun
```

With arguments:

```shell script
./gradlew bootRun --args='--server.port=9999' # --server.port equivalent to java's -Dserver.port 
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

### Tets

```shell script
./gradlew test
```

### Notes

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
- `server.servlet.context-path`: The base path of the middleware API, used to forward requests. Defaults to: `/airr/v1`
- `server.port`: The middleware server port, defaults to `8080`
- `app.adcCsvConfigPath`: The path for the CSV config file containing the custom fields configuration. Example `./field-mapping.csv`. Defaults to the file `src/main/resources/field-mapping.csv`. See below for structure of file.

Optional Dev:
- (H2 only) `spring.h2.console.enabled`: Will enable H2 web console on `http://localhost:8080/airr/v1/h2-console` (default with url `jdbc:h2:file:./data/db` account `sa:password`). Defaults to false.

Running with custom properties file (using deployment jar):

```shell script
# ./config.properties is the custom file, in the current working directory
# MAKE sure to also include the MANDATORY default properties file 'classpath:/application.properties' 
java -jar ./build/libs/adc-auth-middleware-0.0.1-SNAPSHOT.jar \ 
--spring.config.location=classpath:/application.properties,./config.properties 
```

##### DB configuration

- Example config for H2 DB

```
adc.resourceServerUrl=http://localhost:80/airr/v1

uma.wellKnownUrl=http://localhost:8082/auth/realms/master/.well-known/uma2-configuration
uma.clientId=adc-middleware
uma.clientSecret=ef6f421a-1375-4d5c-a187-e41ea9f26379
uma.resourceOwner=owner

# DB
spring.datasource.url=jdbc:h2:file:./data/h2/db
spring.datasource.username=sa
spring.datasource.password=password
```

- Example config for PostgreSQL DB

See the `data/config/example.properties` for a working example


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

#### Synchronization

The middleware needs to synchronize with the backend periodically. No automatic synchronization is performed so you must invoke synchronization when data in the resource server changes, namely when: a repertoire or rearrangement ir added, deleted or updated (study, repertoire_id and rearrangement_id fields).

To synchronize you can make the following request:

```shell script
curl --location --request POST "$MIDDLEWARE_HOST/airr/v1/synchronize" --header "Authorization: Bearer $THE_PASSWORD"
```


##### Generating password

```shell script
sudo apt install apache2-utils
PASSWORD=$(xxd -l 32 -c 100000 -p /dev/urandom) # or use a different password
echo -n $PASSWORD | sha256sum
```

## Profilling

### Flamegraphs

1. Install JavaFX
2. Follow https://blog.codecentric.de/en/2017/09/jvm-fire-using-flame-graphs-analyse-performance/

> TODO add profiling instructions