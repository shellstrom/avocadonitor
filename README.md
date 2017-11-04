# Avocado (or whatever you want) tree monitor, Android Things style

Welcome to this Android Studio project, where I have created some code for you to draw inspiration from in terms of using the [Camera module v2](https://www.raspberrypi.org/products/camera-module-v2/) and post to Twitter (and save to a samba connected device if you so wish)

## Why this?

I wanted to see whether I could do it. And I had some grief that I'd like you to be aware of in terms of the camera and what can be achieved with it, so you can avoid running into the same problems.

## What were the problems?

* Getting the camera to snap pictures at its specced resolution, which is 3280x2464. Android things seem to only support 640x480 max at this point in time.
* Dealing with ImageFormat was a pain. There are 4 supported formats: 1 (unspecified), 256 (JPEG), 34 (PRIVATE), and 35 (YUV_420_888). I couldn't get any other format to work other than JPEG and YUV_420_888, and a lot of other developers tells you to avoid YUV_420_888. I got the best results using JPEG but only at a resolution of 320x240 so far. Using 640x480 creates incompatible JPEGs for some reason.
* Dealing with Twitter APIs. There are two different sets of development kits; Fabric and "Twitter apps". I went in the wrong direction at first with Fabric, which only seem to support posting tweets if you have a screen for interaction. I replaced that implementation with the twitter4j library instead. It does the job with the current version of Twitter's APIs.

## What do I need to know?

* You have to create your own Twitter account and [app](http://apps.twitter.com) with at least Read and Write permissions, and tie the consumer keys and access tokens to the app.
* Tweets are being sent only when a certain hour of the day is detected.
* Per default images are saved to a samba share every hour.

Life is pretty damn awesome
