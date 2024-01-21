# Door Alert
An IOT + Android project to provide notification alerts in an android phone when detecting movements through a door or a passage.

### Components :
1. ESP8266 microcontroller
2. HCSR04 ultrasonic sensor
3. Android App
4. A PC with firebase Admin SDK as a helper server to mimic Firebase Cloud Function

### How it works :
The NodeMCU microcontroller communicates with firebase backend through andmin REST API endpoints. Whenever movement is detected by microcontroller through the ultrasonic sensor, a new movement record is added with timestamp to the Firebase RTDB.
If the helper server is online, it moniters the RTDB and triggers a push notification when a new movement record appears.
When the android app is online, it copies the movement records that appears (or already existed) to local storage and displays the records.
The sensor state can be activated or deactivated through the android app by updating a boolean field in the RTDB to notify the microcontroller

### Libraries used :
* OkHttp (Android)
* ESP8266WiFi (Arduino)

### Video Demonstration : <a href="https://youtu.be/qOJX1L13l5U"/><img src="https://www.vectorlogo.zone/logos/youtube/youtube-icon.svg" alt="Youtube" height="16" width="16"/> https://youtu.be/jVoAJNi9HJ8?si=p6pL6bZQu_oqGLrU</a>
