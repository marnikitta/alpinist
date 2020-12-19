FROM openjdk:13-alpine

RUN apk update && apk add --no-cache git bash openssh-client

COPY entrypoint.sh /alpinist/entrypoint.sh
# Copy commands are separated so the common libs would be cached
COPY target/lib/*.jar /alpinist/lib/

COPY target/lib/alpinist/*.jar /alpinist/lib/
COPY static/* /alpinist/static/
COPY target/alpinist-application-1.0-SNAPSHOT.jar /alpinist/alpinist-application-1.0-SNAPSHOT.jar

WORKDIR /alpinist
CMD ["bash", "entrypoint.sh"]
