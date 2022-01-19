/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.music.DrumSoundAtPos;
import com.asofterspace.assMusician.video.elements.GeometryMonster;
import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.assMusician.video.elements.StreetElement;
import com.asofterspace.assMusician.video.elements.Waveform;
import com.asofterspace.assMusician.workers.ImgSaveWorker;
import com.asofterspace.toolbox.images.ColorRGBA;
import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.GraphImage;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.sound.Beat;
import com.asofterspace.toolbox.sound.BeatStats;
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
	// * add blocks that are moving around, zooming here, staying a while, zooming there, staying a while,
	//   like during the opening credits of the movie Deja Vu
	// * add the number of this song VERY BIGLY at the start, then fade it away (without leading zeroes though!)
	// * add something like a "lasershow", e.g. a horizontal line that is connected to the origin with equidistant lines
	//   and that moves up and down
	//   (at first a horizontal top to bottom, but then also vertical left to right, etc.!)
	public void generateVideoBasedOnBeats(List<Beat> beats, List<Integer> addedSounds, int totalFrameAmount, int width, int height,
		GraphImage wavGraphImg, Waveform origWaveform, Waveform newWaveform, String songTitle,
		int framesPerFourier, int[][] fouriers, List<DrumSoundAtPos> addedDrumSounds, List<String> debugLog) {

		debugLog.add(": Pre-Processing Video Data");

		debugLog.add("  :: generating drum sound map containing " + addedDrumSounds.size() + " entries");

		Map<Integer, DrumSoundAtPos> addedDrumSoundMap = new HashMap<>();
		for (DrumSoundAtPos drumSound : addedDrumSounds) {
			addedDrumSoundMap.put(drumSound.getBeatPos(), drumSound);
		}

		debugLog.add(": Starting Frame Generation");

		System.out.println("");
		System.out.println("Generating " + totalFrameAmount + " frames...");

		BeatStats stats = new BeatStats(beats);

		Random rand = new Random();
		// nice round number
		int STAR_AMOUNT = 64;
		debugLog.add("  :: " + STAR_AMOUNT + " stars");
		List<Star> stars = new ArrayList<>();
		for (int i = 0; i < STAR_AMOUNT; i++) {
			stars.add(new Star(rand.nextInt(width), rand.nextInt(height/2)));
		}
		List<StreetElement> streetElements = new ArrayList<>();
		for (Beat beat : beats) {
			streetElements.add(new StreetElement(musicGen.beatToFrame(beat), false));
		}
		for (Integer addedSound : addedSounds) {
			streetElements.add(new StreetElement(musicGen.millisToFrame(musicGen.channelPosToMillis(addedSound)), true));
		}
		debugLog.add("  :: " + beats.size() + " street elements");

		GeometryMonster geometryMonster = new GeometryMonster(width, height);
		debugLog.add("  :: 1 geometry monster");
		int geoMaxSecondsBetweenShapes = 32;
		debugLog.add("    ::: max time between special shapes: " + (1000*geoMaxSecondsBetweenShapes) + " ms");

		// a map from frame number to beat detected there
		Map<Integer, Beat> beatMap = new HashMap<>();
		for (Beat beat : beats) {
			beatMap.put(musicGen.beatToFrame(beat), beat);
		}

		ColorRGBA trueBlack = new ColorRGBA(0, 0, 0);
		ColorRGBA trueWhite = new ColorRGBA(255, 255, 255);
		ColorRGBA origBlack = new ColorRGBA(0, 0, 0);
		// the original blue
		ColorRGBA origBlue = ColorRGBA.randomColorfulBright();
		debugLog.add("  :: first color: " + origBlue);
		// the next target blue
		ColorRGBA targetBlue = getNewColorfulColorThatIsNot(origBlue);
		// the midpoint between original and target
		ColorRGBA midBlue = ColorRGBA.max(origBlue, targetBlue);
		debugLog.add("  :: first mid color: " + midBlue);
		debugLog.add("  :: first target color: " + targetBlue);
		double targetColorProgress = 0;
		// the original blue, again - but intermixed with the midpoint or target (as just "blue" will be),
		// but NOT set to all white upon whiteout (different from how just "blue" behaves!)
		ColorRGBA geoBlue = origBlue;

		int startColorInversion = -10 * MusicGenerator.frameRate;

		List<Beat> prevBeats = new ArrayList<>();

		DefaultImageFile textOrigFile = new DefaultImageFile("video/orig.png");
		Image textOrig = textOrigFile.getImage();
		textOrig.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textOrigWhite = textOrig.copy();

		DefaultImageFile textRemixFile = new DefaultImageFile("video/remix.png");
		Image textRemix = textRemixFile.getImage();
		textRemix.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textRemixWhite = textRemix.copy();

		DefaultImageFile textLengthFile = new DefaultImageFile("video/length.png");
		Image textLength = textLengthFile.getImage();
		textLength.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textLengthWhite = textLength.copy();

		DefaultImageFile textLoudnessFile = new DefaultImageFile("video/loudness.png");
		Image textLoudness = textLoudnessFile.getImage();
		textLoudness.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textLoudnessWhite = textLoudness.copy();

		DefaultImageFile textJitterienessFile = new DefaultImageFile("video/jitterieness.png");
		Image textJitterieness = textJitterienessFile.getImage();
		textJitterieness.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textJitterienessWhite = textJitterieness.copy();

		DefaultImageFile textIntensityFile = new DefaultImageFile("video/intensity.png");
		Image textIntensity = textIntensityFile.getImage();
		textIntensity.resampleBy(MusicGenerator.width / (1920 * 4.5), MusicGenerator.width / (1920 * 4.5));
		Image textIntensityWhite = textIntensity.copy();

		double currentLoudnessScaled = 0;
		int lastLength = 0;
		long lastLoudness = 0;
		long lastJitterieness = 0;
		long lastIntensity = 0;

		int beatNum = 0;
		int drawBeatForSteps = 0;

		boolean encounteredFirstChanged = false;
		boolean encounteredChanged = false;
		boolean firstChanged = false;
		boolean curChanged = false;

		// set up debug log movements - a bit of wobbling to the left and / or right
		// by mapping from vertical location to horizontal displacement
		int debugLogFontHeight = 16;
		int debugLogTextHeight = Image.getTextHeight("Neuropol", debugLogFontHeight);

		// setup clock data
		int clockFontSize = 30;
		double clockIntermix = 0;
		double clockIntermixChange = 1 / (2.0 * MusicGenerator.frameRate);
		debugLog.add("  :: adding clocks with font size: " + clockFontSize);

		Map<Integer, Integer> debugLogWobblings = new HashMap<>();
		for (int y = - debugLogTextHeight; y < height; y++) {
			debugLogWobblings.put(y, 0);
		}
		Map<Integer, Integer> debugLogPerturbations = new HashMap<>();
		int perturbationAmount = 16;
		int perturbationWidth = width / 64;
		debugLog.add("  :: introducing " + perturbationAmount + " lateral log perturbations");
		for (int i = 0; i < perturbationAmount; i++) {
			debugLogPerturbations.put(rand.nextInt(height), rand.nextInt(perturbationWidth));
		}

		debugLog.add("  :: starting " + MusicGenerator.workerThreadAmount + " workers");

		List<ImgSaveWorker> imgSaveWorkers = new ArrayList<>();

		for (int w = 0; w < MusicGenerator.workerThreadAmount; w++) {
			System.out.println("Starting Img Save worker #" + w + "...");
			debugLog.add("    ::: starting worker #" + w);
			ImgSaveWorker worker = new ImgSaveWorker();
			imgSaveWorkers.add(worker);
			Thread workerThread = new Thread(worker);
			workerThread.start();
		}

		debugLog.add("{end log}");

		List<String> newDebugLog = new ArrayList<>();
		for (String line : debugLog) {
			line = line.toLowerCase();
			line = line.replaceAll("setting ", "set ");
			line = line.replaceAll("splitting ", "split ");
			line = line.replaceAll("using ", "use ");
			line = line.replaceAll("stopping ", "stop ");
			line = line.replaceAll("clipping ", "clip ");
			line = line.replaceAll("normalizing ", "normalize ");
			line = line.replaceAll("generating ", "generate ");
			line = line.replaceAll("ing ", " ");
			line = line.replaceAll(" assmusician ", " assMusician ");
			newDebugLog.add(line);
		}
		debugLog = newDebugLog;

		for (int step = 0; step < totalFrameAmount; step++) {

			firstChanged = false;
			curChanged = false;

			if ((step > 0) && (step % 1000 == 0)) {
				System.out.println("We are at frame " + step + "...");
			}

			// once per second
			if (step % MusicGenerator.frameRate == 0) {
				// adjust debug log perturbations
				Map<Integer, Integer> newDebugLogPerturbations = new HashMap<>();
				for (Map.Entry<Integer, Integer> entry : debugLogPerturbations.entrySet()) {
					Integer yPos = entry.getKey();
					Integer perturb = entry.getValue();
					newDebugLogPerturbations.put(yPos + rand.nextInt(9) - 4, perturb + rand.nextInt(9) - 4);
				}
				debugLogPerturbations = newDebugLogPerturbations;
				// adjust debug log positions based on perturbations
				for (int y = - debugLogTextHeight; y <= height; y++) {
					debugLogWobblings.put(y, 0);
				}
				for (Map.Entry<Integer, Integer> entry : debugLogPerturbations.entrySet()) {
					Integer yPos = entry.getKey();
					Integer perturb = entry.getValue();
					for (int y = yPos - perturb; y < yPos + perturb; y++) {
						if (debugLogWobblings.get(y) != null) {
							debugLogWobblings.put(y, debugLogWobblings.get(y) + ((perturb - Math.abs(y - yPos)) / 3));
						}
					}
				}
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
				lastIntensity = curBeat.getIntensity();

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
								wavGraphImg.drawVerticalLineAt(curBeat.getPosition(), new ColorRGBA(128, 255, 0));
							}
						}
					}
				}

				prevBeats.add(curBeat);
			}

			ColorRGBA black = origBlack;

			// we want to change color twice, each color change goes to mid, then to the color, so we need 2*2 = 4
			targetColorProgress += 4.0 / totalFrameAmount;

			ColorRGBA blue = origBlue;
			if (targetColorProgress < 1) {
				blue = ColorRGBA.intermix(origBlue, midBlue, 1 - targetColorProgress);
			} else if (targetColorProgress < 2) {
				blue = ColorRGBA.intermix(midBlue, targetBlue, 2 - targetColorProgress);
			} else {
				targetColorProgress = 0;
				origBlue = targetBlue;
				targetBlue = getNewColorfulColorThatIsNot(origBlue);
				midBlue = ColorRGBA.max(origBlue, targetBlue);
				blue = origBlue;
			}
			geoBlue = blue;
			ColorRGBA darkBlue = ColorRGBA.intermix(black, blue, 0.5);
			ColorRGBA darkerBlue = ColorRGBA.intermix(black, darkBlue, 0.5);

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
				darkBlue = new ColorRGBA(128, 128, 128);
				drawAllWhite = true;
			}

			if (skipImageDrawing) {
				continue;
			}

			// background
			Image img = new Image(width, height, black);

			// debug log
			int overallScrollHeight = (debugLog.size() * debugLogTextHeight) + height;
			for (int i = 0; i < debugLog.size(); i++) {
				int top = (i*debugLogTextHeight) + height - ((step * overallScrollHeight) / totalFrameAmount);
				int left = (11 * width) / 100;
				if (top < -debugLogTextHeight) {
					continue;
				}
				if (top > height) {
					break;
				}
				int highlightArea = 2 * height / 3;
				ColorRGBA textCol = darkBlue;
				if ((top > highlightArea) && (top <= highlightArea + debugLogTextHeight)) {
					textCol = blue;
				}
				img.drawText(debugLog.get(i), top, null, null, left + debugLogWobblings.get(top), "Neuropol", debugLogFontHeight, true, textCol);
			}

			// clock at the top left: time we already played through
			clockIntermix += clockIntermixChange;
			if (clockIntermix > 1.0) {
				clockIntermix = 1.0;
				clockIntermixChange = - clockIntermixChange;
			}
			if (clockIntermix < 0.0) {
				clockIntermix = 0.0;
				clockIntermixChange = - clockIntermixChange;
			}
			int clockBorderDistance = (int)(width*0.008);
			ColorRGBA clockColor = ColorRGBA.intermix(blue, darkBlue, clockIntermix);
			int timePassedMS = (step * 1000) / MusicGenerator.frameRate;
			img.drawText(timeToStr(timePassedMS), clockBorderDistance, null, null, clockBorderDistance, "Calibri", clockFontSize, true, clockColor);

			// clock at the top right: time remaining
			clockColor = ColorRGBA.intermix(blue, darkBlue, 1 - clockIntermix);
			int timeRemainingMS = ((totalFrameAmount - step) * 1000) / MusicGenerator.frameRate;
			img.drawText(timeToStr(timeRemainingMS), clockBorderDistance, width - clockBorderDistance, null, null, "Calibri", clockFontSize, true, clockColor);

			// right waveform in the background
			origWaveform.drawOnImageRotated(img, (int)(width*0.8), (int)(height*0.06), (int)(height*0.8), 2.0, step, totalFrameAmount, darkBlue, darkerBlue, blue, addedDrumSoundMap);

			// title
			Image textTitle = Image.createTextImage(songTitle, "Neuropol", 29, true, blue, trueBlack);
			int titleX = (width - textTitle.getWidth()) / 2;
			int titleY = (8 * height) / 1080;
			img.draw(textTitle, titleX, titleY, trueBlack);

			// stars
			for (Star star : stars) {
				star.drawOnImage(img, step, blue, black);
			}

			// horizon
			img.setLineWidth(3);
			img.drawLine(0, height/2, width-1, height/2, blue);
			// street
			img.drawLine(width/2, height/2, width/4, height-1, blue);
			img.drawLine(width/2, height/2, (3*width)/4, height-1, blue);
			// moving elements
			for (StreetElement el : streetElements) {
				el.drawOnImage(img, width, height, step, blue);
			}

			// left bottom beat indicator
			drawBeatForSteps--;
			if (curBeat != null) {
				drawBeatForSteps = 16;
				beatNum++;
			}
			ColorRGBA beat32col = darkerBlue;
			ColorRGBA beat16col = darkerBlue;
			ColorRGBA beat8col = darkerBlue;
			ColorRGBA beat4col = darkerBlue;
			ColorRGBA beat2col = darkerBlue;
			ColorRGBA beat1col = darkerBlue;
			if (drawBeatForSteps > 0) {
				if (beatNum % 32 == 0) {
					beat32col = blue;
				}
				if (beatNum % 16 == 0) {
					beat16col = blue;
				}
				if (beatNum % 8 == 0) {
					beat8col = blue;
				}
				if (beatNum % 4 == 0) {
					beat4col = blue;
				}
				if (beatNum % 2 == 0) {
					beat2col = blue;
				}
				beat1col = blue;
			}

			int beatIndicaLeft = (68 * width) / 1920;
			int beatIndicaRight = (148 * width) / 1920;
			img.drawRectangle(beatIndicaLeft, (601 * height) / 1080, beatIndicaRight, (633 * height) / 1080, beat32col);
			img.drawRectangle(beatIndicaLeft, (642 * height) / 1080, beatIndicaRight, (674 * height) / 1080, beat16col);
			img.drawRectangle(beatIndicaLeft, (683 * height) / 1080, beatIndicaRight, (715 * height) / 1080, beat8col);
			img.drawRectangle(beatIndicaLeft, (724 * height) / 1080, beatIndicaRight, (756 * height) / 1080, beat4col);
			img.drawRectangle(beatIndicaLeft, (765 * height) / 1080, beatIndicaRight, (797 * height) / 1080, beat2col);
			img.drawRectangle(beatIndicaLeft, (806 * height) / 1080, beatIndicaRight, (838 * height) / 1080, beat1col);

			// left HUD
			int hudOffset = (height * 95) / 1080; // vertical start
			int hudOffsetDiff = (height * 100) / 1080; // difference between stats
			int hudLineOffset = (height * 38) / 1080;
			int hudTextOffset = (height * 45) / 1080;
			int hudLeft = (19 * width) / 1920;
			int hudTextLeft = (15 * width) / 1920;
			int hudRight = (int) (width * 0.098);

			img.drawText(""+lastLength, hudOffset, null, null, hudLeft, "Neuropol", 29, true, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, hudRight, hudOffset + hudLineOffset, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, 0, hudOffset + hudLineOffset + hudLeft, blue);
			if (drawAllWhite) {
				img.draw(textLengthWhite, hudTextLeft, hudOffset + hudTextOffset);
			} else {
				Image curText = textLength.copy();
				curText.multiply(blue);
				img.draw(curText, hudTextLeft, hudOffset + hudTextOffset);
			}

			hudOffset += hudOffsetDiff;
			img.drawText(""+lastLoudness, hudOffset, null, null, hudLeft, "Neuropol", 29, true, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, hudRight, hudOffset + hudLineOffset, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, 0, hudOffset + hudLineOffset + (hudLeft / 2), blue);
			if (drawAllWhite) {
				img.draw(textLoudnessWhite, hudTextLeft, hudOffset + hudTextOffset);
			} else {
				Image curText = textLoudness.copy();
				curText.multiply(blue);
				img.draw(curText, hudTextLeft, hudOffset + hudTextOffset);
			}

			hudOffset += hudOffsetDiff;
			img.drawText(""+lastJitterieness, hudOffset, null, null, hudLeft, "Neuropol", 29, true, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, hudRight, hudOffset + hudLineOffset, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, 0, hudOffset + hudLineOffset - (hudLeft / 2), blue);
			if (drawAllWhite) {
				img.draw(textJitterienessWhite, hudTextLeft, hudOffset + hudTextOffset);
			} else {
				Image curText = textJitterieness.copy();
				curText.multiply(blue);
				img.draw(curText, hudTextLeft, hudOffset + hudTextOffset);
			}

			hudOffset += hudOffsetDiff;
			img.drawText(""+lastIntensity, hudOffset, null, null, hudLeft, "Neuropol", 29, true, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, hudRight, hudOffset + hudLineOffset, blue);
			img.drawLine(hudLeft, hudOffset + hudLineOffset, 0, hudOffset + hudLineOffset - hudLeft, blue);
			if (drawAllWhite) {
				img.draw(textIntensityWhite, hudTextLeft, hudOffset + hudTextOffset);
			} else {
				Image curText = textIntensity.copy();
				curText.multiply(blue);
				img.draw(curText, hudTextLeft, hudOffset + hudTextOffset);
			}

			// geo monster
			geometryMonster.drawOnImage(img, width, height, step, totalFrameAmount, currentLoudnessScaled, blue, geoBlue,
				firstChanged, curChanged, encounteredChanged, curBeat, geoMaxSecondsBetweenShapes);

			img.setLineWidth(1);

			origWaveform.drawOnImage(img, width, (int)(height*0.885), step, totalFrameAmount, blue, darkBlue, trueWhite);

			int x = width / 240;
			int y = (int) (height * 0.85);
			if (drawAllWhite) {
				img.draw(textOrigWhite, x, y);
			} else {
				Image curText = textOrig.copy();
				curText.multiply(blue);
				img.draw(curText, x, y);
			}

			newWaveform.drawOnImage(img, width, (int)(height*0.965), step, totalFrameAmount, blue, darkBlue, trueWhite);

			x = width / 240;
			y = (int) (height * 0.93);
			if (drawAllWhite) {
				img.draw(textRemixWhite, x, y);
			} else {
				Image curText = textRemix.copy();
				curText.multiply(blue);
				img.draw(curText, x, y);
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
				int newX = width + 1;
				if (i+1 < curFourier.length) {
					newX = width + 1 - (((curFourier[i+1] * curiness) + (nextFourier[i+1] * nextiness)) / (framesPerFourier * 5000));
				}
				img.drawLine((3*x +   newX) / 4, 4*y+2, width+1, 4*y+2, blue);
				img.drawLine((  x +   newX) / 2, 4*y+1, width+1, 4*y+1, blue);
				img.drawLine((  x + 3*newX) / 4, 4*y  , width+1, 4*y  , blue);
			}

			// fade in from black
			if (step < totalFrameAmount / 100) {
				img.intermix(trueBlack, (float) (step / (totalFrameAmount / 100.0)));
			}

			workerFindLoop:
			while (true) {
				for (int w = 0; w < imgSaveWorkers.size(); w++) {
					ImgSaveWorker worker = imgSaveWorkers.get(w);
					if (!worker.isBusy()) {
						worker.workOn(img, workDir.getAbsoluteDirname() + "/pic" + StrUtils.leftPad0(step, 5) + ".png");
						break workerFindLoop;
					}
				}
				Utils.sleep(1000);
			}
		}

		System.out.println("Waiting until all workers are done...");

		boolean allDone = false;
		while (!allDone) {
			allDone = true;
			for (ImgSaveWorker worker : imgSaveWorkers) {
				if (worker.isBusy()) {
					allDone = false;
					break;
				}
			}
			Utils.sleep(1000);
		}

		System.out.println("Stopping all workers...");

		for (ImgSaveWorker worker : imgSaveWorkers) {
			worker.stop();
		}

		DefaultImageFile doneWaveFile = new DefaultImageFile(workDir, "waveform_upon_video_done.png");
		doneWaveFile.assign(wavGraphImg);
		doneWaveFile.save();

		System.out.println("All " + totalFrameAmount + " frames generated!");
	}

	private ColorRGBA getNewColorfulColorThatIsNot(ColorRGBA origColor) {

		ColorRGBA result = ColorRGBA.randomColorfulBright();

		int trials = 0;
		int MAX_TRIALS = 128;

		while ((trials < MAX_TRIALS) && origColor.fastSimilar(result)) {
			trials++;
			result = ColorRGBA.randomColorfulBright();
		}

		return result;
	}

	private String timeToStr(int timeInMS) {
		int minutes = timeInMS / 60000;
		timeInMS = timeInMS % 60000;
		String result = ""+timeInMS;
		while (result.length() < 5) {
			result = "0" + result;
		}
		result = result.substring(0, result.length() - 3) + "." + result.substring(result.length() - 3);
		result = minutes + ":" + result;
		return result;
	}
}
