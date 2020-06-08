/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.GeometryMonster;
import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.assMusician.video.elements.StreetElement;
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

	public void generateVideoBasedOnBeats(List<Beat> beats, int totalFrameAmount, int width, int height,
		GraphImage wavGraphImg) {

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
		ColorRGB origBlack = new ColorRGB(0, 0, 0);
		ColorRGB origBlue = ColorRGB.randomColorfulBright();
		// ColorRGB blue = new ColorRGB(255, 0, 128);

		int startColorInversion = -10 * MusicGenerator.frameRate;

		List<Beat> prevBeats = new ArrayList<>();

		double currentLoudnessScaled = 0;

		for (int step = 0; step < totalFrameAmount; step++) {
			if ((step > 0) && (step % 1000 == 0)) {
				System.out.println("We are at frame " + step + "...");
			}

			// is there a beat right at this location?
			Beat curBeat = beatMap.get(step);

			if (curBeat != null) {

				currentLoudnessScaled = (curBeat.getLoudness() * 1.0) / stats.getMaxLoudness();

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
			int ssCI = step - startColorInversion;
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
				blue = new ColorRGB(255, 255, 255);
			}

			if (skipImageDrawing) {
				continue;
			}

			Image img = new Image(width, height);
			img.setLineWidth(3);

			// background
			img.drawRectangle(0, 0, width-1, height-1, black);

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

			geometryMonster.drawOnImage(img, width, height, step, currentLoudnessScaled, blue);

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
