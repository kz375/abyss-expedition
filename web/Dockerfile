FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY src/ src/
COPY web/java/ web/java/
RUN mkdir /classes && find src web/java -name '*.java' -print > /sources && javac --release 17 -encoding UTF-8 -d /classes @/sources

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --gid 10001 abyss && useradd --uid 10001 --gid abyss --no-create-home abyss && mkdir /data && chown abyss:abyss /data
COPY --from=build /classes /app/classes
COPY web/public/ /app/public/
ENV ABYSS_HOST=0.0.0.0 ABYSS_WEB_ASSETS=/app/public ABYSS_WEB_DATA=/data ABYSS_MAX_PLAYERS=8
USER abyss
EXPOSE 10000
VOLUME ["/data"]
CMD ["java", "-Xmx128m", "-Djava.awt.headless=true", "-cp", "/app/classes", "abyss.web.WebServer"]
