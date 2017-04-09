Go Ubiquitous
======

This is Go Ubiquitous project of Udacity's Android Developer Nanodegree.
The purpose of this project is to build a wearable watch face for Sunshine app.
The watch face provides the high/low temperatures and graphically displays the weather condition.



Screens
------

![alt text](https://github.com/henriquenfaria/go-ubiquitous/blob/master/art/sunshine_phone.png "Sunshine Phone")
![alt text](https://github.com/henriquenfaria/go-ubiquitous/blob/master/art/sunshine_wearable.png "Sunshine Wearable")



Instructions
------

To connect Sunshine in an Android Wear running in Emulator:

1) Download and install Android Wear app in your phone and configure it to use emulators
2) Make sure your PC and your phone are connected to the same network
3) Forward the adb port: `adb -d forward tcp:5601 tcp:5601`
4) Open the Sunshine app in your phone and than start the the watch face in then emulator



Libraries
------

This project uses the following library:

[Firebase JobDispatcher](https://github.com/firebase/firebase-jobdispatcher-android)



License
------

> Copyright 2017 Henrique Nunes Faria

> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

> Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
