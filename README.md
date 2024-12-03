# What's ultrastarCatalog?
This is a small web app (Spring Boot / Tomcat) to show the catalog of your Ultrastar library.

The intention is, that when you have a Ultrastar-Party, you can start up this little web application and have the attendees of your party open up the URL on their mobile devices. This way, everyone can browse through the songs, that you can offer.

![image](https://github.com/DoubleDee73/ultrastarCatalog/assets/26616916/6f9472d8-fbf5-42ac-8f44-24ef4b02e6cd)

## What can the app do?
Keep in mind, this is still at an early stage, so right now, you can:
- Search by artist, title or tag
- Sort the search results by either artist or title
- Filter by language, decade, tags and/or playlist
- Listen to the audio
- Add songs to existing playlists

## What does the software do?
The software will scan through your songs folder and read the ultrastar-txt files to build up the library. A webserver will be started that can be reached within your wifi network. Images and audio files will be streamed from your songs folder. When someone adds songs to playlists, the software will do that directly on the upl-file.

# What do you need?
- Java 21
- the java.exe from your bin folder should be in your environment variables

# How to start it
- Copy both files somewhere
- Edit the config.properties file and have SongDir point to your Ultrastar songs directory and PlaylistDir to your playlist directory
  - Please note, that if you are on Windows you should use / (slash) or \\\\ (double-backslash) as folder-separators
- open your terminal/console and switch to the directory you have copied the files to and start the jar with:
- java -jar ultrastarCatalog-0.2.jar
  - The call above will use the port 8080 by default. You can optionally append --server.port=8081 to change the port to 8081 (obviously you can use any port you like), if 8080 is already in use on your machine.
- if everything is fine, you can open your browser with http://localhost:8080/ to check if it works
- Find out the IP address of the machine on which the web server has been started, as "localhost" is only accessible from that particular computer
  - In Windows, you can do that by opening a command line and type in "ipconfig". For example: http://192.168.1.100:8080

If you want your party to be able to access this, obviously they have to be in the same wifi as the server.
Alternatively, you could also create a dynamic DNS host name, and configure port-forwarding of port 8080 in your router to point to the computer where the web app is running.

# Are there more options?
- If you want to display your scores, you can edit the config.properties and have the parameter UltrastarDb point to your
Ultrastar.db file, e.g. <code>UltrastarDb=C:\\Program Files (x86)\\UltraStar Deluxe\\Ultrastar.db</code> 
- Let's say, you are using ultrastarCatalog in situation, where you don't want to offer the MP3 audio preview (e.g. 
due to copyright reasons), you can then add the parameter <code>lite=true</code> and the audio preview will not be shown

# Current quirks/limitations
- Currently USDX will only load playlists upon start up. If people add songs to playlists while the game is running, the songs will not show up in the playlists until restart
- A song can only be added once per playlist. This is a measurement to avoid people spamming your playlist. This way, a playlist can only be as long as you have songs in your library
- When you are playing a song, it will continue to play until you either hit the pause button (duh!) or when you open up the song details of another song

# Support
If you like this little app, feel free (but not pressured!) to buy me coffee. Any support is very much appreciated!

<a target="_blank" rel="noopener noreferrer" href="https://www.buymeacoffee.com/DoubleDee73"><img src="https://github.com/user-attachments/assets/a40f851a-2ef1-46ce-a6d6-0bf6fd0ffb95" alt="image" style="max-width: 100%; width: 200px;"></a>

