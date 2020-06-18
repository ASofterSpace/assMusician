/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.video.elements.Waveform;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.GraphDataPoint;
import com.asofterspace.toolbox.images.GraphImage;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.music.Beat;
import com.asofterspace.toolbox.music.BeatStats;
import com.asofterspace.toolbox.music.SoundData;
import com.asofterspace.toolbox.music.WavFile;
import com.asofterspace.toolbox.utils.CallbackWithString;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.Pair;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

	private int fourierLen;
	private int[][] fouriers;

	private int graphImageHeight = 256;
	private GraphImage wavGraphImg;

	// width (px), height (px) and frame rate (fps) of the resulting video
	/**/
	public final static int width = 1920;
	public final static int height = 1080;
	public final static int frameRate = 60;
	// we have 3 frames for one Fourier transform
	public final static int framesPerFourier = 3;
	// we interweave 2 Fouriers, meaning that instead of doing one, then the next,
	// we do one, then one from half until half of next, then next, then one from half of next until half of next next
	// TODO :: actually do this!
	public final static int interwovenFouriers = 2;
	/**/
	/*
	public final static int width = 640;
	public final static int height = 360;
	public final static int frameRate = 30;
	/**/
	private final static boolean skipVideo = false;
	private final static boolean reuseExistingVideo = false;
	private final static int useDrumSounds = 1;


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

		List<String> debugOut = new ArrayList<>();

		debugOut.add("{start log}");

		debugOut.add("Song Analysis");
		debugOut.add(": Load Audio");

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
		debugOut.add("  :: input byte rate: " + this.byteRate);
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		debugOut.add("  :: input bytes per sample: " + this.bytesPerSample);
		SoundData wavSoundData = wav.getSoundData();
		debugOut.add("  :: sound data length: " + wavSoundData.getLength() + " pos");

		// cut off silence from the front and back (basically trim() for the wav file... ^^)
		// but add one second of silence in the end!
		debugOut.add(": Start Pre-Processing");
		int addAmountMS = 1000;
		int addAmount = millisToChannelPos(addAmountMS);
		debugOut.add("  :: trim and add: " + addAmountMS + " ms");
		debugOut.add("  :: trim and add: " + addAmount + " pos");
		wavSoundData.trimAndAdd(addAmount);
		// fade in and out for 1 second each
		int fadeInAmountMS = 1000;
		int fadeInAmount = millisToChannelPos(fadeInAmountMS);
		debugOut.add("  :: fade in: " + fadeInAmountMS + " ms");
		debugOut.add("  :: fade in: " + fadeInAmount + " pos");
		wavSoundData.fadeIn(fadeInAmount);
		int fadeOutAmountMS = 1000;
		int fadeOutAmount = millisToChannelPos(fadeOutAmountMS);
		debugOut.add("  :: fade out: " + fadeOutAmountMS + " ms");
		debugOut.add("  :: fade out: " + fadeOutAmount + " pos");
		wavSoundData.fadeOut(fadeOutAmount);

		debugOut.add("  :: sound data length: " + wavSoundData.getLength() + " pos");

		Waveform origWaveform = new Waveform(wavSoundData);


		// Fourier analysis...

		debugOut.add(": Start Fourier Analysis");

		debugOut.add("  :: frame per Fourier: " + framesPerFourier + " pos");
		debugOut.add("  :: frame rate: " + frameRate);
		fourierLen = millisToChannelPos((1000 * framesPerFourier) / frameRate);
		debugOut.add("  :: Fourier length: " + fourierLen + " pos");
		int fourierNum = 0;
		int fourierMax = 0;
		int fourierAmount = wavSoundData.getLength() / fourierLen;
		debugOut.add("  :: Fourier amount: " + fourierAmount);
		fouriers = new int[fourierAmount][];

		while (true) {
			System.out.println("Processed " + fourierNum + " / " + fourierAmount + " Fouriers, max so far: " + fourierMax + "...");

			if ((fourierNum+1)*fourierLen >= wavSoundData.getLength()) {
				break;
			}
			if (fourierNum >= fourierAmount) {
				break;
			}

			int[] fourier = wavSoundData.getSmallFourier(fourierNum*fourierLen, (fourierNum+1)*fourierLen);

			for (int k = 0; k < fourier.length / 2; k++) {
				if (fourier[k] > fourierMax) {
					fourierMax = fourier[k];
				}
			}

			fouriers[fourierNum] = fourier;

			if (fourierNum % (fourierAmount / 16) == 0) {
				debugOut.add("    ::: [" + fourierNum + "] Fourier max: " + fourierMax);
			}

			/*
			// output Fourier as histogram
			List<GraphDataPoint> fourierData = new ArrayList<>();
			fourierData.add(new GraphDataPoint(-1, fourierMax));
			for (int k = 0; k < fourier.length / 2; k++) {
				fourierData.add(new GraphDataPoint(k/5, Math.abs(fourier[k])));
			}
			GraphImage fourierImg = new GraphImage();
			fourierImg.setInnerWidthAndHeight(fourier.length/5, 512);
			fourierImg.setDataColor(new ColorRGB(0, 0, 255));
			fourierImg.setRelativeDataPoints(fourierData);
			DefaultImageFile fourierImgFile = new DefaultImageFile(workDir, "waveform_fourier_" + fourierNum + ".png");
			fourierImgFile.assign(fourierImg);
			fourierImgFile.save();
			*/

			fourierNum++;
		}

		debugOut.add("  :: Fourier max: " + fourierMax);


		// go over to directly working on the wave data...

		wavDataLeft = wavSoundData.getLeftDataCopy();
		wavDataRight = wavSoundData.getRightDataCopy();

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

		TextFile waveformAnalysisFile = new TextFile(workDir, "waveform_analysis.txt");
		waveformAnalysisFile.saveContent(
			"unser Beispielsong ist 3:41 lang, also 221 Sekunden\r\n" +
			"\r\n" +
			"waveform image Breite ist 2236 px, ohne Rahmen 2216 px - also ohne Rahmen ist -20\r\n" +
			"\r\n" +
			"wenn wir jetzt an Position x interessiert sind, ist der pixwert:\r\n" +
			"x_px = (x_sec * (2236 / 221)) + 10\r\n" +
			"(plus 10, damit wir den linken Rahmen mit drin haben)\r\n" +
			"\r\n" +
			"und andersherum:\r\n" +
			"x_sec = (x_px - 10) * (221 / 2236)\r\n" +
			"\r\n" +
			"uuund das lustige ist, diese Formeln sollten immer gelten, auch für andere Songs! :D"
		);

		// add drums
		List<Beat> drumBeats = getDrumBeats(debugOut);

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition.png");
		wavImgFile.assign(wavGraphImg);
		wavImgFile.save();

		addDrumsBasedOnBeats(drumBeats);

		debugOut.add("Song Finalization");
		debugOut.add(": Start Post-Processing");

		// add rev sound at the beginning at high volume
		int revPos = 0;
		int revLoudness = 8;
		addWavMono(WAV_REV_DRUM, revPos, revLoudness);
		debugOut.add("  :: add rev sound at " + revPos + " pos");
		debugOut.add("  :: rev sound loudness: " + revLoudness);

		/* NO NEED TO FADE ANYMORE, AS WE ADJUST DRUM VOLUME BASED ON BEAT VOLUME, WHICH IS EVEN BETTER
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
		*/

		// add faded wav to overall wav
		for (int i = 0; i < wavDataLeft.length; i++) {
			wavDataLeft[i] += (int) fadeDataLeft[i];
			wavDataRight[i] += (int) fadeDataRight[i];
		}

		// handle the overflow by normalizing the entire song
		SoundData soundData = new SoundData(wavDataLeft, wavDataRight);
		debugOut.add("  :: normalizing sound data");
		debugOut.add("  :: max before: " + soundData.getMax());
		soundData.normalize();
		debugOut.add("  :: max after: " + soundData.getMax());

		Waveform newWaveform = new Waveform(soundData);

		// save the new song as audio
		debugOut.add(": Save Audio");
		WavFile newSongFile = new WavFile(workDir, "our_song.wav");
		int outChannelNum = 2;
		debugOut.add("  :: number of channels: " + outChannelNum);
		newSongFile.setNumberOfChannels(outChannelNum);
		debugOut.add("  :: sample rate: " + wav.getSampleRate());
		newSongFile.setSampleRate(wav.getSampleRate());
		debugOut.add("  :: byte rate: " + byteRate);
		newSongFile.setByteRate(byteRate);
		debugOut.add("  :: bytes per sample: " + bytesPerSample);
		debugOut.add("  :: bits per sample: " + (bytesPerSample * 8));
		newSongFile.setBitsPerSample(bytesPerSample * 8);
		debugOut.add("  :: sound data length: " + soundData.getLength());
		newSongFile.setSoundData(soundData);
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
		// TODO :: also add filters for things happening in the video, to make the raw pixels more appealing
		// (e.g. have some elements blink when something happens, or apply general filters to the entire image,
		// have everything shake etc...)

		if (skipVideo) {
			System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] Song creation for " + originalSong.getLocalFilename() + " done, video creation aborted!");
			return;
		}

		String vidpicdirStr = workDir.getAbsoluteDirname();

		if (reuseExistingVideo) {

			vidpicdirStr = inputDir.getAbsoluteDirname() + "\\default_vid_hotter_than_hell_2";

		} else {

			int totalFrameAmount = calcTotalFrameAmount();

			VideoGenerator vidGenny = new VideoGenerator(this, workDir);

			GraphImage wavGraphImg = new GraphImage(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

			wavData = new ArrayList<>();
			position = 0;
			for (Integer wavInt : wavDataLeft) {
				wavData.add(new GraphDataPoint(position, wavInt));
				position++;
			}
			wavGraphImg.setDataColor(new ColorRGB(0, 0, 255));
			wavGraphImg.setAbsoluteDataPoints(wavData);

			String songTitle = originalSong.getLocalFilename();
			if (songTitle.contains(".")) {
				songTitle = songTitle.substring(0, songTitle.lastIndexOf("."));
			}
			debugOut.add("  :: input song title: " + songTitle);
			songTitle += " (Remix with Drums)";
			debugOut.add("  :: output song title: " + songTitle);

			debugOut.add("{end log}");

			vidGenny.generateVideoBasedOnBeats(drumBeats, totalFrameAmount, width, height, wavGraphImg,
				origWaveform, newWaveform, songTitle, framesPerFourier, fouriers, debugOut);
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

		SimpleFile debugLogFile = new SimpleFile(workDir, "debug.txt");
		debugLogFile.saveContents(debugOut);

		System.out.println("[" + DateUtils.serializeDateTime(new Date()) + "] " + originalSong.getLocalFilename() + " done!");
	}

	// TODO: idea for a better algorithm:
	// * get bpm not overall for entire song, but for song areas, where we define an area to be a duration
	//   over which the loudness does not increase or decrease by more than 10? then generate bpm and align...
	//   but do the overall smoothen after it is done?
	// * maybe give more importance during bpm detection to beats that have higher jitterieness?
	//   (they tend to be where the fun is...)
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
	private List<Beat> getDrumBeats(List<String> debugOut) {

		debugOut.add("Starting Drum Beat Detection");

		/*
		// ALGORITHM 1

		// actually put them into the song
		addDrum(wavDataLeft, 3000);
		addDrum(wavDataRight, 3000);
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
			}
		}
		*/

		// ALGORITHM 3

		debugOut.add(": Starting Algorithm 3");

		// We first iterate over the entire song and try to find maxima, by finding
		// first the maxima over each 0.025 s region, and then we go over all the maxima
		// from end to start and keep only the ones which are preceded by lower ones
		// For this (for now) we only use the left channel...
		List<Integer> maximumPositions = new ArrayList<>();
		List<Integer> potentialMaximumPositions = new ArrayList<>();
		int localMaxPos = 0;
		int localMax = 0;
		int regionSizeMS = 25;
		int regionSize = millisToChannelPos(regionSizeMS);
		debugOut.add("  :: region size: " + regionSizeMS + " ms");
		debugOut.add("  :: region size: " + regionSize + " pos");

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

		debugOut.add("  :: " + potentialMaximumPositions.size() + " potential maximum positions found");

		/*
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
		*/
		Map<Integer, Integer> moreLikelyMaximumPositions = new HashMap<>();
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
				moreLikelyMaximumPositions.put(potentialMaximumPositions.get(i),
					(3*wavDataLeft[potentialMaximumPositions.get(i)])-(wavDataLeft[potentialMaximumPositions.get(i-1)]+
					wavDataLeft[potentialMaximumPositions.get(i-2)]+wavDataLeft[potentialMaximumPositions.get(i-3)]));
			}
		}

		debugOut.add("  :: " + moreLikelyMaximumPositions.size() + " more likely maximum positions found");

		// we now iterate once more, getting the highest / most maximum-y of the maxima
		for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos(100)) {
			int highestPos = -1;
			int highestAmount = -1;
			for (int k = i; k < i + millisToChannelPos(100); k++) {
				Integer val = moreLikelyMaximumPositions.get(k);
				if (val != null) {
					if (val > highestAmount) {
						highestAmount = val;
						highestPos = k;
					}
				}
			}
			if (highestPos >= 0) {
				maximumPositions.add(highestPos);
			}
		}

		debugOut.add("  :: " + maximumPositions.size() + " most maximum-y maxima found");

		Collections.sort(maximumPositions);

/*
		// now iterate over all the found maximum positions, and whenever the distance between some is small-ish
		// (let's say 0.1s), we mush them together into one
		int smooshSize = millisToChannelPos(100);
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
*/

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

		debugOut.add(": Starting Algorithm 3.6");

		Collections.sort(maximumPositions);

		// each bpm candidate is an int representing a bpm value times 10 (so that we have a bit more accuracy),
		// mapping to an int which represents how many values we have put into this bucket
		int BUCKET_ACCURACY_FACTOR = 1;
		int MIN_BPM = 90;
		int MAX_BPM = 180;
		int LOOKBACK = 1;

		debugOut.add("  :: bucket accuracy factor: " + BUCKET_ACCURACY_FACTOR);
		debugOut.add("  :: min bpm: " + MIN_BPM);
		debugOut.add("  :: max bpm: " + MAX_BPM);
		debugOut.add("  :: lookback: " + LOOKBACK);

		Map<Integer, Integer> bpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			bpmCandidates.put(curBpm, 0);
		}

		int candFound = 0;
		int candDistinct = 0;

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
				int curDiffInMs = channelPosToMillis(10*curDist);

				// a difference of 1 ms means that there are 60*1000 beats per minute,
				// and we have the additional *10 to get more accuracy
				if (curDiffInMs == 0) {
					continue;
				}
				int curBpm = (60*1000*BUCKET_ACCURACY_FACTOR*10) / curDiffInMs;

				// scale the bpm into the range that we are interested in
				while (curBpm < MIN_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm *= 2;
				}
				while (curBpm > MAX_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm /= 2;
				}

				if (bpmCandidates.get(curBpm) != null) {
					if (bpmCandidates.get(curBpm) == 0) {
						candDistinct++;
					}
					bpmCandidates.put(curBpm, bpmCandidates.get(curBpm) + 1);
				} else {
					bpmCandidates.put(curBpm, 1);
					candDistinct++;
				}
				candFound++;
			}
		}

		debugOut.add("  :: " + candFound + " bpm candidates found overall");
		debugOut.add("  :: " + candDistinct + " distinct bpm candidates found");

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

		// now find the largest bucket
		int largestBucketContentAmount = 0;
		int largestBucketBpm = 0;
		int bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		double bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		int generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm

		debugOut.add("  :: smoothening the buckets");
		int SMOOTHENING_WIDTH = 1500;
		debugOut.add("  :: smoothening width: " + SMOOTHENING_WIDTH);

		Map<Integer, Integer> smoothBpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			int curAmount = 0;
			for (int i = 1; i < SMOOTHENING_WIDTH; i++) {
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
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		System.out.println("We detected " + bpm + " beats per minute, " +
			"with the largest bucket containing " + largestBucketContentAmount + " values...");


		// ALGORITHM 4

		debugOut.add(": Starting Algorithm 4");

		// Instead of keeping the maximumPositions which we had so far, we use new ones which we
		// base on the Fourier transform and the lowest frequencies we get from it...
		// to do so, we split the entire song into pieces the size of two beats, and take the highest
		// Fourier of the lowest frequency as beat to align within that two-beat window!
		// (However, we only take the louder half of these to not get too much noise)

		maximumPositions = new ArrayList<>();

		/*
		for (int i = 0; i < fouriers.length; i++) {
			if (fouriers[i][fouriers[i].length - 1] > 4*3*5000) {
				maximumPositions.add(i * fourierLen);
			}
		}
		*/

		debugOut.add("  :: using " + fouriers.length + " Fourier levels of size " + fouriers[0].length);

		List<Pair<Integer, Integer>> possibleMaximumPositions = new ArrayList<>();
		List<Integer> allMaximumPositionsForAlignment = new ArrayList<>();
		int lastStart = 0;
		debugOut.add("  :: using generated beat distance " + generatedBeatDistance + " pos");
		debugOut.add("  :: using Fourier length " + fourierLen + " pos");
		debugOut.add("  :: resulting resolution: " + (generatedBeatDistance / fourierLen) + " (higher is better)");
		for (int i = generatedBeatDistance; i < wavDataLeft.length; i += generatedBeatDistance) {
			int maxVal = 0;
			int maxPos = 0;
			for (int j = lastStart; j < i; j += fourierLen) {
				int f = (j / fourierLen);
				// go for fouriers[f].length - 1 as second index such that we get the one with the lowest frequency
				if (fouriers[f][fouriers[f].length - 1] > maxVal) {
					maxPos = j;
					maxVal = fouriers[f][fouriers[f].length - 1];
				}
			}
			possibleMaximumPositions.add(new Pair<Integer, Integer>(maxPos, maxVal));
			allMaximumPositionsForAlignment.add(maxPos);
			lastStart = i;
		}

		debugOut.add("  :: found " + possibleMaximumPositions.size() + " possible maximum positions");

		Collections.sort(possibleMaximumPositions, new Comparator<Pair<Integer, Integer>>() {
			public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
				return a.getRight() - b.getRight();
			}
		});

		int midVal = possibleMaximumPositions.get(possibleMaximumPositions.size() / 2).getRight();

		debugOut.add("  :: mid val: " + midVal);

		// only get the louder half of them for now (for bpm detection)
		for (int i = 0; i < possibleMaximumPositions.size(); i++) {
			if (possibleMaximumPositions.get(i).getRight() >= midVal) {
				maximumPositions.add(possibleMaximumPositions.get(i).getLeft());
			}
		}

		debugOut.add("  :: louder half of maximum positions containing " + maximumPositions.size() + " values");

		Collections.sort(maximumPositions);

		// output new file containing these new positions

		GraphImage graphWithFourierImg = new GraphImage();
		graphWithFourierImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

		List<GraphDataPoint> wavData = new ArrayList<>();
		int position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		graphWithFourierImg.setDataColor(new ColorRGB(0, 0, 255));
		graphWithFourierImg.setAbsoluteDataPoints(wavData);

		for (Integer pos : maximumPositions) {
			graphWithFourierImg.drawVerticalLineAt(pos, new ColorRGB(255, 0, 128));
		}

		DefaultImageFile wavFourierImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_beats.png");
		wavFourierImgFile.assign(graphWithFourierImg);
		wavFourierImgFile.save();


		// ALGORITHM 4.1

		debugOut.add(": Starting Algorithm 4.1");

		// now do beat detection AGAIN - this time, based on the fourier-based maxima
		// TODO :: algo 4.1 has been replaced by algo 4.2, stop calculating it!

		bpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			bpmCandidates.put(curBpm, 0);
		}

		candFound = 0;
		candDistinct = 0;

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
				int curDiffInMs = channelPosToMillis(10*curDist);

				// a difference of 1 ms means that there are 60*1000 beats per minute,
				// and we have the additional *10 to get more accuracy
				if (curDiffInMs == 0) {
					continue;
				}
				int curBpm = (60*1000*BUCKET_ACCURACY_FACTOR*10) / curDiffInMs;

				// scale the bpm into the range that we are interested in
				while (curBpm < MIN_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm *= 2;
				}
				while (curBpm > MAX_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm /= 2;
				}

				if (bpmCandidates.get(curBpm) != null) {
					if (bpmCandidates.get(curBpm) == 0) {
						candDistinct++;
					}
					bpmCandidates.put(curBpm, bpmCandidates.get(curBpm) + 1);
				} else {
					bpmCandidates.put(curBpm, 1);
					candDistinct++;
				}
				candFound++;
			}
		}

		debugOut.add("  :: " + candFound + " bpm candidates found overall");
		debugOut.add("  :: " + candDistinct + " distinct bpm candidates found");

		// output buckets as histogram
		graphWidth = (MAX_BPM - MIN_BPM) * BUCKET_ACCURACY_FACTOR * 10;
		histogramImg = new GraphImage();
		histogramImg.setInnerWidthAndHeight(graphWidth, graphImageHeight);

		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_bpm.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm

		debugOut.add("  :: smoothening the buckets");
		SMOOTHENING_WIDTH = 1500;
		debugOut.add("  :: smoothening width: " + SMOOTHENING_WIDTH);

		smoothBpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			int curAmount = 0;
			for (int i = 1; i < SMOOTHENING_WIDTH; i++) {
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
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_bpm_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		System.out.println("We detected " + bpm + " beats per minute based on the Fourier beats, " +
			"with the largest bucket containing " + largestBucketContentAmount + " values...");


		// ALGORITHM 4.2

		debugOut.add(": Starting Algorithm 4.2");

		// now do beat detection AGAIN - this time, based on the fourier-based maxima, and with a different
		// algorithm - instead of putting bpms into buckets, we want to look at the histogram of all the
		// one-beat distances and see if we can find some truth in that...

		int maxDist = 0;
		int minDist = Integer.MAX_VALUE;
		List<Integer> distances = new ArrayList<>();
		System.out.println("\n\nDistances unsorted:\n");
		for (int i = 1; i < maximumPositions.size(); i++) {
			int curDist = maximumPositions.get(i) - maximumPositions.get(i - 1);
			if (curDist > maxDist) {
				maxDist = curDist;
			}
			if (curDist < minDist) {
				minDist = curDist;
			}
			distances.add(curDist);
			System.out.println("  "+ curDist + " from " + maximumPositions.get(i) + " to " + maximumPositions.get(i-1));
		}

		debugOut.add("  :: " + distances.size() + " distances calculated");
		debugOut.add("  :: max dist: " + maxDist);
		debugOut.add("  :: min dist: " + minDist);
		System.out.println("\n\n");

		Collections.sort(distances);
		System.out.println("\n\nDistances sorted:\n");
		for (Integer i : distances) {
			System.out.println("  "+i);
		}
		System.out.println("\n\n");

		Map<Integer, Integer> beatLenCandidates = new HashMap<>();
		for (int i = 1; i <= maxDist; i++) {
			beatLenCandidates.put(i, 0);
		}

		int beatLenAmount = 0;
		int beatLenDistinct = 0;
		for (int i = 1; i < maximumPositions.size(); i++) {
			int curDist = maximumPositions.get(i) - maximumPositions.get(i - 1);
			if (beatLenCandidates.get(curDist) != null) {
				beatLenCandidates.put(curDist, beatLenCandidates.get(curDist) + 1);
			} else {
				beatLenCandidates.put(curDist, 1);
				beatLenDistinct++;
			}
			beatLenAmount++;
		}
		debugOut.add("  :: calculated " + beatLenDistinct + " distinct best length candidates");
		debugOut.add("  :: calculated " + beatLenAmount + " best length candidates overall");

		// output buckets as histogram
		graphWidth = maxDist;
		histogramImg = new GraphImage();
		histogramImg.setInnerWidthAndHeight(graphWidth / 20, graphImageHeight);

		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		debugOut.add("  :: obtained " + beatLenCandidates.entrySet().size() + " buckets");
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_len_beats.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm
		Map<Integer, Integer> smoothBeatLenCandidates = new HashMap<>();
		for (int curLen = 1; curLen <= maxDist; curLen++) {
			smoothBeatLenCandidates.put(curLen, 0);
		}
		int smoothenBy = 4096;
		debugOut.add("  :: smoothening the buckets by " + smoothenBy);
		for (int curLen = 1; curLen <= maxDist; curLen++) {
			if (beatLenCandidates.get(curLen) != null) {
				if (beatLenCandidates.get(curLen) > 0) {
					for (int i = 0; i < smoothenBy; i++) {
						if (smoothBeatLenCandidates.get(curLen+i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen+i);
							cur += (smoothenBy - i) * beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen+i, cur);
						}
						if (i == 0) {
							continue;
						}
						if (smoothBeatLenCandidates.get(curLen-i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen-i);
							cur += (smoothenBy - i) * beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen-i, cur);
						}
						/*
						if (smoothBeatLenCandidates.get(curLen+i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen+i);
							cur += beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen+i, cur + smoothenBy - i);
						}
						if (smoothBeatLenCandidates.get(curLen-i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen-i);
							cur += beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen-i, cur + smoothenBy - i);
						}
						*/
					}
				}
			}
/*
			int curAmount = 0;
			for (int i = 1; i < 256; i++) {
				if (beatLenCandidates.get(curLen-i) != null) {
					curAmount += beatLenCandidates.get(curLen-i);
				}
				if (beatLenCandidates.get(curLen+i) != null) {
					curAmount += beatLenCandidates.get(curLen+i);
				}
			}
			for (int i = 1; i < 128; i++) {
				if (beatLenCandidates.get(curLen-i) != null) {
					curAmount += beatLenCandidates.get(curLen-i);
				}
				if (beatLenCandidates.get(curLen+i) != null) {
					curAmount += beatLenCandidates.get(curLen+i);
				}
			}
			for (int i = 1; i < 64; i++) {
				if (beatLenCandidates.get(curLen-i) != null) {
					curAmount += beatLenCandidates.get(curLen-i);
				}
				if (beatLenCandidates.get(curLen+i) != null) {
					curAmount += beatLenCandidates.get(curLen+i);
				}
			}
			if (beatLenCandidates.get(curLen) != null) {
				curAmount += beatLenCandidates.get(curLen) * 4;
			}
			smoothBeatLenCandidates.put(curLen, curAmount);
*/
		}
		beatLenCandidates = smoothBeatLenCandidates;

		// output smoothened buckets as histogram
		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_len_beats_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		int largestBucketBeatLen = 0;

		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBeatLen = entry.getKey();
			}
		}

		debugOut.add("  :: " + largestBucketContentAmount + " values in the largest bucket");
		debugOut.add("  :: beat length based on largest bucket: " + largestBucketBeatLen + " pos");

		int largestBucketBeatLenInMS = channelPosToMillis(largestBucketBeatLen);
		debugOut.add("  :: beat length based on largest bucket: " + largestBucketBeatLenInMS + " ms");

		if (largestBucketBeatLenInMS != 0) {
			bpm = 1000 * 60.0 / largestBucketBeatLenInMS;
			debugOut.add("  :: bpm: " + bpm);

			System.out.println("We detected " + bpm + " beats per minute based on the Fourier beats length histogram, " +
				"with the largest bucket containing " + largestBucketContentAmount + " values...");
		} else {
			System.out.println("Falling back on " + bpm + " beats per minute due to a division by zero!");
			debugOut.add("  :: division by zero - fallback bpm: " + bpm);
		}

		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");


		// generate beats based on the detected bpm, but still try to locally align to the closest
		// detected beat, e.g. align to the next one to the right if there is one to the left or
		// right of the current beat and the distance to it is less than 1/10 of a beat...
		List<Beat> mayBeats = new ArrayList<>();
		/*for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos((long) ((1000*60) / bpm))) {
			bpmBasedBeats.add(i);
			wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
		}*/

		int maxPosI = 0;
		// such that we do not have to worry about overflowing the maximumPositions list,
		// we just add an extra beat far after everything else
		maximumPositions = allMaximumPositionsForAlignment;
		Collections.sort(maximumPositions);
		maximumPositions.add(wavDataLeft.length * 10);


		// ALGORITHM 5

		debugOut.add(": Starting Algorithm 5");

		// in addition to all else, here keep a rolling average of the last 10 beat distances that we actually
		// put into the song, and use as predictor for the next beat distances the average of that and the
		// generated distance rather than just the generated distance purely
		int ACTUAL_GEN_RING_SIZE = 10;
		debugOut.add("  :: generator ring size: " + ACTUAL_GEN_RING_SIZE);

		int[] actualGeneratedDistances = new int[ACTUAL_GEN_RING_SIZE];
		int actualGeneratedI = 0;
		for (int i = 0; i < ACTUAL_GEN_RING_SIZE; i++) {
			actualGeneratedDistances[i] = generatedBeatDistance;
		}

		int prevI = -generatedBeatDistance;
		int origGeneratedBeatDistance = generatedBeatDistance;

		System.out.println("");

		int uncertaintyFrontSetting = 4;
		int uncertaintyBackSetting = 5;
		int uncertaintyBackInsertBeatSetting = 10;
		debugOut.add("  :: uncertainty front setting: " + uncertaintyFrontSetting + " / 10");
		debugOut.add("  :: uncertainty back setting: " + uncertaintyBackSetting + " / 10");
		debugOut.add("  :: uncertainty back insert beat setting: " + uncertaintyBackInsertBeatSetting + " / 10");

		debugOut.add("  :: orig generated beat distance: " + origGeneratedBeatDistance + " pos");

		int insertedMidAlignedBeats = 0;
		int insertedAlignedBeats = 0;
		int insertedUnalignedBeats = 0;

		for (int i = 0; i < wavDataLeft.length; i += generatedBeatDistance) {

			// regular alignment: 20% to the front, 20% to the back will be aligned, 60% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 5;
			// semi-aggressive alignment: 40% to the front, 50% to the back will be aligned, 50% to 100% to the back we align but add a beat
			int uncertaintyFront = (generatedBeatDistance * uncertaintyFrontSetting) / 10;
			int uncertaintyBack = (generatedBeatDistance * uncertaintyBackSetting) / 10;
			int uncertaintyBackInsertBeat = (generatedBeatDistance * uncertaintyBackInsertBeatSetting) / 10;
			// aggressive alignment: 50% to the front, 50% to the back will be aligned, 0% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 2;

			while (maximumPositions.get(maxPosI) < i - uncertaintyFront) {
				maxPosI++;
			}
			Beat mayBeat = new Beat(i);
			if (maximumPositions.get(maxPosI) < i + uncertaintyBackInsertBeat) {
				while (maximumPositions.get(maxPosI) < i) {
					maxPosI++;
				}
				int newI = i;
				if (maxPosI < 1) {
					newI = maximumPositions.get(maxPosI);
				} else {
					if (maximumPositions.get(maxPosI) - i < i - maximumPositions.get(maxPosI-1)) {
						newI = maximumPositions.get(maxPosI);
					} else {
						newI = maximumPositions.get(maxPosI-1);
					}
				}
				// if we are over the regular uncertainty towards the back, add an extra beat halfway in between
				if (newI > i + uncertaintyBack) {
					int extraI = (newI + (i - generatedBeatDistance)) / 2;
					Beat extraBeat = new Beat(extraI);
					extraBeat.setIsAligned(false);
					mayBeats.add(extraBeat);
					wavGraphImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
					graphWithFourierImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));

					actualGeneratedDistances[actualGeneratedI] = extraI - prevI;
					prevI = extraI;
					actualGeneratedI++;
					if (actualGeneratedI >= ACTUAL_GEN_RING_SIZE) {
						actualGeneratedI = 0;
					}
					insertedMidAlignedBeats++;
				}
				i = newI;
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				mayBeat.setPosition(i);
				mayBeat.setIsAligned(true);
				insertedAlignedBeats++;
			} else {
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				mayBeat.setIsAligned(false);
				insertedUnalignedBeats++;
			}
			mayBeats.add(mayBeat);

			actualGeneratedDistances[actualGeneratedI] = i - prevI;
			actualGeneratedI++;
			if (actualGeneratedI >= ACTUAL_GEN_RING_SIZE) {
				actualGeneratedI = 0;
			}
			generatedBeatDistance = 0;
			for (int g = 0; g < ACTUAL_GEN_RING_SIZE; g++) {
				generatedBeatDistance += actualGeneratedDistances[g];
			}
			generatedBeatDistance = generatedBeatDistance / ACTUAL_GEN_RING_SIZE;
			generatedBeatDistance = (origGeneratedBeatDistance + generatedBeatDistance) / 2;
			System.out.println("generatedBeatDistance: " + generatedBeatDistance + " orig: " + origGeneratedBeatDistance);
			if (i % (wavDataLeft.length / 16) == 0) {
				debugOut.add("    ::: [" + i + "] generated beat distance: " + generatedBeatDistance + " pos");
			}
			prevI = i;
		}
		System.out.println("");

		debugOut.add("  :: generated " + insertedAlignedBeats + " aligned beats");
		debugOut.add("  :: generated " + insertedMidAlignedBeats + " mid aligned beats");
		debugOut.add("  :: generated " + insertedUnalignedBeats + " unaligned beats");

		/*
		for (int i = 0; i < wavDataLeft.length; i += generatedBeatDistance) {

			// regular alignment: 20% to the front, 20% to the back will be aligned, 60% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 5;
			// semi-aggressive alignment: 40% to the front, 50% to the back will be aligned, 50% to 100% to the back we align but add a beat
			int uncertaintyFront = (generatedBeatDistance * 4) / 10;
			int uncertaintyBack = (generatedBeatDistance * 5) / 10;
			int uncertaintyBackInsertBeat = (generatedBeatDistance * 10) / 10;
			// aggressive alignment: 50% to the front, 50% to the back will be aligned, 0% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 2;

			while (maximumPositions.get(maxPosI) < i - uncertaintyFront) {
				maxPosI++;
			}
			Beat mayBeat = new Beat(i);
			if (maximumPositions.get(maxPosI) < i + uncertaintyBackInsertBeat) {
				while (maximumPositions.get(maxPosI) < i) {
					maxPosI++;
				}
				int newI = i;
				if (maxPosI < 1) {
					newI = maximumPositions.get(maxPosI);
				} else {
					if (maximumPositions.get(maxPosI) - i < i - maximumPositions.get(maxPosI-1)) {
						newI = maximumPositions.get(maxPosI);
					} else {
						newI = maximumPositions.get(maxPosI-1);
					}
				}
				// if we are over the regular uncertainty towards the back, add an extra beat halfway in between
				if (newI > i + uncertaintyBack) {
					int extraI = (newI + (i - generatedBeatDistance)) / 2;
					Beat extraBeat = new Beat(extraI);
					extraBeat.setIsAligned(false);
					mayBeats.add(extraBeat);
					wavGraphImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
					graphWithFourierImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
				}
				i = newI;
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				mayBeat.setPosition(i);
				mayBeat.setIsAligned(true);
			} else {
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				mayBeat.setIsAligned(false);
			}
			mayBeats.add(mayBeat);
		}
		*/

		DefaultImageFile wavImgFileFourier = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition_fourier_before_smoothen.png");
		wavImgFileFourier.assign(graphWithFourierImg);
		wavImgFileFourier.save();

		// ALGORITHM 3.8

		debugOut.add(": Starting Algorithm 3.8");

		// Aaaaand - you thought we were done, huh? :D - we continue... now we are looking at this:
		//  detected beats: |  |  |
		// generated beats: | | | |
		// so the first and last are aligned, but in the middle it would actually fit quite nicely...
		// if we had one less being generated, and they were put to equi-distance - so let's!

		List<Integer> bpmBasedBeats = new ArrayList<>();

		for (int i = 0; i < mayBeats.size(); i++) {
			Beat beat = mayBeats.get(i);

			bpmBasedBeats.add(beat.getPosition());

			/*
			// fun idea, but actually it ends up sounding nicer without this algo ._.'

			if (beat.getIsAligned()) {
				// we accept 2 until 5 beats in between
				for (int k = 2; k < 6; k++) {
					boolean foundSituation = true;
					for (int m = i + 1; m <= i + k; m++) {
						if (m >= mayBeats.size()) {
							foundSituation = false;
							break;
						}
						if (mayBeats.get(m).getIsAligned()) {
							foundSituation = false;
							break;
						}
					}
					// we found such a situation for k beats in between - that is, we have
					// k mayBeats generated in between which are all unaligned...
					if (foundSituation) {
						if (i + k + 1 < mayBeats.size()) {
							if (mayBeats.get(i + k + 1).getIsAligned()) {
								// ... now let's see if we have exactly k-1 or k+1 detected
								// beats in between these!
								int detectedBeatsFound = 0;
								for (int maxPosIter = 0; maxPosIter < maximumPositions.size(); maxPosIter++) {
									if ((maximumPositions.get(maxPosIter) > beat.getPosition()) &&
										(maximumPositions.get(maxPosIter) < mayBeats.get(i+k+1).getPosition())) {
										// TODO :: in addition to all that, also check if the beats detected here
										// are somewhat nicely aligned already - not just |   ||   |, but more
										// like |  |  |  |
										detectedBeatsFound++;
									}
								}
								int startPos = beat.getPosition();
								int endPos = mayBeats.get(i+k+1).getPosition();
								// here we generated one more than we detected - so let's remove one!
								if ((detectedBeatsFound == k - 1) ||
									//  we generated one less than we detected - so let's add one!
									(detectedBeatsFound == k + 1)) {
									System.out.println("At startPos " + startPos + " (" + channelPosToMillis(startPos) + " ms)" +
										" and endPos " + endPos + " (" + channelPosToMillis(endPos) + " ms) we detected " +
										detectedBeatsFound + " beats, but generated " + k + " so let's do something about that!");
									for (int subGenI = 0; subGenI < detectedBeatsFound; subGenI++) {
										bpmBasedBeats.add(startPos + (((subGenI+1)*(endPos - startPos))/(detectedBeatsFound+1)));
									}
									i += k;
									break;
								}
							}
						}
					}
				}
			}
			*/
		}

		debugOut.add("  :: " + bpmBasedBeats.size() + " beats generated");

		Collections.sort(bpmBasedBeats);

		// ALGORITHM 3.7

		debugOut.add(": Starting Algorithm 3.7");

		// Now, after all that is done... the beats are detected, the bpm decided, the beats generated
		// and subsequently aligned against the ones previously detected... we have a problem: as we
		// just aligned the beats, some distances between beats are (noticeably!) different - e.g.
		// if we had beats at 1, 3, 5, 7, and then aligned it and it is now 1, 3, 4.8, 6.8, then the
		// distances from 3 to 4.8 being suddenly different will sound weird... so let's smoothen it!
		// that is, for each we get the distance to front and back, and align to the middle of that:
		// 1, 2.9, 4.9, 6.8... and again! ... and then we are good! :D

		int SMOOTH_AMOUNT = 3;
		debugOut.add("  :: smoothening " + SMOOTH_AMOUNT + " times");
		for (int i = 0; i < SMOOTH_AMOUNT; i++) {
			bpmBasedBeats = smoothenBeats(bpmBasedBeats);
		}

		Collections.sort(bpmBasedBeats);


		graphWithFourierImg = new GraphImage();
		graphWithFourierImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);
		graphWithFourierImg.setDataColor(new ColorRGB(0, 0, 255));
		graphWithFourierImg.setAbsoluteDataPoints(wavData);

		for (Integer pos : maximumPositions) {
			graphWithFourierImg.drawVerticalLineAt(pos, new ColorRGB(255, 0, 128));
		}

		for (Integer i : bpmBasedBeats) {
			graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
		}
		wavImgFileFourier = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition_fourier_post_smoothen.png");
		wavImgFileFourier.assign(graphWithFourierImg);
		wavImgFileFourier.save();


		List<Beat> beats = new ArrayList<>();

		int curBeat = 0;
		int curBeatLength = 0;
		long curBeatLoudness = 0;
		long curBeatJigglieness = 0;
		long jiggleModifier = 16*16*16;
		debugOut.add("  :: jiggle modifier: " + jiggleModifier);
		Beat prevBeat = null;

		// add one far beyond the end so that we do not have to check for there being one all the time
		bpmBasedBeats.add(1 + (wavDataLeft.length * 2));

		for (int i = 0; i < wavDataLeft.length; i++) {
			int absVal = Math.abs(wavDataLeft[i]) + Math.abs(wavDataRight[i]);
			int prevAbsVal = 0;
			if (i > 0) {
				prevAbsVal = Math.abs(wavDataLeft[i-1]) + Math.abs(wavDataRight[i-1]);
			}
			int nextAbsVal = 0;
			if (i < wavDataLeft.length - 1) {
				nextAbsVal = Math.abs(wavDataLeft[i+1]) + Math.abs(wavDataRight[i+1]);
			}
			if (i < bpmBasedBeats.get(curBeat)) {
				curBeatLoudness += absVal;
				if ((prevAbsVal < absVal) && (nextAbsVal < absVal)) {
					curBeatJigglieness++;
				}
				if ((prevAbsVal > absVal) && (nextAbsVal > absVal)) {
					curBeatJigglieness++;
				}
			} else {
				Beat beat = new Beat(i);
				if (prevBeat != null) {
					int len = i - prevBeat.getPosition();
					prevBeat.setLoudness(curBeatLoudness / len);
					prevBeat.setJigglieness((jiggleModifier * curBeatJigglieness) / len);
					prevBeat.setLength(len);
				}
				beats.add(beat);
				prevBeat = beat;
				curBeatLoudness = 0;
				curBeatJigglieness = 0;
				curBeat++;
			}
		}
		if (prevBeat != null) {
			int len = wavDataLeft.length - prevBeat.getPosition();
			prevBeat.setLoudness(curBeatLoudness / len);
			prevBeat.setJigglieness((jiggleModifier * curBeatJigglieness) / len);
			prevBeat.setLength(len);
		}

		debugOut.add("  :: " + beats.size() + " beats detected");
		System.out.println("We detected " + beats.size() + " beats!");

		return beats;
	}

	private List<Integer> smoothenBeats(List<Integer> bpmBasedBeats) {

		Collections.sort(bpmBasedBeats);

		if (bpmBasedBeats.size() > 1) {
			List<Integer> smoothBeats = new ArrayList<>();
			smoothBeats.add(bpmBasedBeats.get(0));
			smoothBeats.add(bpmBasedBeats.get(1));
			for (int i = 2; i < bpmBasedBeats.size() - 2; i++) {
				smoothBeats.add((bpmBasedBeats.get(i-2) + 2*bpmBasedBeats.get(i-1) + bpmBasedBeats.get(i) + 2*bpmBasedBeats.get(i+1) + bpmBasedBeats.get(i+2)) / 7);
			}
			smoothBeats.add(bpmBasedBeats.get(bpmBasedBeats.size() - 2));
			smoothBeats.add(bpmBasedBeats.get(bpmBasedBeats.size() - 1));
			bpmBasedBeats = smoothBeats;
		}

		return bpmBasedBeats;
	}

	private long divOr255(long dividend, long divisor) {
		if (divisor == 0) {
			return 255;
		}
		return dividend / divisor;
	}

	private void addDrumsBasedOnBeats(List<Beat> beats) {

		GraphImage graphImg = new GraphImage();
		graphImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

		List<GraphDataPoint> wavData = new ArrayList<>();
		int position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		graphImg.setDataColor(new ColorRGB(0, 0, 255));
		graphImg.setAbsoluteDataPoints(wavData);

		int instrumentRing = 0;

		BeatStats stats = new BeatStats(beats);

		System.out.println("");
		System.out.println("averageLength: " + stats.getAverageLength());
		System.out.println("averageLoudness: " + stats.getAverageLoudness());
		System.out.println("averageJigglieness: " + stats.getAverageJigglieness());
		System.out.println("maxLength: " + stats.getMaxLength());
		System.out.println("maxLoudness: " + stats.getMaxLoudness());
		System.out.println("maxJigglieness: " + stats.getMaxJigglieness());

		for (int b = 0; b < beats.size(); b++) {
			Beat beat = beats.get(b);

			int curBeat = beat.getPosition();
			int curBeatLen = beat.getLength();

			graphImg.drawVerticalLineAt(beat.getPosition(), new ColorRGB(
				divOr255(255 * beat.getLoudness(), stats.getMaxLoudness()),
				divOr255(255 * beat.getLength(), stats.getMaxLength()),
				divOr255(255 * beat.getJigglieness(), stats.getMaxJigglieness())
			));


			double baseLoudness = (7.5 * beat.getLoudness()) / 25797;

			long nextNextDrumPatternIndicator = getDrumPatternIndicatorFor(beats, b + 2, stats);
			long nextDrumPatternIndicator = getDrumPatternIndicatorFor(beats, b + 1, stats);
			long drumPatternIndicator = getDrumPatternIndicatorFor(beats, b, stats);
			long prevDrumPatternIndicator = getDrumPatternIndicatorFor(beats, b - 1, stats);
			long prevPrevDrumPatternIndicator = getDrumPatternIndicatorFor(beats, b - 2, stats);

			// prevent weirdly missing drums in the middle of lots-of-drum parts

			// prevent one missing
			if ((prevDrumPatternIndicator > drumPatternIndicator) && (nextDrumPatternIndicator > drumPatternIndicator)) {
				drumPatternIndicator = Math.min(prevDrumPatternIndicator, nextDrumPatternIndicator);
			}

			// prevent two missing
			if ((prevDrumPatternIndicator > drumPatternIndicator) && (nextNextDrumPatternIndicator > drumPatternIndicator)) {
				drumPatternIndicator = Math.min(prevDrumPatternIndicator, nextNextDrumPatternIndicator);
			}
			if ((prevPrevDrumPatternIndicator > drumPatternIndicator) && (nextDrumPatternIndicator > drumPatternIndicator)) {
				drumPatternIndicator = Math.min(prevPrevDrumPatternIndicator, nextDrumPatternIndicator);
			}

			switch (useDrumSounds) {
				case 1:
					// we have encountered the following jigglienesses in the wild:
					//  26 .. singing with nearly no instruments
					// 169 .. loud singing with some instruments
					// 201 .. full blast! :D
					if (drumPatternIndicator > 196) {
						addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
						addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
						addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
						addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
						addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
						addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((5 * curBeatLen) / 8), baseLoudness);
						addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((6 * curBeatLen) / 8), baseLoudness);
						beat.setChanged(true);
					} else {
						if (drumPatternIndicator > 128) {
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 1.25*baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
							addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
							beat.setChanged(true);
						} else {
							if (drumPatternIndicator > 96) {
								addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
								addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
								beat.setChanged(true);
							}
						}
					}
					break;
				case 2:
					beat.setChanged(true);
					switch (instrumentRing) {
						case 0:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
							addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
							break;
						case 1:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
							addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
							break;
						case 2:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), 2*baseLoudness);
							break;
						case 3:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 4*baseLoudness);
							break;
						case 4:
							break;
						case 5:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), 2*baseLoudness);
							instrumentRing = -1;
							break;
					}
					break;
			}

			instrumentRing++;
		}

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_stats.png");
		wavImgFile.assign(graphImg);
		wavImgFile.save();
	}

	private long getDrumPatternIndicatorFor(List<Beat> beats, int beatNum, BeatStats stats) {

		if ((beatNum < 0) || (beatNum >= beats.size())) {
			return 0;
		}

		long baseJigglieness = divOr255(255 * beats.get(beatNum).getJigglieness(), stats.getMaxJigglieness());

		// if we are much quieter than maximum, then we will use this factor to reduce the drumPatternIndicator...
		double relativeLoudnessFactor = 2 * beats.get(beatNum).getLoudness() / stats.getMaxLoudness();
		if (relativeLoudnessFactor > 1.0) {
			// ... however, we will NOT use the loudness to increase the drumPatternIndicator!
			relativeLoudnessFactor = 1.0;
		}

		return (long) (baseJigglieness * relativeLoudnessFactor);
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

	private void addFadedWavMono(WavFile wav, int samplePos, double wavVolume) {

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
			fadeDataLeft[i+pos] += (int) (wavVolume * newMono[i]);
			fadeDataRight[i+pos] += (int) (wavVolume * newMono[i]);
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

	public int beatToFrame(Beat beat) {
		return millisToFrame(channelPosToMillis(beat.getPosition()));
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
