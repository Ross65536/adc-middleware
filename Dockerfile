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

FROM openjdk:11

ENV PROPERTIES_PATH ./config/example.properties

WORKDIR /middleware

COPY --from=0 /middleware/middleware.jar .
CMD java -jar ./middleware.jar --spring.config.location=classpath:/application.properties,$PROPERTIES_PATH --uma.clientSecret=$CLIENT_SECRET --adc.resourceServerUrl=$RESOURCE_SERVER_BASE_URL
