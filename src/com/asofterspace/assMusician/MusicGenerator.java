/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician;

import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.WavFile;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;


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

	// width (px), height (px) and frame rate (fps) of the resulting video
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

		System.out.println("Adding drums to " + originalSong.getLocalFilename());

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
		System.out.println("Executing " + ffmpegInvocation);
		IoUtils.execute(ffmpegInvocation);
		Utils.sleep(1000);

		// load the extracted audio
		WavFile wav = new WavFile(workSong);
		this.byteRate = wav.getByteRate();
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		wavDataLeft = wav.getLeftData();
		wavDataRight = wav.getRightData();
		scale(0.5);

		// add drums
		addDrums();

		// save the new song as audio
		WavFile newSongFile = new WavFile(workDir, "our_song.wav");
		newSongFile.setNumberOfChannels(2);
		newSongFile.setSampleRate(wav.getSampleRate());
		newSongFile.setByteRate(byteRate);
		newSongFile.setBitsPerSample(bytesPerSample * 8);
		newSongFile.setLeftData(wavDataLeft);
		newSongFile.setRightData(wavDataRight);
		newSongFile.save();

		// generate a video for the song
		// TODO
		// maybe some geometric thingy
		// in the middle (maybe moving
		// based on the music, maybe made
		// to look a bit gritty and real,
		// not JUST abstract), and
		// some pop colors around,
		// maybe slowly (?) changing
		// colors, maybe even blinking
		// whenever a drum is played etc.
		// (at least the stuff that we
		// add we have full information
		// about, so we can do whatever
		// we want with it!)
		// also see for inpiration about videos: Gealdýr - Sær
		// EVEN BETTER - for greatness of videoness, see those funky cyberpunk thingies Michi likes,
		//   which are showing a cockpit in purple/pink and travel along a road... and then have the
		//   road (and ultimately, maybe even a space-parcours etc.) autogenerated and somesuch!
		//   with blackish blackground! and funk!
		int totalFrameAmount = calcTotalFrameAmount();
		for (int i = 0; i < totalFrameAmount; i++) {
			ColorRGB black = new ColorRGB(0, 0, 0);
			Image img = new Image(width, height);
			img.drawRectangle(0, 0, width-1, height-1, black);
			DefaultImageFile curImgFile = new DefaultImageFile(
				workDir.getAbsoluteDirname() + "/pic" + StrUtils.leftPad0(i, 5) + ".png"
			);
			curImgFile.assign(img);
			curImgFile.save();
		}

		// splice the generated audio together with the generated video
		// TODO

		// WavFile newSongFile = new WavFile(outputDir, originalSong.getLocalFilenameWithoutType() + ".wav");

		// upload it to youtube
		// (and in the description, link to the original song)
		// TODO
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
	private void addDrums() {

		// add rev sound at the beginning at double volume
		addWavMono(WAV_REV_DRUM, 0, 4);

		// find the beat in the loaded song - by sliding a window of several samples,
		// and only when all samples within the window are increasing, calling it
		// a maximum
		int windowLength = 32;
		int addTimes = 0;
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

		System.out.println("We added " + addTimes + " drum sounds!");

		// determine which drums sounds to add where
		// TODO

		// actually put them into the song
		addDrum(wavDataLeft, 3000);
		addDrum(wavDataRight, 3000);
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

	// TODO:
	// try out what happens if we replace current mixin with max of the two (or min? or... hm... ^^)
	private int mixin(int one, int two) {
		long newVal = one + two;
		if (newVal > Integer.MAX_VALUE) {
			newVal = Integer.MAX_VALUE;
		} else if (newVal < Integer.MIN_VALUE) {
			newVal = Integer.MIN_VALUE;
		}
		return (int) newVal;
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
