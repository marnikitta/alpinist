FROM openjdk:8-jdk

RUN apt-get update && apt-get install -y git

COPY entrypoint.sh /alpinist/entrypoint.sh
# Copy commands are separated so the common libs would be cached
COPY target/lib/*.jar /alpinist/lib/

COPY target/lib/alpinist/*.jar /alpinist/lib/
COPY target/alpinist-application-1.0-SNAPSHOT.jar /alpinist/alpinist-application-1.0-SNAPSHOT.jar

WORKDIR /alpinist
CMD ["bash", "entrypoint.sh"]
