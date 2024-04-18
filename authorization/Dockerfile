FROM gradle:5.6.2-jdk11

COPY / /authorization
WORKDIR /authorization
RUN gradle getDeps --build-file build-libs.gradle
RUN gradle shadowJar


