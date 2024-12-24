FROM openjdk:21-jdk-slim

WORKDIR /app

# Download ultrastarCatalog (should come from the release build action, which currently doesn't exist)
RUN apt-get update && apt-get install -y wget
RUN wget -O ultrastarCatalog.jar https://github.com/DoubleDee73/ultrastarCatalog/releases/download/0.2/ultrastarCatalog-0.2.jar

# Create the config.properties file
RUN echo "SongDir1=songs" > config.properties
RUN echo "UltrastarDb=ultraStar.db" >> config.properties
RUN echo "UltrastarDbLegacy=ultrastar_legacy.db" >> config.properties
RUN echo "PlaylistsDir=playlists" >> config.properties
RUN echo "lite={LITE}" >> config.properties

EXPOSE 8080

CMD ["java", "-jar", "ultrastarCatalog.jar"]