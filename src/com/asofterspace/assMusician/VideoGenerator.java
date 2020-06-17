/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.GeometryMonster;
import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.assMusician.video.elements.StreetElement;
import com.asofterspace.assMusician.video.elements.Waveform;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.GraphImage;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.music.Beat;
import com.asofterspace.toolbox.music.BeatStats;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class VideoGenerator {

	private final static boolean skipImageDrawing = false;

	private MusicGenerator musicGen;

	private Directory workDir;


	public VideoGenerator(MusicGenerator musicGen, Directory workDir) {

		this.musicGen = musicGen;

		this.workDir = workDir;
	}

	// TODO :: add asofterspace fun logos to the start of the video?
	// like, they are there (and stay there for a second or so) and then the actual stuff fades in?
	public void generateVideoBasedOnBeats(List<Beat> beats, int totalFrameAmount, int width, int height,
		GraphImage wavGraphImg, Waveform origWaveform, Waveform newWaveform, String songTitle,
		int framesPerFourier, int[][] fouriers) {

		System.out.println("");
		System.out.println("Generating " + totalFrameAmount + " frames...");

		BeatStats stats = new BeatStats(beats);

		Random rand = new Random();
		// nice round number
		int STAR_AMOUNT = 64;
		List<Star> stars = new ArrayList<>();
		for (int i = 0; i < STAR_AMOUNT; i++) {
			stars.add(new Star(rand.nextInt(width), rand.nextInt(height/2)));
		}
		List<StreetElement> streetElements = new ArrayList<>();
		for (Beat beat : beats) {
			streetElements.add(new StreetElement(musicGen.beatToFrame(beat)));
		}
		GeometryMonster geometryMonster = new GeometryMonster(width, height);

		// a map from frame number to beat detected there
		Map<Integer, Beat> beatMap = new HashMap<>();
		for (Beat beat : beats) {
			beatMap.put(musicGen.beatToFrame(beat), beat);
		}

		ColorRGB trueBlack = new ColorRGB(0, 0, 0);
		ColorRGB trueWhite = new ColorRGB(255, 255, 255);
		ColorRGB origBlack = new ColorRGB(0, 0, 0);
		ColorRGB origBlue = ColorRGB.randomColorfulBright();
		ColorRGB origDarkBlue = ColorRGB.intermix(origBlack, origBlue, 0.5);
		// ColorRGB blue = new ColorRGB(255, 0, 128);

		int startColorInversion = -10 * MusicGenerator.frameRate;

		List<Beat> prevBeats = new ArrayList<>();

		DefaultImageFile textOrigFile = new DefaultImageFile("video/orig.png");
		Image textOrig = textOrigFile.getImage();
		textOrig.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textOrigWhite = textOrig.copy();
		textOrig.multiply(origBlue);

		DefaultImageFile textRemixFile = new DefaultImageFile("video/remix.png");
		Image textRemix = textRemixFile.getImage();
		textRemix.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textRemixWhite = textRemix.copy();
		textRemix.multiply(origBlue);

		DefaultImageFile textLengthFile = new DefaultImageFile("video/length.png");
		Image textLength = textLengthFile.getImage();
		textLength.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textLengthWhite = textLength.copy();
		textLength.multiply(origBlue);

		DefaultImageFile textLoudnessFile = new DefaultImageFile("video/loudness.png");
		Image textLoudness = textLoudnessFile.getImage();
		textLoudness.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textLoudnessWhite = textLoudness.copy();
		textLoudness.multiply(origBlue);

		DefaultImageFile textJitterienessFile = new DefaultImageFile("video/jitterieness.png");
		Image textJitterieness = textJitterienessFile.getImage();
		textJitterieness.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textJitterienessWhite = textJitterieness.copy();
		textJitterieness.multiply(origBlue);

		Image textTitle = Image.createTextImage(songTitle, "Neuropol", 29, true, origBlue, trueBlack);
		Image textTitleWhite = Image.createTextImage(songTitle, "Neuropol", 29, true, trueWhite, trueBlack);

		double currentLoudnessScaled = 0;
		int lastLength = 0;
		long lastLoudness = 0;
		long lastJitterieness = 0;

		boolean encounteredFirstChanged = false;
		boolean encounteredChanged = false;
		boolean firstChanged = false;
		boolean curChanged = false;

		for (int step = 0; step < totalFrameAmount; step++) {

			firstChanged = false;
			curChanged = false;

			if ((step > 0) && (step % 1000 == 0)) {
				System.out.println("We are at frame " + step + "...");
			}

			// is there a beat right at this location?
			Beat curBeat = beatMap.get(step);

			if (curBeat != null) {

				if (curBeat.getChanged()) {
					curChanged = true;
					encounteredChanged = true;
					if (!encounteredFirstChanged) {
						encounteredFirstChanged = true;
						firstChanged = true;
					}
				}

				currentLoudnessScaled = (curBeat.getLoudness() * 1.0) / stats.getMaxLoudness();
				lastLength = curBeat.getLength();
				lastLoudness = curBeat.getLoudness();
				if (stats.getMaxJigglieness() == 0) {
					lastJitterieness = 255;
				} else {
					lastJitterieness = (curBeat.getJigglieness() * 255) / stats.getMaxJigglieness();
				}

				int beatLookbackForFlicker = 20;
				if (prevBeats.size() > beatLookbackForFlicker) {
					long lastBeatsAverageLoudness = 0;
					for (int i = prevBeats.size() - beatLookbackForFlicker; i < prevBeats.size(); i++) {
						lastBeatsAverageLoudness += prevBeats.get(i).getLoudness();
					}
					lastBeatsAverageLoudness = lastBeatsAverageLoudness / beatLookbackForFlicker;

					// is this beat more than twice as loud as the previous ones were on average?
					// AND is this beat more than the half loudness? (as we don't want to flash e.g.
					// during the intro...)
					if ((curBeat.getLoudness() > 2 * lastBeatsAverageLoudness) &&
						(curBeat.getLoudness() > stats.getMaxLoudness() / 2)) {
						// then check the upcoming beats - if the loudness stays this way for at least
						// a few more, then actully do this...
						List<Beat> nextBeats = new ArrayList<>();
						for (int i = step + 1; i < totalFrameAmount; i++) {
							Beat nextBeat = beatMap.get(i);
							if (nextBeat != null) {
								nextBeats.add(nextBeat);
								if (nextBeats.size() == beatLookbackForFlicker) {
									break;
								}
							}
						}
						if (nextBeats.size() == beatLookbackForFlicker) {
							long nextBeatsAverageLoudness = 0;
							for (int i = 0; i < nextBeats.size(); i++) {
								nextBeatsAverageLoudness += nextBeats.get(i).getLoudness();
							}
							nextBeatsAverageLoudness = nextBeatsAverageLoudness / beatLookbackForFlicker;
							if (nextBeatsAverageLoudness > 2 * lastBeatsAverageLoudness) {
								// then start flickering for a while!
								startColorInversion = step;
								wavGraphImg.drawVerticalLineAt(curBeat.getPosition(), new ColorRGB(128, 255, 0));
							}
						}
					}
				}

				prevBeats.add(curBeat);
			}

			ColorRGB black = origBlack;
			ColorRGB blue = origBlue;
			ColorRGB darkBlue = origDarkBlue;
			int ssCI = step - startColorInversion;
			boolean drawAllWhite = false;
			// when flickering, have everything being bright be a bit shorter than everything being dark
			if ((ssCI < MusicGenerator.frameRate / 10) ||
				((ssCI > (2.25 * MusicGenerator.frameRate) / 10) && (ssCI < (3 * MusicGenerator.frameRate) / 10)) ||
				((ssCI > (4.25 * MusicGenerator.frameRate) / 10) && (ssCI < (5 * MusicGenerator.frameRate) / 10)) ||
				((ssCI > (6.25 * MusicGenerator.frameRate) / 10) && (ssCI < (7 * MusicGenerator.frameRate) / 10))) {
				// flicker!
				/*
				// instead of flickering the entire background as well...
				blue = origBlack;
				black = origBlue;
				*/
				// just flicker the foreground to white and back!
				blue = trueWhite;
				darkBlue = new ColorRGB(128, 128, 128);
				drawAllWhite = true;
			}

			if (skipImageDrawing) {
				continue;
			}

			// background
			Image img = new Image(width, height, black);
			img.setLineWidth(3);

			// title
			int titleX = (width - textTitle.getWidth()) / 2;
			int titleY = (8 * height) / 1080;
			if (drawAllWhite) {
				img.draw(textTitleWhite, titleX, titleY);
			} else {
				img.draw(textTitle, titleX, titleY);
			}

			// stars
			for (Star star : stars) {
				star.drawOnImage(img, step, blue, black);
			}

			// horizon
			img.drawLine(0, height/2, width-1, height/2, blue);
			// street
			img.drawLine(width/2, height/2, width/4, height-1, blue);
			img.drawLine(width/2, height/2, (3*width)/4, height-1, blue);
			// moving elements
			for (StreetElement el : streetElements) {
				el.drawOnImage(img, width, height, step, blue);
			}

			// left HUD
			img.drawText(""+lastLength, (64 * height) / 1080, null, null, (19 * width) / 1920, "Neuropol", 29, true, blue);
			img.drawLine(0, (height * 102) / 1080, (int) (width * 0.098), (height * 102) / 1080, blue);
			if (drawAllWhite) {
				img.draw(textLengthWhite, (15 * width) / 1920, (109 * height) / 1080);
			} else {
				img.draw(textLength, (15 * width) / 1920, (109 * height) / 1080);
			}
			img.drawText(""+lastLoudness, (187 * height) / 1080, null, null, (19 * width) / 1920, "Neuropol", 29, true, blue);
			img.drawLine(0, (height * 225) / 1080, (int) (width * 0.098), (height * 225) / 1080, blue);
			if (drawAllWhite) {
				img.draw(textLoudnessWhite, (15 * width) / 1920, (232 * height) / 1080);
			} else {
				img.draw(textLoudness, (15 * width) / 1920, (232 * height) / 1080);
			}
			img.drawText(""+lastJitterieness, (310 * height) / 1080, null, null, (19 * width) / 1920, "Neuropol", 29, true, blue);
			img.drawLine(0, (height * 348) / 1080, (int) (width * 0.098), (height * 348) / 1080, blue);
			if (drawAllWhite) {
				img.draw(textJitterienessWhite, (15 * width) / 1920, (355 * height) / 1080);
			} else {
				img.draw(textJitterieness, (15 * width) / 1920, (355 * height) / 1080);
			}

			geometryMonster.drawOnImage(img, width, height, step, currentLoudnessScaled, blue, origBlue, firstChanged, curChanged, encounteredChanged);

			img.setLineWidth(1);

			origWaveform.drawOnImage(img, width, (int)(height*0.885), step, totalFrameAmount, blue, darkBlue, trueWhite);

			int x = width / 240;
			int y = (int) (height * 0.85);
			if (drawAllWhite) {
				img.draw(textOrigWhite, x, y);
			} else {
				img.draw(textOrig, x, y);
			}

			newWaveform.drawOnImage(img, width, (int)(height*0.965), step, totalFrameAmount, blue, darkBlue, trueWhite);

			x = width / 240;
			y = (int) (height * 0.93);
			if (drawAllWhite) {
				img.draw(textRemixWhite, x, y);
			} else {
				img.draw(textRemix, x, y);
			}

			// draw Fourier at the top right, turned sideways, with lower base stuff shown at the bottom
			int fourierNum = step / framesPerFourier;
			if (fourierNum >= fouriers.length) {
				fourierNum = fouriers.length - 1;
			}
			int[] curFourier = fouriers[fourierNum];
			int[] nextFourier = curFourier;
			if (fourierNum+1 < fouriers.length) {
				nextFourier = fouriers[fourierNum+1];
			}
			int nextiness = step % framesPerFourier;
			int curiness = framesPerFourier - nextiness;
			for (int i = 0; i < curFourier.length; i++) {
				y = curFourier.length - (i + 1);
				x = width + 1 - (((curFourier[i] * curiness) + (nextFourier[i] * nextiness)) / (framesPerFourier * 5000));
				img.drawLine(x, 4*y+3, width, 4*y+3, blue);
				int newX = width;
				if (i+1 < curFourier.length) {
					newX = width + 1 - (((curFourier[i+1] * curiness) + (nextFourier[i+1] * nextiness)) / (framesPerFourier * 5000));
				}
				img.drawLine((3*x +   newX) / 4, 4*y+2, width, 4*y+2, blue);
				img.drawLine((  x +   newX) / 2, 4*y+1, width, 4*y+1, blue);
				img.drawLine((  x + 3*newX) / 4, 4*y  , width, 4*y  , blue);
			}

			DefaultImageFile curImgFile = new DefaultImageFile(
				workDir.getAbsoluteDirname() + "/pic" + StrUtils.leftPad0(step, 5) + ".png"
			);
			// fade in from black
			if (step < totalFrameAmount / 100) {
				img.intermix(trueBlack, (float) (step / (totalFrameAmount / 100.0)));
			}
			curImgFile.assign(img);
			curImgFile.save();
		}

		DefaultImageFile doneWaveFile = new DefaultImageFile(workDir, "waveform_upon_video_done.png");
		doneWaveFile.assign(wavGraphImg);
		doneWaveFile.save();

		System.out.println("All " + totalFrameAmount + " frames generated!");
	}
}
