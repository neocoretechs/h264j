This is an H264 Java port to replace Xuggler in ARDrone video processing. It is also a general purpose H264 decoder.
This fork adds stream processing and callbacks with raw video frames, which is how the ARDrone video is implemented.
There is a standalone player that reads files, another for streams, and a headless version with the callbacks with each AVFrame.
```
Stream example:
		H264StreamPlayer hsd = new H264StreamPlayer(is); // where is is the inputstream from ARDrone video
		hsd.playStream();
Listener example:		
		H264StreamCallback hsc = new H264StreamCallback(is, listener); // where listener is RGBListener receiving AVFrames
		hsc.playStream();
File example:
		H264Player hp = new H264Player("file.mp4");
		
```
