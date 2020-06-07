/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.GeometryMonster;
import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.assMusician.video.elements.StreetElement;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.DefaultImageFile;
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

	private MusicGenerator musicGen;

	private Directory workDir;


	public VideoGenerator(MusicGenerator musicGen, Directory workDir) {

		this.musicGen = musicGen;

		this.workDir = workDir;
	}

	public void generateVideoBasedOnBeats(List<Beat> beats, int totalFrameAmount, int width, int height) {

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

		ColorRGB origBlack = new ColorRGB(0, 0, 0);
		ColorRGB origBlue = ColorRGB.randomColorfulBright();
		// ColorRGB blue = new ColorRGB(255, 0, 128);

		boolean lastBeatQuiteQuiet = true;
		int startColorInversion = -10 * MusicGenerator.frameRate;

		for (int step = 0; step < totalFrameAmount; step++) {
			if ((step > 0) && (step % 1000 == 0)) {
				System.out.println("We are at frame " + step + "...");
			}

			// is there a beat right at this location?
			Beat curBeat = beatMap.get(step);
			if (curBeat != null) {
				// is this beat louder than 0.9*max, and the previous one was not?
				if (curBeat.getLoudness() > stats.getMaxLoudness() * 0.9) {
					if (lastBeatQuiteQuiet) {
						// then start flickering for a while!
						startColorInversion = step;
					}
					lastBeatQuiteQuiet = false;
				} else {
					lastBeatQuiteQuiet = true;
				}
			}

			ColorRGB black = origBlack;
			ColorRGB blue = origBlue;
			int ssCI = step - startColorInversion;
			if ((ssCI < MusicGenerator.frameRate / 4) ||
				((ssCI > (2 * MusicGenerator.frameRate) / 4) && (ssCI < (3 * MusicGenerator.frameRate) / 4)) ||
				((ssCI > (4 * MusicGenerator.frameRate) / 4) && (ssCI < (5 * MusicGenerator.frameRate) / 4)) ||
				((ssCI > (6 * MusicGenerator.frameRate) / 4) && (ssCI < (7 * MusicGenerator.frameRate) / 4))) {
				// flicker!
				blue = origBlack;
				black = origBlue;
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

			geometryMonster.drawOnImage(img, width, height, step, blue);

			DefaultImageFile curImgFile = new DefaultImageFile(
				workDir.getAbsoluteDirname() + "/pic" + StrUtils.leftPad0(step, 5) + ".png"
			);
			// fade in from black
			if (step < totalFrameAmount / 100) {
				img.intermix(black, (float) (step / (totalFrameAmount / 100.0)));
			}
			curImgFile.assign(img);
			curImgFile.save();
		}
		System.out.println("All " + totalFrameAmount + " frames generated!");
	}
}
