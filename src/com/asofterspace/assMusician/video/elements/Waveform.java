/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.music.SoundData;


public class Waveform {

	private SoundData soundData;


	public Waveform(SoundData soundData) {
		this.soundData = soundData;
	}

	public void drawOnImage(Image img, int width, int vertPos, int step, int totalFrameAmount,
		ColorRGB foregroundColor, ColorRGB midgroundColor, ColorRGB highlightColor) {

		int[] leftData = soundData.getLeftData();
		int[] rightData = soundData.getRightData();

		int leftOffset = 28;

		for (int x = leftOffset; x < width; x++) {
			int loudMax = 0;
			int loudMin = 0;
			for (long i = ((step + x - (width/2)) * (long) leftData.length) / totalFrameAmount; i < ((1 + step + x - (width/2)) * (long) leftData.length) / totalFrameAmount; i++) {
				int offset = (int) i;
				if ((offset >= 0) && (offset < leftData.length)) {
					if (leftData[offset] > loudMax) {
						loudMax = leftData[offset];
					}
					if (leftData[offset] < loudMin) {
						loudMin = leftData[offset];
					}
					if (rightData[offset] > loudMax) {
						loudMax = rightData[offset];
					}
					if (rightData[offset] < loudMin) {
						loudMin = rightData[offset];
					}
				}
			}
			// 64 is the vertical height in both (!) directions off zero in which we want to show the waveform
			// 8*16*16*16 is the maximum positive value that is possible as loudness
			int top = vertPos - ((loudMax * 64) / (8*16*16*16));
			int mid = vertPos;
			int bottom = vertPos - ((loudMin * 64) / (8*16*16*16));
			if (x < leftOffset + 10) {
				// for the first ten pixels, fade in the waveform
				ColorRGB black = new ColorRGB(0, 0, 0);
				if (x == width / 2) {
					img.drawLine(x, top, x, bottom, ColorRGB.intermix(highlightColor, black, (x - leftOffset) / 10.0));
				} else {
					img.drawLine(x, mid, x, bottom, ColorRGB.intermix(midgroundColor, black, (x - leftOffset) / 10.0));
					img.drawLine(x, top, x, mid, ColorRGB.intermix(foregroundColor, black, (x - leftOffset) / 10.0));
				}
			} else {
				if (x == width / 2) {
					img.drawLine(x, top, x, bottom, highlightColor);
				} else {
					img.drawLine(x, mid, x, bottom, midgroundColor);
					img.drawLine(x, top, x, mid, foregroundColor);
				}
			}
		}
	}
}
