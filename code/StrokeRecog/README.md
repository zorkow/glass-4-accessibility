#Stroke Recognition#

This part of the project is concerned with extracting the stroke information from a noise-free input video, and then passing the information in a specific format to the character recognition engine.

The code is obviously still very much work-in-progress, and it should be noted that the program can be sensitive to various input parameters.

##Running the program##

You will need the Java OpenCV library (v2.4.9) installed and added to the build path as a user library (see [here][1] for download and [here][2] for using with Eclipse).  

The program is designed to take input from any one of:
>1) Webcam

>2) Video file (.mp4)

>3) A series of images (frames) saved as .jpgs

The program is started from the VideoFrameMainTest.java class in package 'gui'.  Comment/uncomment the input variables related to the desired mode of input and adjust the file paths for the template and input video as required.  (See the class documentation for further details).

The key method in the program is the startProcessing() method in class ProcessVideo.

There are a number of parameters that can be adjusted to change the performance of the program.  These should all be found at the top of the relevant class in the field variables.

A test input video file and template jpg can be found in the TestInput folder.

[1]: http://opencv.org/downloads.html
[2]: http://docs.opencv.org/trunk/doc/tutorials/introduction/java_eclipse/java_eclipse.html
