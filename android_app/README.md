# Installation instructions
1. Go to the Github page [Releases](https://github.com/williamyang98/gps_tracking/releases) page and download the latest apk.
2. Copy the apk to your phone and install it via your installed file manager.
    - Requires granting permission to install external apps (you should rescind this permission afterwards).

# First time usage instructions
1. Navigate to permissions page and enable all permissions. 
    - Select precise location permission first of you want more accurate GPS readings.
    - On newer Android versions it will let you choose whether to give coarse or precise location access to the application.
2. Navigate to the settings page and fill out the fields. 
    - User id is just a unique number to identify the phone.
    - User name is your description for this device.
    - Interval time is how often the background service will try to send location updates to the server.
    - Server url is the link to the root of all your gcloud function endpoints.
3. Navigate to the home page and click the button to run it in the background.
    - If everything works out then the service status indicator will turn on and you will get a notification that it is running. 
    - If it didn't do this then try to restart the background service by toggling the start button. 
4. Go the locations page to register your phone with the server. 
    - Click the user icon in the server response section to send your user id and name to your gcloud server. 
    - If it works you should get a notification telling you that the username was registered. 
    - Click the refresh button in the gps sensor section to manually measure and submit a single gps data point to the server. 
5. Go to the timeline page to make sure the gps data was sent successfully. 
    - If it all worked out then you should see successful timeline entries for the gps measurement and server response. 
    - If the gps measurement failed make sure location permissions have been provided. 
    - If there was a server error click on the timeline entry to get a detailed description for why it failed. 
6. Minimise or close the app to leave it running in the background. 

# Changing Android permissions for best reliability 
1. Disable power usage optimisation for the app to prevent the operating system from closing it down to save energy. 
    - Requires setting the power profile to unoptimised.
    - Disable any options that would close the app if the phone is trying to conserve power. 
2. Enable option to allow the application to launch when the phone starts. 
    - The app should autolaunch when the phone has booted up if this was enabled in the settings page. 
    - However on some Android devices it may require an additional toggle to be enabled in your settings. 
3. Allow app to keep permissions if it has been used for a long time. 
    - Even though app is running in the background some Android phones will still revoke permissions if the user hasn't manually launched it. 
    - You can disable this option in your settings app for the tracking app. 
4. Change settings relating to network usage. 
    - Allow for mobile data usage while using data saver for the tracking app if that is supported on your phone. 
    - Permit use of the network while application is in the background if that is an option on your phone. 
5. Disable option to automatically delete unused apps for the tracking app. 

# (Optional) Development Instructions
1. Download Android Studio to build application: [Android Studio](https://developer.android.com/studio).
2. Open this project folder within Android Studio.
3. Build the project.
    - ```Menu > Build > Build Bundle(s) / APK(s) > Build APK(s)```.
    - Copy the output apk from ```./android_app/app/build/outputs/apk/debug/app-debug.apk```.
4. Copy the apk to your phone and install it via your installed file manager.
    - Requires granting permission to install external apps (you should rescind this permission afterwards).

**NOTE:** You may also build the project by building and uploading it using ADB Debugging mode on your phone.

