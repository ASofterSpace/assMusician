/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.assMusician.music.AbsMaxPos;
import com.asofterspace.assMusician.music.BeatGenerator;
import com.asofterspace.assMusician.music.DrumSoundAtPos;
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

		List<String> debugLog = new ArrayList<>();

		debugLog.add("{start log}");
		debugLog.add(Utils.getFullProgramIdentifierWithDate());

		debugLog.add("Song Analysis");
		debugLog.add(": Load Audio");

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
		debugLog.add("  :: input byte rate: " + this.byteRate);
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		debugLog.add("  :: input bytes per sample: " + this.bytesPerSample);
		SoundData wavSoundData = wav.getSoundData();
		debugLog.add("  :: sound data length: " + wavSoundData.getLength() + " pos");

		// cut off silence from the front and back (basically trim() for the wav file... ^^)
		// but add one second of silence in the end!
		debugLog.add(": Start Pre-Processing");
		int addAmountMS = 1000;
		int addAmount = millisToChannelPos(addAmountMS);
		debugLog.add("  :: trim and add: " + addAmountMS + " ms");
		debugLog.add("  :: trim and add: " + addAmount + " pos");
		wavSoundData.trimAndAdd(addAmount);
		// fade in and out for 1 second each
		int fadeInAmountMS = 1000;
		int fadeInAmount = millisToChannelPos(fadeInAmountMS);
		debugLog.add("  :: fade in: " + fadeInAmountMS + " ms");
		debugLog.add("  :: fade in: " + fadeInAmount + " pos");
		wavSoundData.fadeIn(fadeInAmount);
		int fadeOutAmountMS = 1000;
		int fadeOutAmount = millisToChannelPos(fadeOutAmountMS);
		debugLog.add("  :: fade out: " + fadeOutAmountMS + " ms");
		debugLog.add("  :: fade out: " + fadeOutAmount + " pos");
		wavSoundData.fadeOut(fadeOutAmount);

		debugLog.add("  :: sound data length: " + wavSoundData.getLength() + " pos");

		Waveform origWaveform = new Waveform(wavSoundData);


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

		// the visual output graph gets 2400 px width per minute of song
		wavGraphImg = new GraphImage();
		wavGraphImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 25, graphImageHeight);

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
		List<Beat> drumBeats = getDrumBeats(debugLog);

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition.png");
		wavImgFile.assign(wavGraphImg);
		wavImgFile.save();

		List<DrumSoundAtPos> addedDrumSounds = addDrumsBasedOnBeats(drumBeats, debugLog);

		debugLog.add("Song Finalization");
		debugLog.add(": Start Post-Processing");

		// add rev sound at the beginning at high volume
		int revPos = 0;
		int revLoudness = 8;
		addWavMono(WAV_REV_DRUM, revPos, revLoudness);
		debugLog.add("  :: add rev sound at " + revPos + " pos");
		debugLog.add("  :: rev sound loudness: " + revLoudness);

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
		debugLog.add("  :: normalizing sound data");
		debugLog.add("  :: max before: " + soundData.getMax());
		soundData.normalize();
		debugLog.add("  :: max after: " + soundData.getMax());

		Waveform newWaveform = new Waveform(soundData);

		// save the new song as audio
		debugLog.add(": Save Audio");
		WavFile newSongFile = new WavFile(workDir, "our_song.wav");
		int outChannelNum = 2;
		debugLog.add("  :: number of channels: " + outChannelNum);
		newSongFile.setNumberOfChannels(outChannelNum);
		debugLog.add("  :: sample rate: " + wav.getSampleRate());
		newSongFile.setSampleRate(wav.getSampleRate());
		debugLog.add("  :: byte rate: " + byteRate);
		newSongFile.setByteRate(byteRate);
		debugLog.add("  :: bytes per sample: " + bytesPerSample);
		debugLog.add("  :: bits per sample: " + (bytesPerSample * 8));
		newSongFile.setBitsPerSample(bytesPerSample * 8);
		debugLog.add("  :: sound data length: " + soundData.getLength());
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

		SimpleFile debugLogFile = new SimpleFile(workDir, "debug_after_music.txt");
		debugLogFile.saveContents(debugLog);

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

			// Fourier analysis...

			debugLog.add(": Fourier Analysis");

			debugLog.add("  :: frame per Fourier: " + framesPerFourier + " pos");
			debugLog.add("  :: frame rate: " + frameRate);
			fourierLen = millisToChannelPos((1000 * framesPerFourier) / frameRate);
			debugLog.add("  :: Fourier length: " + fourierLen + " pos");
			int fourierNum = 0;
			int fourierMax = 0;
			int fourierAmount = wavSoundData.getLength() / fourierLen;
			debugLog.add("  :: Fourier amount: " + fourierAmount);
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

				if (fourierNum % (fourierAmount / 32) == 0) {
					debugLog.add("    ::: [" + fourierNum + "] Fourier max: " + fourierMax);
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

			debugLog.add("  :: Fourier max: " + fourierMax);

			debugLog.add("Video Generation");
			debugLog.add(": Setup");

			String songTitle = originalSong.getLocalFilename();
			if (songTitle.contains(".")) {
				songTitle = songTitle.substring(0, songTitle.lastIndexOf("."));
			}
			debugLog.add("  :: input song title: " + songTitle);
			songTitle += " (Remix with Drums)";
			debugLog.add("  :: output song title: " + songTitle);

			int totalFrameAmount = calcTotalFrameAmount();
			debugLog.add("  :: video width: " + width + " px");
			debugLog.add("  :: video height: " + height + " px");
			debugLog.add("  :: frame rate: " + frameRate + " frames / sec");
			debugLog.add("  :: frames per Fourier: " + framesPerFourier);
			debugLog.add("  :: calculated frame amount: " + totalFrameAmount);

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

			vidGenny.generateVideoBasedOnBeats(drumBeats, totalFrameAmount, width, height, wavGraphImg,
				origWaveform, newWaveform, songTitle, framesPerFourier, fouriers, addedDrumSounds, debugLog);
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

		debugLogFile = new SimpleFile(workDir, "debug_final.txt");
		debugLogFile.saveContents(debugLog);

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
	private List<Beat> getDrumBeats(List<String> debugLog) {

		debugLog.add("Starting Beat Detection");


		// ALGORITHM 6

		debugLog.add(": Starting Algorithm 6");

		// We first iterate over the entire song and try to find maxima, by taking the abs
		// of the entire waveform, and then sliding a window of length 1 sec over the entire
		// abs song and within this 1 sec taking the max and checking if it takes more than
		// twice as long for the song to return to baseline as it did for it to raise from
		// baseline - basically, we want to detect stuff like this:
		//    |\  | \    |\
		// ---| >-|  =---| >--
		//    |/  |_/    |/
		// ... so it seems like the rapid ascend and slow descend are actually quite critical
		// here!
		// then - after we just detected the presence of these maxima - we want to assign a
		// quality to them, based on their loudness relative to the overall loudness of the
		// song, as well as on how long it really does take to return to local baseline
		// (in all this, we take as local baseline the max of the 25% of lowest values)
		// oh oh oh - and for all that, we don't want to look at the (abs) data directly, but
		// instead we want to smooch it all together into 128 "sound pixels" per second, which
		// are always the max of the actual (abs) data in there! so that we don't have to think
		// about regular wubbelings!

		int soundPixPerWindow = 128;
		int soundPixelWindowMS = 500;
		int soundPixelLen = millisToChannelPos(soundPixelWindowMS) / soundPixPerWindow;

		debugLog.add("  :: window length: " + soundPixelWindowMS + " ms");
		debugLog.add("  :: sound pixels per window: " + soundPixPerWindow);
		debugLog.add("  :: sound pixel length: " + ((soundPixelWindowMS * 1.0) / soundPixPerWindow) + " ms");
		debugLog.add("  :: sound pixel length: " + soundPixelLen + " pos");
		debugLog.add("  :: input wave length: " + wavDataLeft.length + " pos");
		int[] absLeft = new int[(wavDataLeft.length / soundPixelLen) + 1];
		debugLog.add("  :: output abs sound pixels: " + absLeft.length + " spx");

		debugLog.add("  :: setting abs sound pixels to zero");
		for (int i = 0; i < absLeft.length; i++) {
			absLeft[i] = 0;
		}

		debugLog.add("  :: setting abs sound pixels to max of abs input data");
		for (int i = 0; i < wavDataLeft.length; i++) {
			absLeft[i / soundPixelLen] = Math.max(absLeft[i / soundPixelLen], Math.abs(wavDataLeft[i]));
		}

		debugLog.add("  :: smoothening abs sound pixels");
		int[] absLeftSmooth = new int[absLeft.length];
		for (int i = 0; i < absLeft.length; i++) {
			if (i < 2) {
				absLeftSmooth[i] = (absLeft[i] + absLeft[i+1] + absLeft[i+2]) / 3;
				continue;
			}
			if (i >= absLeft.length - 2) {
				absLeftSmooth[i] = (absLeft[i-2] + absLeft[i-1] + absLeft[i]) / 3;
				continue;
			}
			absLeftSmooth[i] = (absLeft[i-2] + absLeft[i-1] + absLeft[i] + absLeft[i+1] + absLeft[i+2]) / 5;
		}
		absLeft = absLeftSmooth;

		debugLog.add("  :: determining abs sound pixel stats");
		int absMin = Integer.MAX_VALUE;
		int absMax = -1;
		for (int i = 0; i < absLeft.length; i++) {
			if (absLeft[i] > absMax) {
				absMax = absLeft[i];
			}
			if (absLeft[i] < absMin) {
				absMin = absLeft[i];
			}
		}

		debugLog.add("  :: detected abs min: " + absMin);
		debugLog.add("  :: detected abs max: " + absMax);

		if (absMin != 0) {
			debugLog.add("  :: resetting abs min to zero");
			absMax -= absMin;
			absMin -= absMin;
			debugLog.add("  :: new abs max: " + absMax);
			debugLog.add("  :: new abs min: " + absMin);
			for (int i = 0; i < absLeft.length; i++) {
				absLeft[i] = absLeft[i] - absMin;
			}
		}

		List<Integer> absLeftList = new ArrayList<>();
		for (int i = 0; i < absLeft.length; i++) {
			absLeftList.add(absLeft[i]);
		}

		Collections.sort(absLeftList);
		int abs25 = absLeftList.get(absLeftList.size() / 4);
		int abs50 = absLeftList.get(absLeftList.size() / 2);
		int abs75 = absLeftList.get((absLeftList.size() * 3) / 4);
		int abs80 = absLeftList.get((absLeftList.size() * 4) / 5);
		debugLog.add("  :: detected abs 25%: " + abs25);
		debugLog.add("  :: detected abs 50%: " + abs50);
		debugLog.add("  :: detected abs 75%: " + abs75);
		debugLog.add("  :: detected abs 80%: " + abs80);

		int windowSize = soundPixPerWindow;
		int windowHalfSize = windowSize / 2;
		debugLog.add("  :: sliding a " + windowSize + " spx window over the array to find maxima");

		List<AbsMaxPos> absMaxPositions = new ArrayList<>();

		for (int i = windowHalfSize; i < absLeft.length - windowHalfSize; i++) {
			int curMaxPos = -1;
			int curMaxVal = -1;
			for (int j = i - windowHalfSize; j < i + windowHalfSize; j++) {
				if (absLeft[j] > curMaxVal) {
					curMaxVal = absLeft[j];
					curMaxPos = j;
				}
			}
			// the current sound pixel is exactly the maximum of windowHalfSize before and windowHalfSize after it
			if (curMaxPos == i) {
				// get information about the immediate environment - that is, three times the window size
				List<Integer> absLocalList = new ArrayList<>();
				for (int j = i - 3*windowHalfSize; j < i + 3*windowHalfSize; j++) {
					if ((j >= 0) && (j < absLeft.length)) {
						absLocalList.add(absLeft[j]);
					}
				}
				Collections.sort(absLocalList);
				int absLocalMin = absLocalList.get(0);
				int absLocal25 = absLocalList.get(absLocalList.size() / 4);
				int absLocal50 = absLocalList.get(absLocalList.size() / 2);
				int absLocal75 = absLocalList.get((absLocalList.size() * 3) / 4);
				int absLocal80 = absLocalList.get((absLocalList.size() * 4) / 5);
				int absLocalMax = absLocalList.get(absLocalList.size() - 1);

				// get the distance from the max to the start of it going up from 25%
				int distToStart = 3*windowHalfSize;
				for (int j = i; j > i - 3*windowHalfSize; j--) {
					if (j < 0) {
						break;
					}
					if (absLeft[j] < absLocal25) {
						distToStart = i - j;
						break;
					}
				}

				// get the distance from the max to the end of it going down to 25%
				int distToEnd = 3*windowHalfSize;
				for (int j = i; j < i + 3*windowHalfSize; j++) {
					if (j >= absLeft.length) {
						break;
					}
					if (absLeft[j] < absLocal25) {
						distToEnd = j - i;
						break;
					}
				}

				AbsMaxPos absMaxPos = new AbsMaxPos(curMaxPos);

				// get 10 points for this value being the local max value, less if it is lower
				double qual = (10.0 * curMaxVal) / absLocalMax;
				// get 20 points for this value being double the abs local 50
				qual += (10.0 * curMaxVal) / absLocal50;
				// get 20 points for the start distance being twice as short as the end distance
				qual += (10.0 * distToEnd) / distToStart;
				// get 5 points for this value being above 50% of the overall max value
				qual += (10.0 * curMaxVal) / absMax;
				absMaxPos.setQuality(qual);

				absMaxPos.setOrigPosition(curMaxPos * soundPixelLen);

				absMaxPositions.add(absMaxPos);
			}
		}

		debugLog.add("  :: found " + absMaxPositions.size() + " abs max positions");

		List<AbsMaxPos> absMaxPositionsTemporalOrdered = new ArrayList<>();
		for (AbsMaxPos absMaxPos : absMaxPositions) {
			absMaxPositionsTemporalOrdered.add(absMaxPos);
		}

		Collections.sort(absMaxPositions, new Comparator<AbsMaxPos>() {
			public int compare(AbsMaxPos a, AbsMaxPos b) {
				if (a.getQuality() - b.getQuality() > 0) {
					return 1;
				}
				if (b.getQuality() - a.getQuality() > 0) {
					return -1;
				}
				return 0;
			}
		});

		debugLog.add("  :: lowest quality: " + absMaxPositions.get(0).getQuality());
		debugLog.add("  :: highest quality: " + absMaxPositions.get(absMaxPositions.size() - 1).getQuality());

		List<Integer> maximumPositions = new ArrayList<>();
		for (int i = 0; i < absMaxPositions.size() / 2; i++) {
			AbsMaxPos absMaxPos = absMaxPositions.get(i);
			wavGraphImg.drawVerticalLineAt(absMaxPos.getOrigPosition(), new ColorRGB(128, 128, 0));
		}
		for (int i = absMaxPositions.size() / 2; i < absMaxPositions.size(); i++) {
			AbsMaxPos absMaxPos = absMaxPositions.get(i);
			maximumPositions.add(absMaxPos.getOrigPosition());
			wavGraphImg.drawVerticalLineAt(absMaxPos.getOrigPosition(), new ColorRGB(255, 0, 0));
		}

		debugLog.add("  :: converted the highest " + maximumPositions.size() + " values into maximum positions");

		Collections.sort(maximumPositions);

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_detection.png");
		wavImgFile.assign(wavGraphImg);
		wavImgFile.save();


		// ALGORITHM 3.6

		// We take algorithm 3 as basis and try to get one number for the song's overall bpm (beats per minute)
		// To do so, we look at all pairs of detected maxima, and their distances as raw beat lengths
		// We then scale the raw beat lengths (by doubling or halfing againd again) until they all fall
		// into our preferred bpm band - that is, we would like to have between 90 and 180 bpm (we have
		// to choose some range, and this range seems like it will make the resulting sound quite energetic,
		// which we like!)
		// We then have lots and lots of estimates for the bpm, which we all put into buckets - and the
		// largest bucket wins!

		debugLog.add(": Starting Algorithm 3.6");

		Collections.sort(maximumPositions);

		// each bpm candidate is an int representing a bpm value times 10 (so that we have a bit more accuracy),
		// mapping to an int which represents how many values we have put into this bucket
		int BUCKET_ACCURACY_FACTOR = 1;
		int MIN_BPM = 90;
		int MAX_BPM = 180;
		int LOOKBACK = 1;

		debugLog.add("  :: bucket accuracy factor: " + BUCKET_ACCURACY_FACTOR);
		debugLog.add("  :: min bpm: " + MIN_BPM);
		debugLog.add("  :: max bpm: " + MAX_BPM);
		debugLog.add("  :: lookback: " + LOOKBACK);

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

		debugLog.add("  :: " + candFound + " bpm candidates found overall");
		debugLog.add("  :: " + candDistinct + " distinct bpm candidates found");

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

		debugLog.add("  :: " + bucketAmount + " buckets used");
		debugLog.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugLog.add("  :: largest bucket value: " + largestBucketBpm);
		debugLog.add("  :: bpm based on largest bucket: " + bpm);
		int generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugLog.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		System.out.println("We detected " + bpm + " beats per minute, " +
			"with the largest bucket containing " + largestBucketContentAmount + " values...");


		// ALGORITHM 7

		debugLog.add(": Starting Algorithm 7");

		BeatGenerator beatGenny = new BeatGenerator(debugLog);

		List<Integer> generatedBeatDistances = new ArrayList<>();
		generatedBeatDistances.add(generatedBeatDistance);
		int DIFF_BEAT_AMOUNT = 32;
		for (int i = 0; i < DIFF_BEAT_AMOUNT; i++) {
			generatedBeatDistances.add(generatedBeatDistance - ((i*generatedBeatDistance) / 200));
		}
		for (int i = 0; i < DIFF_BEAT_AMOUNT; i++) {
			generatedBeatDistances.add(generatedBeatDistance + ((i*generatedBeatDistance) / 200));
		}
		debugLog.add("  :: attempting alignment with " + generatedBeatDistances.size() + " slightly different bpm values");

		AbsMaxPos bestAbsMax = null;
		int bestAlignmentQuality = -1;
		int bestGeneratedBeatDistance = generatedBeatDistance;
		int uncertaintyFrontSetting = 1;
		int uncertaintyBackSetting = 1;

		for (Integer curGeneratedBeatDistance : generatedBeatDistances) {
			debugLog.add("  :: uncertainty front setting: " + uncertaintyFrontSetting + " / 10");
			debugLog.add("  :: uncertainty back setting: " + uncertaintyBackSetting + " / 10");
			debugLog.add("  :: generated beat distance: " + curGeneratedBeatDistance + " pos");

			for (int i = absMaxPositions.size() / 2; i < absMaxPositions.size(); i++) {
				AbsMaxPos absMaxPos = absMaxPositions.get(i);
				beatGenny.generateBeatsFor(absMaxPos, absMaxPositionsTemporalOrdered, wavDataLeft.length, curGeneratedBeatDistance, uncertaintyFrontSetting, uncertaintyBackSetting);
				if (beatGenny.getAlignmentQuality() > bestAlignmentQuality) {
					bestAbsMax = absMaxPos;
					bestAlignmentQuality = beatGenny.getAlignmentQuality();
					bestGeneratedBeatDistance = curGeneratedBeatDistance;
				}
			}
		}

		debugLog.add("  :: best abs max: " + bestAbsMax);
		debugLog.add("  :: best generated beat distance: " + bestGeneratedBeatDistance);
		debugLog.add("  :: best alignment quality: " + bestAlignmentQuality);
		beatGenny.generateBeatsFor(bestAbsMax, absMaxPositionsTemporalOrdered, wavDataLeft.length, bestGeneratedBeatDistance, uncertaintyFrontSetting, uncertaintyBackSetting);
		List<Integer> bpmBasedBeats = beatGenny.getBeats();

		debugLog.add("  :: adding detected beats to debug graph");
		for (Integer beatPos : bpmBasedBeats) {
			wavGraphImg.drawVerticalLineAt(beatPos, new ColorRGB(0, 255, 0));
		}

		// ALGORITHM 3.7

		debugLog.add(": Starting Algorithm 3.7");

		// Now, after all that is done... the beats are detected, the bpm decided, the beats generated
		// and subsequently aligned against the ones previously detected... we have a problem: as we
		// just aligned the beats, some distances between beats are (noticeably!) different - e.g.
		// if we had beats at 1, 3, 5, 7, and then aligned it and it is now 1, 3, 4.8, 6.8, then the
		// distances from 3 to 4.8 being suddenly different will sound weird... so let's smoothen it!
		// that is, for each we get the distance to front and back, and align to the middle of that:
		// 1, 2.9, 4.9, 6.8... and again! ... and then we are good! :D

		int SMOOTH_AMOUNT = 3;
		debugLog.add("  :: smoothening " + SMOOTH_AMOUNT + " times");
		for (int i = 0; i < SMOOTH_AMOUNT; i++) {
			debugLog.add("  :: smoothening #" + i);
			bpmBasedBeats = smoothenBeats(bpmBasedBeats, debugLog);
		}

		Collections.sort(bpmBasedBeats);


		List<GraphDataPoint> wavData = new ArrayList<>();
		int position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		/*
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
		*/

		GraphImage postSmoothImg = new GraphImage();
		postSmoothImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 25, graphImageHeight);
		postSmoothImg.setDataColor(new ColorRGB(0, 0, 255));
		postSmoothImg.setAbsoluteDataPoints(wavData);

		for (Integer pos : maximumPositions) {
			postSmoothImg.drawVerticalLineAt(pos, new ColorRGB(255, 0, 128));
		}

		for (Integer i : bpmBasedBeats) {
			postSmoothImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
		}
		DefaultImageFile postSmoothImgFile = new DefaultImageFile(workDir, "waveform_drum_post_smoothen.png");
		postSmoothImgFile.assign(postSmoothImg);
		postSmoothImgFile.save();


		List<Beat> beats = new ArrayList<>();

		int curBeat = 0;
		int curBeatLength = 0;
		long curBeatLoudness = 0;
		long curBeatJigglieness = 0;
		long jiggleModifier = 16*16*16;
		debugLog.add("  :: jiggle modifier: " + jiggleModifier);
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

		debugLog.add("  :: " + beats.size() + " beats detected");
		System.out.println("We detected " + beats.size() + " beats!");

		return beats;
	}

	private List<Integer> smoothenBeats(List<Integer> bpmBasedBeats, List<String> debugLog) {

		Collections.sort(bpmBasedBeats);

		if (bpmBasedBeats.size() > 1) {
			List<Integer> smoothBeats = new ArrayList<>();
			smoothBeats.add(bpmBasedBeats.get(0));
			debugLog.add("    ::: [0] before: " + bpmBasedBeats.get(0) + ", after: " + bpmBasedBeats.get(0));
			smoothBeats.add(bpmBasedBeats.get(1));
			debugLog.add("    ::: [1] before: " + bpmBasedBeats.get(1) + ", after: " + bpmBasedBeats.get(1));
			for (int i = 2; i < bpmBasedBeats.size() - 2; i++) {
				int before = bpmBasedBeats.get(i);
				int after = (bpmBasedBeats.get(i-2) + 2*bpmBasedBeats.get(i-1) + before + 2*bpmBasedBeats.get(i+1) + bpmBasedBeats.get(i+2)) / 7;
				smoothBeats.add(after);
				if (i < 16) {
					debugLog.add("    ::: [" + i + "] before: " + before + ", after: " + after);
				}
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

	private List<DrumSoundAtPos> addDrumsBasedOnBeats(List<Beat> beats, List<String> debugLog) {

		debugLog.add("Starting Drum Addition");

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

		BeatStats stats = new BeatStats(beats);

		debugLog.add(": Starting Beat Input Analysis");

		System.out.println("");
		System.out.println("averageLength: " + stats.getAverageLength());
		System.out.println("averageLoudness: " + stats.getAverageLoudness());
		System.out.println("averageJigglieness: " + stats.getAverageJigglieness());
		System.out.println("maxLength: " + stats.getMaxLength());
		System.out.println("maxLoudness: " + stats.getMaxLoudness());
		System.out.println("maxJigglieness: " + stats.getMaxJigglieness());

		debugLog.add("  :: received " + beats.size() + " beats as input");
		debugLog.add("  :: average length: " + stats.getAverageLength());
		debugLog.add("  :: average loudness: " + stats.getAverageLoudness());
		debugLog.add("  :: average igglieness: " + stats.getAverageJigglieness());
		debugLog.add("  :: max length: " + stats.getMaxLength());
		debugLog.add("  :: max loudness: " + stats.getMaxLoudness());
		debugLog.add("  :: max jigglieness: " + stats.getMaxJigglieness());

		debugLog.add(": Starting Drum Sound Addition");

		List<DrumSoundAtPos> drumSounds = new ArrayList<>();

		Map<Integer, Integer> drumSoundTally = new HashMap<>();
		drumSoundTally.put(0, 0);
		drumSoundTally.put(1, 0);
		drumSoundTally.put(2, 0);
		drumSoundTally.put(3, 0);
		drumSoundTally.put(12, 0);
		drumSoundTally.put(13, 0);

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

			drumSounds.add(new DrumSoundAtPos(curBeat, curBeatLen, baseLoudness, (int) drumPatternIndicator));
		}

		// we have encountered the following jigglienesses in the wild:
		//  2600 .. singing with nearly no instruments
		// 16900 .. loud singing with some instruments
		// 20100 .. full blast! :D

		// however, we want to prevent the whole song from being played as constantly full blast drums...
		// so instead, let's adjust the levels down a bit if we notice that too many drum sounds go so far up!

		int pattern3startsAbove = 19600;
		int pattern2startsAbove = 12800;
		int pattern1startsAbove = 9600;

		debugLog.add("  :: normalizing beat pattern indicators across entire song");
		debugLog.add("    ::: pattern 1 originally above " + pattern1startsAbove);
		debugLog.add("    ::: pattern 2 originally above " + pattern2startsAbove);
		debugLog.add("    ::: pattern 3 originally above " + pattern3startsAbove);

		List<DrumSoundAtPos> actuallyPlayedDrumSounds = new ArrayList<>();
		for (DrumSoundAtPos drumSound : drumSounds) {
			if (drumSound.getDrumPatternIndicator() > pattern1startsAbove) {
				actuallyPlayedDrumSounds.add(drumSound);
			}
		}

		// sort by drum pattern indicator, ascending
		Collections.sort(actuallyPlayedDrumSounds, new Comparator<DrumSoundAtPos>() {
			public int compare(DrumSoundAtPos a, DrumSoundAtPos b) {
				return a.getDrumPatternIndicator() - b.getDrumPatternIndicator();
			}
		});

		// ensure that at least 80% of all drum sounds are below pattern 3 (so pattern 3 starts at 80% or higher)
		if (actuallyPlayedDrumSounds.get((4 * actuallyPlayedDrumSounds.size()) / 5).getDrumPatternIndicator() > pattern3startsAbove) {
			pattern3startsAbove = actuallyPlayedDrumSounds.get((4 * actuallyPlayedDrumSounds.size()) / 5).getDrumPatternIndicator();
		}

		// ensure that at least 50% of all drum sounds are below pattern 2 (so pattern 2 starts at 50% or higher)
		if (actuallyPlayedDrumSounds.get(actuallyPlayedDrumSounds.size() / 2).getDrumPatternIndicator() > pattern2startsAbove) {
			pattern2startsAbove = actuallyPlayedDrumSounds.get(actuallyPlayedDrumSounds.size() / 2).getDrumPatternIndicator();
		}

		debugLog.add("    ::: pattern 1 now above " + pattern1startsAbove);
		debugLog.add("    ::: pattern 2 now above " + pattern2startsAbove);
		debugLog.add("    ::: pattern 3 now above " + pattern3startsAbove);

		for (int i = 0; i < drumSounds.size(); i++) {

			DrumSoundAtPos curSound = drumSounds.get(i);
		}

		debugLog.add("  :: converting beat pattern indicators into beat patterns");

		for (int i = 0; i < drumSounds.size(); i++) {

			DrumSoundAtPos curSound = drumSounds.get(i);

			if (curSound.getDrumPatternIndicator() > pattern3startsAbove) {
				curSound.setBeatPattern(3);
			} else if (curSound.getDrumPatternIndicator() > pattern2startsAbove) {
				curSound.setBeatPattern(2);
			} else if (curSound.getDrumPatternIndicator() > pattern1startsAbove) {
				curSound.setBeatPattern(1);
			} else {
				curSound.setBeatPattern(0);
			}
		}

		debugLog.add("  :: adjusting beat patterns based on pattern environment");

		for (int i = 0; i < drumSounds.size(); i++) {

			DrumSoundAtPos curSound = drumSounds.get(i);
			DrumSoundAtPos nextSound = null;
			if (i + 1 < drumSounds.size()) {
				nextSound = drumSounds.get(i + 1);
			}

			// if we are coming from a lot of drum-ness...
			if (curSound.getBeatPattern() == 3) {
				// ... and going over to less drum-ness...
				if ((nextSound == null) || (nextSound.getBeatPattern() == 0)) {
					// ... then make a nice big bang in the end!
					curSound.setBeatPattern(13);
				}
			}

			// if we are coming from a lot of drum-ness...
			if (curSound.getBeatPattern() == 2) {
				// ... and going over to less drum-ness...
				if ((nextSound == null) || (nextSound.getBeatPattern() == 0)) {
					// ... then make a nice big bang in the end!
					curSound.setBeatPattern(12);
				}
			}

			drumSoundTally.put(curSound.getBeatPattern(), curSound.getBeatPattern() + 1);
		}

		debugLog.add("    ::: determined " + drumSoundTally.get(0) + " beats without drums");
		debugLog.add("    ::: determined " + drumSoundTally.get(1) + " beats with drum pattern 1");
		debugLog.add("    ::: determined " + drumSoundTally.get(2) + " beats with drum pattern 2");
		debugLog.add("    ::: determined " + drumSoundTally.get(3) + " beats with drum pattern 3");
		debugLog.add("    ::: determined " + drumSoundTally.get(12) + " beats with drum pattern 12");
		debugLog.add("    ::: determined " + drumSoundTally.get(13) + " beats with drum pattern 13");

		debugLog.add("  :: adding actual drum sounds to the audio tracks");

		for (int i = 0; i < drumSounds.size(); i++) {

			DrumSoundAtPos drumSound = drumSounds.get(i);

			int curBeat = drumSound.getBeatPos();
			double baseLoudness = drumSound.getBaseLoudness();
			int curBeatLen = drumSound.getBeatLength();

			if (drumSound.getBeatPattern() > 0) {
				beats.get(i).setChanged(true);
			}

			switch (drumSound.getBeatPattern()) {
				/*
				case 13:
					addFadedWavMono(WAV_SMALL_F_TIMPANI, curBeat, 0.8*baseLoudness);
					addFadedWavMono(WAV_SMALL_F_TIMPANI, curBeat + ((2 * curBeatLen) / 8), 0.6*baseLoudness);
					addFadedWavMono(WAV_SMALL_F_TIMPANI, curBeat + ((4 * curBeatLen) / 8), 0.8*baseLoudness);
					break;
				case 12:
					addFadedWavMono(WAV_SMALL_F_TIMPANI, curBeat, 0.8*baseLoudness);
					break;
				*/
				case 13:
					addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2.5*baseLoudness);
					addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
					addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), 1.25*baseLoudness);
					addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), 1.5*baseLoudness);
					break;
				case 12:
					addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
					addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
					break;
				case 3:
					addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
					addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
					addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((5 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((6 * curBeatLen) / 8), baseLoudness);
					break;
				case 2:
					addFadedWavMono(WAV_TOM1_DRUM, curBeat, 1.25*baseLoudness);
					addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
					addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
					addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
					break;
				case 1:
					addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
					addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
					break;
			}
		}

		DefaultImageFile wavImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_stats.png");
		wavImgFile.assign(graphImg);
		wavImgFile.save();

		return drumSounds;
	}

	private long getDrumPatternIndicatorFor(List<Beat> beats, int beatNum, BeatStats stats) {

		if ((beatNum < 0) || (beatNum >= beats.size())) {
			return 0;
		}

		long baseJigglieness = divOr255(25500 * beats.get(beatNum).getJigglieness(), stats.getMaxJigglieness());

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

		addedSounds.add(samplePos);
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

	public int channelPosToMillis(long channelPos) {
		long NUM_OF_CHANNELS = 2;
		return (int) ((channelPos * 1000 * bytesPerSample * NUM_OF_CHANNELS) / byteRate);
	}

	private int calcTotalFrameAmount() {
		return millisToFrame(channelPosToMillis(wavDataLeft.length));
	}

	public int millisToFrame(int millis) {
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
