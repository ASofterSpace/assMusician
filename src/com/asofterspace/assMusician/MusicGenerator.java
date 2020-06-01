/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.GraphDataPoint;
import com.asofterspace.toolbox.images.GraphImage;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.WavFile;
import com.asofterspace.toolbox.utils.CallbackWithString;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;


public class MusicGenerator {

	private final WavFile WAV_REV_DRUM;
	private final WavFile WAV_SNR_DRUM;
	private final WavFile WAV_TOM1_DRUM;
	private final WavFile WAV_TOM2_DRUM;
	private final WavFile WAV_TOM3_DRUM;
	private final WavFile WAV_TOM4_DRUM;
	private final WavFile WAV_SMALL_F_TIMPANI;

	private Database database;

	private Directory inputDir;
	private Directory outputDir;
	private Directory workDir;

	private int byteRate;
	private int bytesPerSample;
	private int[] wavDataLeft;
	private int[] wavDataRight;

	private int graphImageHeight = 256;
	private GraphImage wavGraphImg;

	// width (px), height (px) and frame rate (fps) of the resulting video
	/*
	private int width = 1920;
	private int height = 1080;
	private int frameRate = 60;
	*/
	private int width = 640;
	private int height = 360;
	private int frameRate = 30;

	public MusicGenerator(Database database, Directory inputDir, Directory outputDir) {
		this.database = database;
		this.inputDir = inputDir;
		this.outputDir = outputDir;

		workDir = new Directory("work");

		// rev drum for the beginning of each (!) song, as a unifying feature :)
		WAV_REV_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-RevCrash.wav");

		// snr drum, kind of reminding us of Fun - Some Nights...
		WAV_SNR_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-Snr01.wav");

		// deep drum sounds
		WAV_TOM1_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-Tom01.wav");
		WAV_TOM2_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-Tom02.wav");
		WAV_TOM3_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-Tom03.wav");
		WAV_TOM4_DRUM = new WavFile(inputDir, "drums/Drum Kits/Kurzweil Kit 01/CYCdh_Kurz01-Tom04.wav");

		// timpanis
		WAV_SMALL_F_TIMPANI = new WavFile(inputDir, "noiiz/timpani/TimpaniSmall_F_395_SP.wav");
	}

	public void addDrumsToSong(File originalSong) {

		System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Adding drums to " + originalSong.getLocalFilename());

		workDir.clear();

		String ffmpegPath = database.getRoot().getString("ffmpegPath");
		File workSong = new File(workDir, "song.wav");

		// extract the audio of the original song (e.g. in case the original is a music video)
		String ffmpegInvocation = ffmpegPath;
		ffmpegInvocation += " -i \"";
		ffmpegInvocation += originalSong.getAbsoluteFilename();
		// bit rate 192 kbps, sample rate 44100 Hz, audio channels: 2 (stereo), audio codec: PCM 16, no video
		ffmpegInvocation += "\" -ab 192000 -ar 44100 -ac 2 -acodec pcm_s16le -vn \"";
		ffmpegInvocation += workSong.getAbsoluteFilename();
		ffmpegInvocation += "\"";
		System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Executing " + ffmpegInvocation);
		IoUtils.execute(ffmpegInvocation, new CallbackWithString() {
			public void call(String line) {
				System.out.println(line);
			}
		});

		// load the extracted audio
		WavFile wav = new WavFile(workSong);
		this.byteRate = wav.getByteRate();
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		wavDataLeft = wav.getLeftData();
		wavDataRight = wav.getRightData();
		// scale(0.5);

		// the visual output graph gets 600 px width per minute of song
		wavGraphImg = new GraphImage(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

		List<GraphDataPoint> wavData = new ArrayList<>();
		int position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		wavGraphImg.setDataColor(new ColorRGB(0, 0, 255));
		wavGraphImg.setAbsoluteDataPoints(wavData);
		DefaultImageFile basicWavFile = new DefaultImageFile(workDir, "waveform_before.png");
		basicWavFile.assign(wavGraphImg);
		basicWavFile.save();

		// add drums
		addDrums();

		// handle the overflow by normalizing the entire song
		normalize();

		// save the new song as audio
		WavFile newSongFile = new WavFile(workDir, "our_song.wav");
		newSongFile.setNumberOfChannels(2);
		newSongFile.setSampleRate(wav.getSampleRate());
		newSongFile.setByteRate(byteRate);
		newSongFile.setBitsPerSample(bytesPerSample * 8);
		newSongFile.setLeftData(wavDataLeft);
		newSongFile.setRightData(wavDataRight);
		newSongFile.save();

		// save the new song as waveform image for diagnostic purposes
		wavGraphImg = new GraphImage(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

		wavData = new ArrayList<>();
		position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		wavGraphImg.setDataColor(new ColorRGB(0, 0, 255));
		wavGraphImg.setAbsoluteDataPoints(wavData);
		DefaultImageFile doneWaveFile = new DefaultImageFile(workDir, "waveform_upon_being_done.png");
		doneWaveFile.assign(wavGraphImg);
		doneWaveFile.save();

		// generate a video for the song
		// maybe some geometric thingy in the middle (maybe moving based on the music, maybe made to look a bit gritty and real, not JUST abstract), and some pop colors around,
		// maybe slowly (?) changing colors, maybe even blinking whenever a drum is played etc.
		// (at least the stuff that we add we have full information about, so we can do whatever we want with it!)
		// also see for inpiration about videos: Gealdýr - Sær
		// EVEN BETTER - for greatness of videoness, see those funky cyberpunk thingies Michi likes,
		//   which are showing a cockpit in purple/pink and travel along a road... and then have the
		//   road (and ultimately, maybe even a space-parcours etc.) autogenerated and somesuch!
		//   with blackish blackground! and funk!

		boolean skipVideo = true;
		boolean reuseExistingVideo = true;

		if (skipVideo) {
			System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Song creation for " + originalSong.getLocalFilename() + " done, video creation aborted!");
			return;
		}

		String vidpicdirStr = workDir.getAbsoluteDirname();

		if (reuseExistingVideo) {

			vidpicdirStr = inputDir.getAbsoluteDirname() + "\\default_vid_lady_wood";

		} else {

			int totalFrameAmount = calcTotalFrameAmount();
			Random rand = new Random();
			// nice round number
			int STAR_AMOUNT = 64;
			List<Star> stars = new ArrayList<>();
			for (int i = 0; i < STAR_AMOUNT; i++) {
				stars.add(new Star(rand.nextInt(width), rand.nextInt(height/2)));
			}
			for (int i = 0; i < totalFrameAmount; i++) {
				ColorRGB black = new ColorRGB(0, 0, 0);
				ColorRGB blue = new ColorRGB(0, 128, 255);

				double step = ((double) i) / (10 * frameRate);

				Image img = new Image(width, height);
				img.setLineWidth(3);

				// background
				img.drawRectangle(0, 0, width-1, height-1, black);

				// stars
				for (Star star : stars) {
					star.calcFrame(step);
					if (star.getBrightness() > 0.001) {
						ColorRGB starColor = ColorRGB.intermix(blue, black, star.getBrightness());
						img.setPixelSafely(star.getX(), star.getY(), starColor);
						img.setPixelSafely(star.getX()-1, star.getY(), starColor);
						img.setPixelSafely(star.getX()-2, star.getY(), starColor);
						img.setPixelSafely(star.getX()+1, star.getY(), starColor);
						img.setPixelSafely(star.getX()+2, star.getY(), starColor);
						img.setPixelSafely(star.getX(), star.getY()-1, starColor);
						img.setPixelSafely(star.getX(), star.getY()-2, starColor);
						img.setPixelSafely(star.getX(), star.getY()+1, starColor);
						img.setPixelSafely(star.getX(), star.getY()+2, starColor);
					}
				}

				// horizon
				img.drawLine(0, height/2, width-1, height/2, blue);
				// street
				img.drawLine(width/2, height/2, width/4, height-1, blue);
				img.drawLine(width/2, height/2, (3*width)/4, height-1, blue);
				// moving elements
				double movBy = step * 4;
				while (movBy > 2) {
					movBy -= 2;
				}
				movBy = movBy * movBy * movBy * movBy * movBy;
				movBy = movBy / 32;
				double movByPerc = movBy;
				double scaleTo = height/2;
				movBy *= scaleTo;
				img.drawLine((width/2)-(int)((movByPerc*width)/4), (height/2) + (int)movBy, (width/2)+(int)((movByPerc*width)/4), (height/2) + (int)movBy, blue);

				DefaultImageFile curImgFile = new DefaultImageFile(
					workDir.getAbsoluteDirname() + "/pic" + StrUtils.leftPad0(i, 5) + ".png"
				);
				curImgFile.assign(img);
				curImgFile.save();
			}
		}

		// splice the generated audio together with the generated video
		File outputFile = new File(outputDir, originalSong.getLocalFilenameWithoutType() + ".mp4");
		outputFile.delete();
		ffmpegInvocation = ffmpegPath;
		ffmpegInvocation += " -framerate " + frameRate;
		ffmpegInvocation += " -i \"" + vidpicdirStr + "\\pic%05d.png\"";
		ffmpegInvocation += " -i \"";
		ffmpegInvocation += newSongFile.getAbsoluteFilename();
		ffmpegInvocation += "\" -c:v libx264 -vf \"fps=" + frameRate + ",format=yuv420p,";
		ffmpegInvocation += "scale=" + width + ":" + height + "\" -c:a aac -map 0:v:0 -map 1:a:0 \"";
		ffmpegInvocation += outputFile.getAbsoluteFilename();
		ffmpegInvocation += "\"";
		System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Executing " + ffmpegInvocation);
		IoUtils.execute(ffmpegInvocation, new CallbackWithString() {
			public void call(String line) {
				System.out.println(line);
			}
		});

		// upload it to youtube
		// (and in the description, link to the original song)
		// TODO

		System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] " + originalSong.getLocalFilename() + " done!");
	}

	// TODO: idea for a better algorithm:
	// * try to get heights first (kind of like here), then make up several predictions over when
	//   the next height should happen, and check which one of those ends up being the best one...
	//   or maybe split the song into several (overlapping?) areas and try for each area to get
	//   all of the beats and then sync them up?
	// * also, at the start do not add the drums, but add them slowly, and even more towards the
	//   end! maybe 10 areas, first no drums, next four a bit of drums, final five LOTS of drums?
	// * have different modes of drums that can be added
	//   >> for one, get inspired by drums in Oh Land - White Nights
	//   >> for another, get inspired by Fun - Some Nights
	//   >> for another, get inspired by Alan Walker - Game Of Thrones (not just the drums, but the whole song!)
	//   >> finally, also by Zara Larsson - Bad Blood
	//   ... then let all of them be generated, and manually choose the best music out of maybe
	//   ten that have been generated and upload that one :)
	// * also make fun noises with water in the bathtub, record them, and put them instead of
	//   drums just for fun / April 1st edition? :D
	private void addDrums() {

		int addTimes = 0;

		/*
		// ALGORITHM 1

		// actually put them into the song
		addDrum(wavDataLeft, 3000);
		addDrum(wavDataRight, 3000);
		addTimes = 2;
		*/

		/*
		// ALGORITHM 2

		// find the beat in the loaded song - by sliding a window of several samples,
		// and only when all samples within the window are increasing, calling it
		// a maximum
		int windowLength = 32;
		int instrumentRing = 0;

		for (int i = 0; i < wavDataLeft.length - windowLength; i++) {

			boolean continuousInWindow = true;

			for (int w = i+1; w < i + windowLength; w++) {
				if (wavDataLeft[w - 1] > wavDataLeft[w]) {
					continuousInWindow = false;
					break;
				}
			}

			if (continuousInWindow) {

				switch (instrumentRing) {
					case 0:
						addWavMono(WAV_TOM1_DRUM, i, 2);
						break;
					case 1:
						addWavMono(WAV_TOM2_DRUM, i, 2);
						break;
					case 2:
						addWavMono(WAV_TOM3_DRUM, i, 2);
						break;
					case 3:
						addWavMono(WAV_TOM4_DRUM, i, 2);
						break;
					case 4:
						addWavMono(WAV_SMALL_F_TIMPANI, i, 1);
						break;
					default:
						break;
				}
				instrumentRing++;
				if (instrumentRing > 4) {
					instrumentRing = 0;
				}

				// jump ahead 200 ms (so that we do not identify this exact maximum
				// again as new maximum a second time)
				i += millisToChannelPos(200);

				addTimes++;
			}
		}
		*/

		// ALGORITHM 3

		// We first iterate over the entire song and try to find maxima, by finding
		// first the maxima over each 0.2 s region, and then we go over all the maxima
		// from end to start and keep only the ones which are preceded by lower ones
		// For this (for now) we only use the left channel...
		List<Integer> maximumPositions = new ArrayList<>();
		List<Integer> potentialMaximumPositions = new ArrayList<>();
		int localMaxPos = 0;
		int localMax = 0;
		int regionSize = millisToChannelPos(200);
		for (int i = 0; i < wavDataLeft.length; i++) {
			if (wavDataLeft[i] > localMax) {
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
			if (i % regionSize == 0) {
				potentialMaximumPositions.add(localMaxPos);
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
		}
		for (int i = potentialMaximumPositions.size() - 1; i > 2; i--) {

				// if the previous maximum is smaller
			if ((wavDataLeft[potentialMaximumPositions.get(i-1)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-2)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-3)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and this maximum is above volume 1/8
				(wavDataLeft[potentialMaximumPositions.get(i)] > 16*16*16)) {

				// then we actually fully accept it as maximum :)
				maximumPositions.add(potentialMaximumPositions.get(i));
			}
		}

		/*
		int instrumentRing = 0;
		for (Integer maxPos : maximumPositions) {

			addWavMono(WAV_TOM1_DRUM, maxPos, 1);

			addTimes++;
		}
		*/

		// ALGORITHM 3.5

		// We take algorithm 3 as basis, but now after having found our maxima we try to space them around equi-distant
		// However, we cannot just do this across the whole song, so instead we try to first split up the song into
		// larger areas (by getting the distances between maxima, and taking the 16 largest distances as split locations),
		// and then we equi-distantize the maxima across these song parts
		// Map<Integer, Integer> maxPosToDistance = new HashMap<>();
		TreeMap<Integer, Integer> distanceToMaxPos = new TreeMap<>();
		// maxPosToDistance.put(maximumPositions.get(0), 0);
		distanceToMaxPos.put(0, maximumPositions.get(0));
		for (int i = 1; i < maximumPositions.size(); i++) {
			distanceToMaxPos.put(maximumPositions.get(i) - maximumPositions.get(i-1), maximumPositions.get(i));
			// maxPosToDistance.put(maximumPositions.get(i), maximumPositions.get(i) - maximumPositions.get(i-1));
		}
		// get the highest distances:
		List<Integer> highestDist = new ArrayList<>();
		Collection<Integer> distances = distanceToMaxPos.values();
		List<Integer> distanceList = new ArrayList<>(distances);
		for (int i = distanceList.size() - 16; i < distanceList.size(); i++) {
			if (i >= 0) {
				highestDist.add(distanceList.get(i)+1);
			}
		}
		Collections.sort(highestDist);
		Collections.sort(maximumPositions);

		// now for each song part...
		int j = 0;
		for (int i = 0; i < highestDist.size(); i++) {
			// ... assemble all the beats that we found
			System.out.println("Song part " + i + " at " + highestDist.get(i) + ", beats:");
			List<Integer> partBeats = new ArrayList<>();
			List<Integer> newBeats = new ArrayList<>();
			for (; j < maximumPositions.size(); j++) {
				if (maximumPositions.get(j) < highestDist.get(i)) {
					partBeats.add(maximumPositions.get(j));
					System.out.println(maximumPositions.get(j));
				} else {
					break;
				}
			}
			// now actually equidistantize the beats in these song parts...
			// first of all, fill beat gaps - so if we detected X X   X X, make it into X X X X X
			// for that, get the average distance between beats in this song part:
			int avgDist = 0;
			for (int k = 1; k < partBeats.size(); k++) {
				avgDist += partBeats.get(k) - partBeats.get(k-1);
			}
			avgDist /= partBeats.size() - 1;
			// give at least 200 ms between beats (at least for the ones we are adding in between now...)
			if (avgDist < millisToChannelPos(200)) {
				avgDist = millisToChannelPos(200);
			}
			// now, check if between any two beats there is twice that much space, or three times, etc.
			for (int k = 1; k < partBeats.size(); k++) {
				int curDist = partBeats.get(k) - partBeats.get(k-1);
				if (curDist > (3 * avgDist) / 2) {
					if (curDist > (5 * avgDist) / 2) {
						if (curDist > (7 * avgDist) / 2) {
							newBeats.add((partBeats.get(k) + partBeats.get(k-1)) / 4);
							newBeats.add(((partBeats.get(k) + partBeats.get(k-1)) * 2) / 4);
							newBeats.add(((partBeats.get(k) + partBeats.get(k-1)) * 3) / 4);
						} else {
							newBeats.add((partBeats.get(k) + partBeats.get(k-1)) / 3);
							newBeats.add(((partBeats.get(k) + partBeats.get(k-1)) * 2) / 3);
						}
					} else {
						newBeats.add((partBeats.get(k) + partBeats.get(k-1)) / 2);
					}
				}
			}

			partBeats.addAll(newBeats);
			Collections.sort(partBeats);

			int instrumentRing = 0;
			for (int k = 0; k < partBeats.size(); k++) {

				int curBeat = partBeats.get(k);

				/*
				switch (instrumentRing) {
					case 0:
						addWavMono(WAV_TOM1_DRUM, curBeat, 1);
						break;
					case 1:
						addWavMono(WAV_TOM2_DRUM, curBeat, 1);
						break;
					case 2:
						addWavMono(WAV_TOM3_DRUM, curBeat, 1);
						break;
					case 3:
						addWavMono(WAV_TOM4_DRUM, curBeat, 1);
						break;
					default:
						break;
				}
				instrumentRing++;
				if (instrumentRing > 3) {
					instrumentRing = 0;
				}
				*/

				// one possible pattern - try out others as well!
				int curBeatLen = avgDist;
				if (k < partBeats.size() - 1) {
					if (partBeats.get(k+1) - curBeat < curBeatLen) {
						curBeatLen = partBeats.get(k+1) - curBeat;
					}
					addWavMono(WAV_TOM1_DRUM, curBeat, 2);
					addWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), 2);
					addWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), 2);
					addWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), 2);
					addWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), 2);
				} else {
					// at the end of each segment, a big wooosh!
					addWavMono(WAV_SMALL_F_TIMPANI, curBeat, 4);
				}

				addTimes++;
			}
		}

		// add rev sound at the beginning at high volume
		addWavMono(WAV_REV_DRUM, 0, 8);
		addTimes++;

		System.out.println("We added " + addTimes + " drum sounds!");
	}

	/**
	 * Add a WAV file by mixing it in left and right at a given position (expressed as sample byte number!)
	 */
	private void addWav(WavFile wav, int samplePos, int wavVolume) {

		wav.normalizeTo16Bits();

		int[] newLeft = wav.getLeftData();
		int[] newRight = wav.getRightData();

		// int pos = millisToChannelPos(posInMillis);
		int pos = samplePos;

		int len = newLeft.length;
		if (len + pos > wavDataLeft.length) {
			len = wavDataLeft.length - pos;
		}

		for (int i = 0; i < len; i++) {
			wavDataLeft[i+pos] = mixin(wavDataLeft[i+pos], wavVolume * newLeft[i]);
			wavDataRight[i+pos] = mixin(wavDataRight[i+pos], wavVolume * newRight[i]);
		}
	}

	/**
	 * Add a WAV file by mixing it in left and right at a given position (expressed as sample byte number!),
	 * but taking the new WAV file as mono, not as stereo file!
	 */
	private void addWavMono(WavFile wav, int samplePos, int wavVolume) {

		wav.normalizeTo16Bits();

		int[] newMono = wav.getMonoData();

		// int pos = millisToChannelPos(posInMillis);
		int pos = samplePos;

		int len = newMono.length;
		if (len + pos > wavDataLeft.length) {
			len = wavDataLeft.length - pos;
		}

		for (int i = 0; i < len; i++) {
			if (i+pos >= wavDataLeft.length) {
				return;
			}
			if ((i+pos < 0) || (i < 0)) {
				return;
			}
			wavDataLeft[i+pos] = mixin(wavDataLeft[i+pos], wavVolume * newMono[i]);
			wavDataRight[i+pos] = mixin(wavDataRight[i+pos], wavVolume * newMono[i]);
		}
	}

	private void scale(double scaleFactor) {
		for (int i = 0; i < wavDataLeft.length; i++) {
			wavDataLeft[i] = (int) (scaleFactor * wavDataLeft[i]);
			wavDataRight[i] = (int) (scaleFactor * wavDataRight[i]);
		}
	}

	private int mixin(int one, int two) {

		// add the two values, handle the overflow later:
		return one + two;
	}

	/*
	private int handleOverflow(int val) {

		// now handle overflow - not Integer overflow, but two-byte-overflow,
		// so 16*16*16*16 overflow
		// (the way how we handle it: maximum both one and two are at max value,
		// so the sum is 2*16*16*16*16... so if the sum is over 8*16*16*16, then
		// we just smush the remainder up to 2*... into the space until 16*16*16*16)
		if (val > 8*16*16*16) {
			val -= 8*16*16*16;
			val /= 2;
			val += 8*16*16*16;
		} else if (val < -8*16*16*16) {
			val += 8*16*16*16;
			val /= 2;
			val -= 8*16*16*16;
		}
		if (val > 16*16*16*16) {
			val = 16*16*16*16;
		} else if (val < -16*16*16*16) {
			val = -16*16*16*16;
		}
		return val;
	}
	*/

	private void normalize() {

		int max = 0;
		for (int i = 0; i < wavDataLeft.length; i++) {
			if (wavDataLeft[i] > max) {
				max = wavDataLeft[i];
			}
			if (-wavDataLeft[i] > max) {
				max = -wavDataLeft[i];
			}
			if (wavDataRight[i] > max) {
				max = wavDataRight[i];
			}
			if (-wavDataRight[i] > max) {
				max = -wavDataRight[i];
			}
		}
		for (int i = 0; i < wavDataLeft.length; i++) {
			wavDataLeft[i] = (int) ((wavDataLeft[i] * (long) 8*16*16*16) / max);
			wavDataRight[i] = (int) ((wavDataRight[i] * (long) 8*16*16*16) / max);
		}
	}

	private void addDrum(int[] songData, int posInMillis) {

		int[] drumData = generateDrum();

		int pos = millisToChannelPos(posInMillis);

		int len = drumData.length;
		if (len + pos > songData.length) {
			len = songData.length - pos;
		}

		for (int i = 0; i < len; i++) {
			songData[pos + i] = mixin(songData[pos + i], drumData[i]);
		}
	}

	/**
	 * Takes a position in milliseconds and returns the exact offset into the int array at which
	 * this time is occurring in the song data
	 */
	private int millisToChannelPos(long posInMillis) {
		long NUM_OF_CHANNELS = 2;
		return (int) ((posInMillis * byteRate) / (1000 * bytesPerSample * NUM_OF_CHANNELS));
	}

	private int channelPosToMillis(long channelPos) {
		long NUM_OF_CHANNELS = 2;
		return (int) ((channelPos * 1000 * bytesPerSample * NUM_OF_CHANNELS) / byteRate);
	}

	private int calcTotalFrameAmount() {
		return (channelPosToMillis(wavDataLeft.length) * frameRate) / 1000;
	}

	private int[] generateDrum() {

		// duration: 2 seconds
		int durationMillis = 2000;

		// frequency: 50 Hz
		double frequency = 50;

		// max is 8*16*16*16 - we take half that
		int amplitude = 4*16*16*16;

		int[] data = new int[millisToChannelPos(durationMillis)];
		int bytesPerSecond = millisToChannelPos(1000);

		for (int i = 0; i < data.length; i++) {
			double inout = 1.0;
			if (i < data.length / 10) {
				inout = (10.0 * i) / data.length;
			}
			if (i > data.length / 2) {
				inout = 2 - ((2.0 * i) / data.length);
			}
			data[i] = (int) (inout * amplitude * Math.sin((12.56637 * i * frequency) / bytesPerSecond));
		}

		return data;
	}
}
