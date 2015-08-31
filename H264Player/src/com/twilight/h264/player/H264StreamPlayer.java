package com.twilight.h264.player;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.swing.JFrame;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.DebugTool;
import com.twilight.h264.decoder.H264Context;
import com.twilight.h264.decoder.H264Data;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.H264PredictionContext;
import com.twilight.h264.decoder.MpegEncContext;

public class H264StreamPlayer implements Runnable {
	
	public static final int INBUF_SIZE = 65535;
	private PlayerFrame displayPanel;
	private int[] buffer = null;
	InputStream stream;
	boolean shouldRun = true;

	public H264StreamPlayer(InputStream stream) {
		this.stream = stream;
			JFrame frame = new JFrame("Player");
			displayPanel = new PlayerFrame();

			frame.getContentPane().add(displayPanel, BorderLayout.CENTER);

			// Finish setting up the frame, and show it.
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});
			displayPanel.setVisible(true);
			frame.pack();
			frame.setVisible(true);
			frame.setSize(new Dimension(600, 800));
			
			new Thread(this).start();
	}
	
	public void run() {
		System.out.println("Playing ");
		while(shouldRun)
			playStream();
	}
	
	@SuppressWarnings("unused")
	public boolean playStream() {
	    H264Decoder codec;
	    MpegEncContext c= null;
	    //FileInputStream fin = null;
	    int frame, len;
	    int[] got_picture = new int[1];
	    //File f = new File(filename);
	    AVFrame picture;
	    //uint8_t inbuf[INBUF_SIZE + H264Context.FF_INPUT_BUFFER_PADDING_SIZE];
	    byte[] inbuf = new byte[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    int[] inbuf_int = new int[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    //char buf[1024];
	    byte[] buf = new byte[1024];
	    AVPacket avpkt = new AVPacket();

	    avpkt.av_init_packet();

	    /* set end of buffer to 0 (this ensures that no overreading happens for damaged mpeg streams) */
	    Arrays.fill(inbuf, INBUF_SIZE, MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, (byte)0);

	    System.out.println("Video decoding\n");

	    /* find the mpeg1 video decoder */
	    codec = new H264Decoder();
	    if (codec == null) {
	    	System.out.println("codec not found\n");
	        throw new RuntimeException("codec not found\n");
	    }

	    c= MpegEncContext.avcodec_alloc_context();
	    picture= AVFrame.avcodec_alloc_frame();

	    if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0)
	        c.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

	    /* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

	    /* open it */
	    if (c.avcodec_open(codec) < 0) {
	    	System.out.println("could not open codec\n");
	        throw new RuntimeException("could not open codec\n");
	    }

	    try {
		    /* the codec gives us the frame size, in samples */
	
		    frame = 0;
		    int dataPointer;

		    // avpkt must contain exactly 1 NAL Unit in order for decoder to decode correctly.
	    	// thus we must read until we get next NAL header before sending it to decoder.
			// Find 1st NAL
			int[] cacheRead = new int[3];
			cacheRead[0] = stream.read();
			cacheRead[1] = stream.read();
			cacheRead[2] = stream.read();
			
			while(!(
					cacheRead[0] == 0x00 &&
					cacheRead[1] == 0x00 &&
					cacheRead[2] == 0x01 
					)) {
				 cacheRead[0] = cacheRead[1];
				 cacheRead[1] = cacheRead[2];
				 cacheRead[2] = stream.read();
			} // while
	    	
			boolean hasMoreNAL = true;
			
			// 4 first bytes always indicate NAL header
			inbuf_int[0]=inbuf_int[1]=inbuf_int[2]=0x00;
			inbuf_int[3]=0x01;
			
			while(hasMoreNAL) {
				dataPointer = 4;
				// Find next NAL
				cacheRead[0] = stream.read();
				if(cacheRead[0]==-1) hasMoreNAL = false;
				cacheRead[1] = stream.read();
				if(cacheRead[1]==-1) hasMoreNAL = false;
				cacheRead[2] = stream.read();
				if(cacheRead[2]==-1) hasMoreNAL = false;
				while(!(
						cacheRead[0] == 0x00 &&
						cacheRead[1] == 0x00 &&
						cacheRead[2] == 0x01 
						) && hasMoreNAL) {
					 inbuf_int[dataPointer++] = cacheRead[0];
					 cacheRead[0] = cacheRead[1];
					 cacheRead[1] = cacheRead[2];
					 cacheRead[2] = stream.read();
					if(cacheRead[2]==-1) hasMoreNAL = false;
				} // while

				avpkt.size = dataPointer;

		        avpkt.data_base = inbuf_int;
		        avpkt.data_offset = 0;

		        try {
			        while (avpkt.size > 0) {
			            len = c.avcodec_decode_video2(picture, got_picture, avpkt);
			            if (len < 0) {
			                System.out.println("Error while decoding frame "+ frame);
			                // Discard current packet and proceed to next packet
			                break;
			            } // if
			            if (got_picture[0]!=0) {
			            	picture = c.priv_data.displayPicture;
		
							int bufferSize = picture.imageWidth * picture.imageHeight;
							if (buffer == null || bufferSize != buffer.length) {
								buffer = new int[bufferSize];
							}
							FrameUtils.YUV2RGB(picture, buffer);			
							displayPanel.lastFrame = displayPanel.createImage(new MemoryImageSource(picture.imageWidth
									, picture.imageHeight, buffer, 0, picture.imageWidth));
							displayPanel.invalidate();
							displayPanel.updateUI();			            	
			            }
			            avpkt.size -= len;
			            avpkt.data_offset += len;
			        }
		        } catch(Exception ie) {
		        	// Any exception, we should try to proceed reading next packet!
		        	ie.printStackTrace();
		        } // try
				
			} // while
					
	
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

	    c.avcodec_close();
	    c = null;
	    picture = null;
	    System.out.println("Stop playing video.");
	    
	    return true;
	}
	

}
