package com.twilight.h264.player;

import com.twilight.h264.decoder.AVFrame;

public interface RGBListener {
	void imageUpdated(AVFrame image);
}