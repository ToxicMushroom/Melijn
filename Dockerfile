FROM eclipse-temurin:21-jdk-jammy as builder
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

# Full jdk required for font rendering on ship ect
FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/melijn
ENV VERSION_HASH=%VERSION_HASH%
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx${RAM_LIMIT} \
    -Dkotlin.script.classpath="/opt/melijn/melijn.jar" \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -jar \
    ./melijn.jar