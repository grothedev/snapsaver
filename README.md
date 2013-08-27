snapsaver
=========

an android app that saves snapchats

How it Works: When the app starts, there is a button to press. This button will copy the pictures and videos onto your sdcard(from /data/data/com.snapchat.android/cache/received_image_snaps/ and /storage/sdcard0/Android/data/com.snapchat.android/cache/received_video_snaps/ to /storage/sdcard0/savedsnaps) after 15 seconds (so the user has time to let snaps load). After another 10 seconds it will remove the .nomedia extension off of the files. If it has root access it will copy pics and vids. If it doesn't have root access it will only copy vids. 
