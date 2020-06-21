FROM openjdk:12-jdk
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM toxicmushroom/openjdk12-procps:v3
WORKDIR /opt/melijn
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx2G \
    -jar \
    ./melijn.jar