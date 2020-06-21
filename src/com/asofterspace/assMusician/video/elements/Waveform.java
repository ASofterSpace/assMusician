/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.assMusician.music.DrumSoundAtPos;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.music.SoundData;

import java.util.List;


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

			int fadeLen = width / 150;
			double fadeLenDouble = fadeLen;

			// for the first ten pixels, fade in the waveform
			if (x < leftOffset + fadeLen) {
				img.drawLine(x, mid, x, bottom, ColorRGB.intermix(midgroundColor, new ColorRGB(0, 0, 0), (x - leftOffset) / fadeLenDouble));
				img.drawLine(x, top, x, mid, ColorRGB.intermix(foregroundColor, new ColorRGB(0, 0, 0), (x - leftOffset) / fadeLenDouble));
				continue;
			}

			// show a light spot in the middle
			int halfWidth = width / 2;

			if ((x > halfWidth - fadeLen) && (x < halfWidth)) {
				img.drawLine(x, mid, x, bottom, ColorRGB.intermix(midgroundColor, highlightColor, (halfWidth - x) / fadeLenDouble));
				img.drawLine(x, top, x, mid, ColorRGB.intermix(foregroundColor, highlightColor, (halfWidth - x) / fadeLenDouble));
				continue;
			}

			if (x == halfWidth) {
				img.drawLine(x, top, x, bottom, highlightColor);
				continue;
			}

			if ((x > halfWidth) && (x < halfWidth + fadeLen)) {
				img.drawLine(x, mid, x, bottom, ColorRGB.intermix(midgroundColor, highlightColor, (x - halfWidth) / fadeLenDouble));
				img.drawLine(x, top, x, mid, ColorRGB.intermix(foregroundColor, highlightColor, (x - halfWidth) / fadeLenDouble));
				continue;
			}

			// for all other pixels, just show the waveform regularly
			img.drawLine(x, mid, x, bottom, midgroundColor);
			img.drawLine(x, top, x, mid, foregroundColor);
		}
	}

	public void drawOnImageRotated(Image img, int horzPos, int top, int bottom, double widthModifier,
		int step, int totalFrameAmount, ColorRGB foregroundColor, ColorRGB midgroundColor, ColorRGB highlightColor,
		List<DrumSoundAtPos> addedDrumSounds) {

		int[] leftData = soundData.getLeftData();
		int[] rightData = soundData.getRightData();
		int height = bottom - top;

		// how much do we want to zoom in on time?
		int timeDilationParameter = 4;

		for (int y = 0; y < height; y++) {
			int loudMax = 0;
			int loudMin = 0;
			for (long i = (long) ((step + ((y - (height/2.0)) / timeDilationParameter)) * (long) leftData.length) / totalFrameAmount; i < (long) ((step + ((y + 1 - (height/2.0)) / timeDilationParameter)) * (long) leftData.length) / totalFrameAmount; i++) {
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
			int left = horzPos - (int)((loudMax * 64 * widthModifier) / (8*16*16*16));
			int mid = horzPos;
			int right = horzPos - (int)((loudMin * 64 * widthModifier) / (8*16*16*16));

			int fadeLen = height / 32;
			double fadeLenDouble = fadeLen;

			// at the top and bottom, fade in the waveform
			if (y < fadeLen) {
				img.drawLine(mid, y+top, right, y+top, ColorRGB.intermix(midgroundColor, new ColorRGB(0, 0, 0), y / fadeLenDouble));
				img.drawLine(left, y+top, mid, y+top, ColorRGB.intermix(foregroundColor, new ColorRGB(0, 0, 0), y / fadeLenDouble));
				continue;
			}
			if (y > height - fadeLen) {
				img.drawLine(mid, y+top, right, y+top, ColorRGB.intermix(midgroundColor, new ColorRGB(0, 0, 0), (height - y) / fadeLenDouble));
				img.drawLine(left, y+top, mid, y+top, ColorRGB.intermix(foregroundColor, new ColorRGB(0, 0, 0), (height - y) / fadeLenDouble));
				continue;
			}

			// show a light spot in the middle
			int halfHeight = height / 2;

			if ((y > halfHeight - fadeLen) && (y < halfHeight)) {
				img.drawLine(mid, y+top, right, y+top, ColorRGB.intermix(midgroundColor, highlightColor, (halfHeight - y) / fadeLenDouble));
				img.drawLine(left, y+top, mid, y+top, ColorRGB.intermix(foregroundColor, highlightColor, (halfHeight - y) / fadeLenDouble));
				continue;
			}

			if (y == halfHeight) {
				img.drawLine(left, y+top, right, y+top, highlightColor);
				continue;
			}

			if ((y > halfHeight) && (y < halfHeight + fadeLen)) {
				img.drawLine(mid, y+top, right, y+top, ColorRGB.intermix(midgroundColor, highlightColor, (y - halfHeight) / fadeLenDouble));
				img.drawLine(left, y+top, mid, y+top, ColorRGB.intermix(foregroundColor, highlightColor, (y - halfHeight) / fadeLenDouble));
				continue;
			}

			// for all other pixels, just show the waveform regularly
			img.drawLine(mid, y+top, right, y+top, midgroundColor);
			img.drawLine(left, y+top, mid, y+top, foregroundColor);
		}
	}

}
