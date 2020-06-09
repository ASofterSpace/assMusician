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
		ColorRGB foregroundColor) {

		int[] leftData = soundData.getLeftData();

		for (int x = 0; x < width; x++) {
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
				}
			}
			int top = vertPos - ((loudMax * 64) / (8*16*16*16));
			int bottom = vertPos - ((loudMin * 64) / (8*16*16*16));
			img.drawLine(x, top, x, bottom, foregroundColor);
		}
	}
}
