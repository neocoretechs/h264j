package com.twilight.h264.player;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

public class PlayerFrame extends JPanel {
	public Image lastFrame;
	public void setLastFrame(Image lf) { 
		if( lf == null ) return; 
		if(lastFrame == null) { lastFrame = lf; return; }
		synchronized(lastFrame) { 
			lastFrame = lf; 
		} 
	}
	public void paint(Graphics g) {
		synchronized(lastFrame) {
		  if(lastFrame != null) {
			g.drawImage(lastFrame, 0, 0, lastFrame.getWidth(this), lastFrame.getHeight(this), this);
		  } // if
		}
	}

}
