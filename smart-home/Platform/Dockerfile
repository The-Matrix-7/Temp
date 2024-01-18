# Get the Gradle image so we can build JAR file first
FROM gradle:7.0.2-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

# Get the Java version 8 image
FROM openjdk:11

# Fetch the archived repository. Unfortunately, jessie isn't there anymore so we are going to use buster (repo).
RUN echo "deb [check-valid-until=no, trusted=yes] http://cdn-fastly.deb.debian.org/debian buster main" > /etc/apt/sources.list.d/buster.list
RUN echo "deb [check-valid-until=no, trusted=yes] http://cdn-fastly.deb.debian.org/debian buster-backports main" > /etc/apt/sources.list.d/buster-backports.list
RUN sed -i '/deb http:\/\/cdn-fastly.deb.debian.org\/debian buster-updates main/d' /etc/apt/sources.list
RUN apt-get -o Acquire::Check-Valid-Until=false update

# Install JavaFX
RUN apt-get install -y openjfx

WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/tartan-1.0-SNAPSHOT.jar /app
COPY config.docker.yml /app
EXPOSE 8080
EXPOSE 8081

CMD ["java", "-jar","tartan-1.0-SNAPSHOT.jar","server","config.docker.yml"]

