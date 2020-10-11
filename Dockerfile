FROM openjdk:15 as builder
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM openjdk:15
WORKDIR /opt/melijn
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx3000M \
    -jar \
    ./melijn.jar