FROM openjdk:15-jdk-alpine3.12 as builder
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

# Full jdk required for font rendering on ship ect
FROM openjdk:15-jdk-alpine3.12
WORKDIR /opt/melijn
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx${RAM_LIMIT} \
    -Dkotlin.script.classpath="/opt/melijn/melijn.jar" \
    -jar \
    ./melijn.jar