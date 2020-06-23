/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.assMusician.MusicGenerator;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;


public class StreetElement {

	// the frame at which this street element should arrive at the bottom
	private int arrivalFrame;

	private int startFrame;

	private boolean important;

	// if we have 25 frames per second, we want to show the animation for 50 frames, such that it is
	// visible for 2 seconds
	private int FRAME_LENGTH = MusicGenerator.frameRate * 2;


	public StreetElement(int arrivalFrame, boolean important) {
		this.arrivalFrame = arrivalFrame;
		this.startFrame = arrivalFrame - FRAME_LENGTH;
		this.important = important;
	}

	public void drawOnImage(Image img, int width, int height, int step, ColorRGB color) {
		if (step < startFrame) {
			return;
		}
		if (step > arrivalFrame) {
			return;
		}

		double movBy = ((double) step - startFrame) / FRAME_LENGTH;
		movBy = movBy * movBy * movBy * movBy * movBy;
		double movByPerc = movBy;
		double scaleTo = height/2;
		movBy *= scaleTo;

		if ((height/2) + (int)movBy >= height) {
			return;
		}

		if (important) {
			color = ColorRGB.intermix(new ColorRGB(255, 255, 255), color, 0.5);
		}

		img.drawLine((width/2)-(int)((movByPerc*width)/4), (height/2) + (int)movBy, (width/2)+(int)((movByPerc*width)/4), (height/2) + (int)movBy, color);
	}
}
