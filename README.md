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
- Java 17
- the java.exe from your bin folder should be in your environment variables

# How to start it
- Copy both files somewhere
- Edit the config.properties file and have SongDir point to your Ultrastar songs directory and PlaylistDir to your playlist directory
  - Please note, that if you are on Windows you should use / (slash) or \\\\ (double-backslash) as folder-separators
- open your terminal/console and switch to the directory you have copied the files to and start the jar with:
- java -jar ultrastarCatalog-0.1.jar
  - The call above will use the port 8080 by default. You can optionally append --server.port=8081 to change the port to 8081 (obviously you can use any port you like), if 8080 is already in use on your machine.
- if everything is fine, you can open your browser with http://localhost:8080/ to check if it works
- Find out the IP address of the machine on which the web server has been started, as "localhost" is only accessible from that particular computer
  - In Windows, you can do that by opening a command line and type in "ipconfig". For example: http://192.168.1.100:8080

If you want your party to be able to access this, obviously they have to be in the same wifi as the server.
Alternatively, you could also create a dynamic DNS host name, and configure port-forwarding of port 8080 in your router to point to the computer where the web app is running.

# Current quirks/limitations
- Currently USDX will only load playlists upon start up. If people add songs to playlists while the game is running, the songs will not show up in the playlists until restart
- A song can only be added once per playlist. This is a measurement to avoid people spamming your playlist. This way, a playlist can only be as long as you have songs in your library
- When you are playing a song, it will continue to play until you either hit the pause button (duh!) or when you open up the song details of another song

# Support
If you like this little app, feel free (but not pressured!) to buy me coffee. Any support is very much appreciated!

<a target="_blank" rel="noopener noreferrer" href="https://www.buymeacoffee.com/DoubleDee73"><img src="https://private-user-images.githubusercontent.com/26616916/302110878-ed72f62f-5c9c-4065-b50c-736b89f0fca2.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDcwNTgxMTMsIm5iZiI6MTcwNzA1NzgxMywicGF0aCI6Ii8yNjYxNjkxNi8zMDIxMTA4NzgtZWQ3MmY2MmYtNWM5Yy00MDY1LWI1MGMtNzM2Yjg5ZjBmY2EyLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAyMDQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMjA0VDE0NDMzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTVkMTY3NWY0M2I5YjAxMDFiOGVmNjFhZWFmMzdiNDBiMTdhNDk3YmU2Mjc3ZDVmOTA0YWRlOGQ5N2MzNzA0NWImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.sOQyyhEegfZ0lMjn36rwzmKzZWW7Ft9rc0fRJhHVNqc" alt="image" style="max-width: 100%; width: 200px;"></a>

