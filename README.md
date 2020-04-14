# ADC auth extensions middleware

Middleware server for handling UMA authorization and access control.

## Instructions

- To run:

```shell script
./gradlew bootRun
```

With arguments:

```shell script
./gradlew bootRun --args='--server.port=9999' # --server.port equivalent to java's -Dserver.port 
```

- Build jar

The jar uses java 11

```shell script
./gradlew bootJar # jar will be placed in ./build/libs/ 
```

### Arguments

You can set these by either adding a custom properties file (using `--spring.config.location` to inject the file, see example below) or by passing them as CLI options (with `-D<property>=<value>`)

Required:
- `adc.resourceServerUrl`: The url to the underlying resource server.
 
Optional:
- `server.servlet.context-path`: The base path of the middleware API, defaults to: `/airr/v2`
- `server.port`: The port, defaults to `8080`

Running with custom properties file (using deployment jar):

```shell script
# ./config.properties is the custom file, in the current working directory
# makes sure to also include the default properties file 'application.properties' 
java -jar ./build/libs/adc-auth-middleware-0.0.1-SNAPSHOT.jar \ 
--spring.config.location=classpath:/application.properties,./config.properties 
```