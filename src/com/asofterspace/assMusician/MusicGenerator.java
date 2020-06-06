/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.Star;
import com.asofterspace.assMusician.video.elements.StreetElement;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


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

	// the data that we read from the original input wav file, and some additional
	// data that we add directly to it
	private int[] wavDataLeft;
	private int[] wavDataRight;

	// most of the data that we add during the runtime of the song, which will be
	// intermixed with wavData after all else is done, but with an additional fade
	// in and fade out effect
	private long[] fadeDataLeft;
	private long[] fadeDataRight;

	private int graphImageHeight = 256;
	private GraphImage wavGraphImg;

	// width (px), height (px) and frame rate (fps) of the resulting video
	/**/
	public final static int width = 1920;
	public final static int height = 1080;
	public final static int frameRate = 60;
	/**/
	/*
	public final static int width = 640;
	public final static int height = 360;
	public final static int frameRate = 30;
	*/


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

		// cut off silence from the front and back (basically trim() for the wav file... ^^)
		trimWav();

		fadeDataLeft = new long[wavDataLeft.length];
		fadeDataRight = new long[wavDataRight.length];
		for (int i = 0; i < wavDataLeft.length; i++) {
			fadeDataLeft[i] = 0;
			fadeDataRight[i] = 0;
		}
		// scale(0.5);

		// the visual output graph gets 600 px width per minute of song
		wavGraphImg = new GraphImage();
		wavGraphImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

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
		List<Integer> drumBeats = addDrums();

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

		boolean skipVideo = false;
		boolean reuseExistingVideo = false;

		if (skipVideo) {
			System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Song creation for " + originalSong.getLocalFilename() + " done, video creation aborted!");
			return;
		}

		String vidpicdirStr = workDir.getAbsoluteDirname();

		if (reuseExistingVideo) {

			vidpicdirStr = inputDir.getAbsoluteDirname() + "\\default_vid_hotter_than_hell_2";

		} else {

			int totalFrameAmount = calcTotalFrameAmount();
			System.out.println("");
			System.out.println("Generating " + totalFrameAmount + " frames...");
			Random rand = new Random();
			// nice round number
			int STAR_AMOUNT = 64;
			List<Star> stars = new ArrayList<>();
			for (int i = 0; i < STAR_AMOUNT; i++) {
				stars.add(new Star(rand.nextInt(width), rand.nextInt(height/2)));
			}
			List<StreetElement> streetElements = new ArrayList<>();
			for (Integer beat : drumBeats) {
				streetElements.add(new StreetElement(millisToFrame(channelPosToMillis(beat))));
			}
			for (int step = 0; step < totalFrameAmount; step++) {
				if ((step > 0) && (step % 1000 == 0)) {
					System.out.println("We are at frame " + step + "...");
				}
				ColorRGB black = new ColorRGB(0, 0, 0);
				ColorRGB blue = new ColorRGB(0, 128, 255);

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
				for (StreetElement el : streetElements) {
					el.drawOnImage(img, width, height, step, blue);
				}

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
	// returns a list of positions at which beats were, in the very end, added in the song
	private List<Integer> addDrums() {

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
		int regionSize = millisToChannelPos(100);
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

		Collections.sort(maximumPositions);

		// now iterate over all the found maximum positions, and whenever the distance between some is small-ish
		// (let's say 0.5s), we mush them together into one
		int smooshSize = millisToChannelPos(500);
		List<Integer> smooshedMaximumPositions = new ArrayList<>();

		for (int i = maximumPositions.size() - 1; i >= 0; i--) {
			int j = i - 1;
			int smooshedVal = maximumPositions.get(i);
			while (j >= 0) {
				if (maximumPositions.get(i) - maximumPositions.get(j) < smooshSize) {
					smooshedVal += maximumPositions.get(j);
					j--;
				} else {
					break;
				}
			}
			int amountSmooshed = i - j;
			j++;
			i = j;
			smooshedMaximumPositions.add(smooshedVal / amountSmooshed);
		}

		maximumPositions = smooshedMaximumPositions;

		Collections.sort(maximumPositions);

		for (int i = 0; i < maximumPositions.size(); i++) {
			wavGraphImg.drawVerticalLineAt(maximumPositions.get(i), new ColorRGB(255, 0, 0));

			/*
			// DEBUG beat detection visualization
			DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_detection_" + StrUtils.leftPad0(i, 3) + ".png");
			wavImgFile.assign(wavGraphImg);
			wavImgFile.save();
			*/
		}

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_detection.png");
		wavImgFile.assign(wavGraphImg);
		wavImgFile.save();

		/*
		int instrumentRing = 0;
		for (Integer maxPos : maximumPositions) {

			addWavMono(WAV_TOM1_DRUM, maxPos, 1);

			addTimes++;
		}
		*/

		// ALGORITHM 3.6

		// We take algorithm 3 as basis and try to get one number for the song's overall bpm (beats per minute)
		// To do so, we look at all pairs of detected maxima, and their distances as raw beat lengths
		// We then scale the raw beat lengths (by doubling or halfing againd again) until they all fall
		// into our preferred bpm band - that is, we would like to have between 90 and 180 bpm (we have
		// to choose some range, and this range seems like it will make the resulting sound quite energetic,
		// which we like!)
		// We then have lots and lots of estimates for the bpm, which we all put into buckets - and the
		// largest bucket wins!

		Collections.sort(maximumPositions);

		// each bpm candidate is an int representing a bpm value times 10 (so that we have a bit more accuracy),
		// mapping to an int which represents how many values we have put into this bucket
		int BUCKET_ACCURACY_FACTOR = 1;
		int MIN_BPM = 90;
		int MAX_BPM = 180;
		int LOOKBACK = 1;
		Map<Integer, Integer> bpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			bpmCandidates.put(curBpm, 0);
		}

		for (int i = 0; i < maximumPositions.size(); i++) {
			// actually, instead of looking at all pairs...
			// for (int j = 0; j < i; j++) {
			// we just want to look at the closest other beat, having a lookback of just 1
			// (after looking at a histogram of the bpm for different lookback values)
			for (int j = i - LOOKBACK; j < i; j++) {
				if (j < 0) {
					continue;
				}
				int curDist = maximumPositions.get(i) - maximumPositions.get(j);
				int curDiffInMs = channelPosToMillis(curDist);

				// a difference of 1 ms means that there are 60*1000 beats per minute,
				// and we have the additional *10 to get more accuracy
				int curBpm = (60*1000*BUCKET_ACCURACY_FACTOR) / curDiffInMs;

				// scale the bpm into the range that we are interested in
				while (curBpm < MIN_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm *= 2;
				}
				while (curBpm > MAX_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm /= 2;
				}

				if (bpmCandidates.get(curBpm) != null) {
					bpmCandidates.put(curBpm, bpmCandidates.get(curBpm) + 1);
				} else {
					bpmCandidates.put(curBpm, 1);
				}
			}
		}

		// output buckets as histogram
		int graphWidth = (MAX_BPM - MIN_BPM) * BUCKET_ACCURACY_FACTOR * 10;
		GraphImage histogramImg = new GraphImage();
		histogramImg.setInnerWidthAndHeight(graphWidth, graphImageHeight);

		List<GraphDataPoint> histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		DefaultImageFile histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_histogram_for_bpm.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm
		Map<Integer, Integer> smoothBpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			int curAmount = 0;
			for (int i = 1; i < 1500; i++) {
				if (bpmCandidates.get(curBpm-i) != null) {
					curAmount += bpmCandidates.get(curBpm-i);
				}
				if (bpmCandidates.get(curBpm+i) != null) {
					curAmount += bpmCandidates.get(curBpm+i);
				}
			}
			if (bpmCandidates.get(curBpm) != null) {
				curAmount += bpmCandidates.get(curBpm) * 2;
			}
			smoothBpmCandidates.put(curBpm, curAmount);
		}
		bpmCandidates = smoothBpmCandidates;

		// output smoothened buckets as histogram
		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_histogram_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		int largestBucketContentAmount = 0;
		int largestBucketBpm = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		double bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		System.out.println("We detected " + bpm + " beats per minute, " +
			"with the largest bucket containing " + largestBucketContentAmount + " values...");

		// generate beats based on the detected bpm, but still try to locally align to the closest
		// detected beat, e.g. align to the next one to the right if there is one to the left or
		// right of the current beat and the distance to it is less than 1/10 of a beat...
		List<Integer> bpmBasedBeats = new ArrayList<>();
		/*for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos((long) ((1000*60) / bpm))) {
			bpmBasedBeats.add(i);
			wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
		}*/
		int generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		int uncertainty = generatedBeatDistance / 5;
		int maxPosI = 0;
		// such that we do not have to worry about overflowing the maximumPositions list,
		// we just add an extra beat far after everything else
		Collections.sort(maximumPositions);
		maximumPositions.add((wavDataLeft.length + uncertainty) * 2);
		for (int i = 0; i < wavDataLeft.length; i += generatedBeatDistance) {
			while (maximumPositions.get(maxPosI) < i - uncertainty) {
				maxPosI++;
			}
			if (maximumPositions.get(maxPosI) < i + uncertainty) {
				while (maximumPositions.get(maxPosI) < i) {
					maxPosI++;
				}
				if (maxPosI < 1) {
					i = maximumPositions.get(maxPosI);
				} else {
					if (maximumPositions.get(maxPosI) - i < i - maximumPositions.get(maxPosI-1)) {
						i = maximumPositions.get(maxPosI);
					} else {
						i = maximumPositions.get(maxPosI-1);
					}
				}
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
			} else {
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
			}
			bpmBasedBeats.add(i);
		}

		// ALGORITHM 3.7

		// Now, after all that is done... the beats are detected, the bpm decided, the beats generated
		// and subsequently aligned against the ones previously detected... we have a problem: as we
		// just aligned the beats, some distances between beats are (noticeably!) different - e.g.
		// if we had beats at 1, 3, 5, 7, and then aligned it and it is now 1, 3, 4.8, 6.8, then the
		// distances from 3 to 4.8 being suddenly different will sound weird... so let's smoothen it!
		// that is, for each we get the distance to front and back, and align to the middle of that:
		// 1, 2.9, 4.9, 6.8... and again! ... and then we are good! :D

		int SMOOTH_AMOUNT = 2;
		for (int i = 0; i < SMOOTH_AMOUNT; i++) {
			bpmBasedBeats = smoothenBeats(bpmBasedBeats);
		}

		Collections.sort(bpmBasedBeats);

		int instrumentRing = 0;
		int curBeatLen = 0;
		for (int k = 0; k < bpmBasedBeats.size(); k++) {

			int curBeat = bpmBasedBeats.get(k);
			if (k + 1 < bpmBasedBeats.size()) {
				curBeatLen = bpmBasedBeats.get(k+1) - curBeat;
			}

			addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2);
			addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), 2);
			addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), 2);
			addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), 2);
			addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), 2);

			addTimes++;
		}


		/*
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
			if (partBeats.size() - 1 > 0) {
				avgDist /= partBeats.size() - 1;
			} else {
				avgDist = 0;
			}
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
							wavGraphImg.drawVerticalLineAt((partBeats.get(k) + partBeats.get(k-1)) / 4, new ColorRGB(128, 128, 0));
							wavGraphImg.drawVerticalLineAt(((partBeats.get(k) + partBeats.get(k-1)) * 2) / 4, new ColorRGB(128, 128, 0));
							wavGraphImg.drawVerticalLineAt(((partBeats.get(k) + partBeats.get(k-1)) * 3) / 4, new ColorRGB(128, 128, 0));
						} else {
							newBeats.add((partBeats.get(k) + partBeats.get(k-1)) / 3);
							newBeats.add(((partBeats.get(k) + partBeats.get(k-1)) * 2) / 3);
							wavGraphImg.drawVerticalLineAt((partBeats.get(k) + partBeats.get(k-1)) / 3, new ColorRGB(128, 128, 0));
							wavGraphImg.drawVerticalLineAt(((partBeats.get(k) + partBeats.get(k-1)) * 2) / 3, new ColorRGB(128, 128, 0));
						}
					} else {
						newBeats.add((partBeats.get(k) + partBeats.get(k-1)) / 2);
						wavGraphImg.drawVerticalLineAt((partBeats.get(k) + partBeats.get(k-1)) / 2, new ColorRGB(128, 128, 0));
					}
				}
			}

			partBeats.addAll(newBeats);
			Collections.sort(partBeats);

			int instrumentRing = 0;
			for (int k = 0; k < partBeats.size(); k++) {

				int curBeat = partBeats.get(k);

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
					wavGraphImg.drawVerticalLineAt(curBeat, new ColorRGB(0, 255, 255));
				}

				addTimes++;
			}
		}
		*/

		wavImgFile = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition.png");
		wavImgFile.assign(wavGraphImg);
		wavImgFile.save();

		// add rev sound at the beginning at high volume
		addWavMono(WAV_REV_DRUM, 0, 8);
		addTimes++;

		// fade in the faded wav
		for (int i = 0; i < wavDataLeft.length / 6; i++) {
			fadeDataLeft[i] = (long) ((fadeDataLeft[i] * i) / (wavDataLeft.length / 6.0));
			fadeDataRight[i] = (long) ((fadeDataRight[i] * i) / (wavDataLeft.length / 6.0));
		}

		// fade out the faded wav
		for (int i = wavDataLeft.length - (wavDataLeft.length / 12); i < wavDataLeft.length; i++) {
			fadeDataLeft[i] = (long) ((fadeDataLeft[i] * (wavDataLeft.length - i)) / (wavDataLeft.length / 12.0));
			fadeDataRight[i] = (long) ((fadeDataRight[i] * (wavDataLeft.length - i)) / (wavDataLeft.length / 12.0));
		}

		// add faded wav to overall wav
		for (int i = 0; i < wavDataLeft.length; i++) {
			wavDataLeft[i] += (int) fadeDataLeft[i];
			wavDataRight[i] += (int) fadeDataRight[i];
		}

		System.out.println("We added " + addTimes + " drum sounds!");

		return bpmBasedBeats;
	}

	private List<Integer> smoothenBeats(List<Integer> bpmBasedBeats) {

		Collections.sort(bpmBasedBeats);

		if (bpmBasedBeats.size() > 1) {
			List<Integer> smoothBeats = new ArrayList<>();
			smoothBeats.add(bpmBasedBeats.get(0));
			for (int i = 1; i < bpmBasedBeats.size() - 1; i++) {
				smoothBeats.add((bpmBasedBeats.get(i-1) + bpmBasedBeats.get(i+1)) / 2);
			}
			smoothBeats.add(bpmBasedBeats.get(bpmBasedBeats.size() - 1));
			bpmBasedBeats = smoothBeats;
		}

		return bpmBasedBeats;
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
			wavDataLeft[i+pos] += wavVolume * newLeft[i];
			wavDataRight[i+pos] += wavVolume * newRight[i];
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
			wavDataLeft[i+pos] += wavVolume * newMono[i];
			wavDataRight[i+pos] += wavVolume * newMono[i];
		}
	}

	private void addFadedWavMono(WavFile wav, int samplePos, int wavVolume) {

		wav.normalizeTo16Bits();

		int[] newMono = wav.getMonoData();

		int pos = samplePos;

		int len = newMono.length;
		if (len + pos > fadeDataLeft.length) {
			len = fadeDataLeft.length - pos;
		}

		for (int i = 0; i < len; i++) {
			if (i+pos >= fadeDataLeft.length) {
				return;
			}
			if ((i+pos < 0) || (i < 0)) {
				return;
			}
			fadeDataLeft[i+pos] += wavVolume * newMono[i];
			fadeDataRight[i+pos] += wavVolume * newMono[i];
		}
	}

	private void scale(double scaleFactor) {
		for (int i = 0; i < wavDataLeft.length; i++) {
			wavDataLeft[i] = (int) (scaleFactor * wavDataLeft[i]);
			wavDataRight[i] = (int) (scaleFactor * wavDataRight[i]);
		}
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
			songData[pos + i] += drumData[i];
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
		return millisToFrame(channelPosToMillis(wavDataLeft.length));
	}

	private int millisToFrame(int millis) {
		return (millis * frameRate) / 1000;
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

	private void trimWav() {
		int max = 0;
		int min = 0;
		for (int i = 0; i < wavDataLeft.length; i++) {
			if (wavDataLeft[i] > max) {
				max = wavDataLeft[i];
			}
			if (wavDataLeft[i] < min) {
				min = wavDataLeft[i];
			}
			if (wavDataRight[i] > max) {
				max = wavDataRight[i];
			}
			if (wavDataRight[i] < min) {
				min = wavDataRight[i];
			}
		}
		if (- min > max) {
			max = - min;
		}
		max = max / 100;
		int noiseStart = 0;
		int noiseLength = 0;
		for (int i = 0; i < wavDataLeft.length; i++) {
			int val = wavDataLeft[i];
			if (val < 0) {
				val = - val;
			}
			if (val > max) {
				noiseStart = i;
				break;
			}
			val = wavDataRight[i];
			if (val < 0) {
				val = - val;
			}
			if (val > max) {
				noiseStart = i;
				break;
			}
		}
		for (int i = noiseStart; i < wavDataLeft.length; i++) {
			int val = wavDataLeft[i];
			if (val < 0) {
				val = - val;
			}
			if (val > max) {
				noiseLength = i - noiseStart;
			}
			val = wavDataRight[i];
			if (val < 0) {
				val = - val;
			}
			if (val > max) {
				noiseLength = i - noiseStart;
			}
		}
		int[] newLeft = new int[noiseLength];
		int[] newRight = new int[noiseLength];
		for (int i = noiseStart; i < noiseLength; i++) {
			newLeft[i - noiseStart] = wavDataLeft[i];
			newRight[i - noiseStart] = wavDataRight[i];
		}
		wavDataLeft = newLeft;
		wavDataRight = newRight;
	}
}
