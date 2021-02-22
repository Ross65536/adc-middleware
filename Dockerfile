# Docker instructions
#
# docker build -t $USERNAME/adc-middleware:$VERSION .
# docker login --username $USERNAME
# docker push $USERNAME/adc-middleware:$VERSION


FROM openjdk:11

WORKDIR /middleware

ADD src /middleware/src
ADD gradle /middleware/gradle
ADD build.gradle /middleware
ADD gradlew /middleware

RUN ./gradlew bootJar
RUN mv ./build/libs/*.jar ./middleware.jar

FROM openjdk:11-jre-slim

WORKDIR /middleware

COPY --from=0 /middleware/middleware.jar .
CMD java -jar ./middleware.jar --spring.config.location=classpath:/application.properties,$PROPERTIES_PATH --uma.clientSecret=$UMA_CLIENT_SECRET --spring.datasource.password=$DB_PASSWORD --spring.flyway.password=$DB_PASSWORD
