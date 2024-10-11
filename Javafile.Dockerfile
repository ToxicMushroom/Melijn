FROM arm64v8/alpine:edge
RUN apk add --no-cache qalc
RUN apk add --no-cache openjdk21