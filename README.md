This is a small web app (Spring Boot / Tomcat) to show the catalog of your Ultrastar library.

What do you need?
Java 17
the java.exe from your bin folder should be in your environment variables

# How to start it
- Copy both files somewhere
- Edit the config.properties file and have SongDir point to your Ultrastar songs directory
  - Please note, that if you are on Windows you should use / (slash) or \\\\ (double-backslash) as folder-separators
- open your terminal/console and switch to the directory you have copied the files to and start the jar with:
- java -jar ultrastar-0.0.2-SNAPSHOT.jar
  - The call above will use the port 8080 by default. You can optionally append --server.port=8081 to change the port to 8081 (obviously you can use any port you like), if 8080 is already in use on your machine.
- if everything is fine, you can open your browser with http://localhost:8080/artists

If you want your party to be able to access this, obviously they have to be in the same wifi as the server.
Alternatively, you could also create a dynamic DNS host name, and configure port-forwarding of port 8080 in your router to point to the computer where the web app is running.
