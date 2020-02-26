# NOT TO BE RUN IN PRODUCTION

FROM openjdk:8-alpine3.9

ARG KEYCLOAK_ARCHIVE_URL=https://downloads.jboss.org/keycloak/9.0.0/keycloak-9.0.0.tar.gz
ARG SHA1=a667600d9e849a13e14e1bcb3b9821e28ce4ae9c
ARG ADMIN_PASS=admin

RUN apk add --no-cache --virtual .build-deps \
    curl

RUN curl $KEYCLOAK_ARCHIVE_URL > keycloak.tar.gz
RUN test $(echo $SHA1) = $SHA1
RUN tar -xvzf keycloak.tar.gz

RUN rm keycloak.tar.gz
RUN apk del .build-deps

RUN mv keycloak-* keycloak

WORKDIR keycloak

COPY config.xml standalone/configuration/standalone.xml

RUN sh bin/add-user-keycloak.sh -u admin -p $ADMIN_PASS

EXPOSE 8080

CMD sh bin/standalone.sh -Djboss.http.port=80 -Djboss.bind.address=0.0.0.0 -Djboss.bind.address.management=0.0.0.0