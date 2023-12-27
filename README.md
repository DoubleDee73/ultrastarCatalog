This is a small web app (Spring Boot / Tomcat) to show the catalog of your Ultrastar library.

What do you need?
Java 17
the java.exe from your bin folder should be in your environment variables

How to start it
Copy both files somewhere
Edit the config.properties file and have SongDir point to your Ultrastar songs directory
open your terminal/console and switch to the directory you have copied the files to and start the jar with:
java -jar ultrastar-0.0.1-SNAPSHOT.jar

if everything is fine, you can open your browser with https://localhost:8080/artists

If you want your party to be able to access this, obviously they have to be in the same wifi as the server.
Alternatively, you could also create a dynamic DNS host name, and configure port-forwarding of port 8080 in your router to point to the computer where the web app is running.
