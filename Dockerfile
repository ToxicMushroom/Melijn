FROM azul/zulu-openjdk-alpine:15 as builder
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM azul/zulu-openjdk-alpine:15
WORKDIR /opt/melijn
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx2560M \
    -jar \
    ./melijn.jar