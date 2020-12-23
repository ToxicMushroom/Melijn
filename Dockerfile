FROM openjdk:15-jdk as builder
WORKDIR /etc/melijn
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

FROM openjdk:15-jdk
WORKDIR /opt/melijn
COPY --from=builder ./etc/melijn/build/libs/ .
ENTRYPOINT java \
    -Xmx3000M \
    -XX:+UseShenandoahGC \
    -Dkotlin.script.classpath="/opt/melijn/melijn.jar" \
    -jar \
    ./melijn.jar