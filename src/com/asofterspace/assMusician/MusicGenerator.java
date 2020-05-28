/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.WavFile;
import com.asofterspace.toolbox.Utils;


public class MusicGenerator {

	private final WavFile WAV_REV_DRUM;
	private final WavFile WAV_SNR_DRUM;
	private final WavFile WAV_TOM1_DRUM;
	private final WavFile WAV_TOM2_DRUM;
	private final WavFile WAV_TOM3_DRUM;
	private final WavFile WAV_TOM4_DRUM;

	private Database database;

	private Directory inputDir;
	private Directory outputDir;
	private Directory workDir;

	private int byteRate;
	private int bytesPerSample;
	private int[] wavDataLeft;
	private int[] wavDataRight;


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
		Utils.sleep(500);

		// load the extracted audio
		WavFile wav = new WavFile(workSong);
		this.byteRate = wav.getByteRate();
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		wavDataLeft = wav.getLeftData();
		wavDataRight = wav.getRightData();

		// add drums
		addDrums();

		// save the new song as audio
		WavFile newSongFile = new WavFile(outputDir, originalSong.getLocalFilenameWithoutType() + ".wav");
		newSongFile.setNumberOfChannels(2);
		newSongFile.setSampleRate(wav.getSampleRate());
		newSongFile.setByteRate(wav.getByteRate());
		newSongFile.setBitsPerSample(2);
		newSongFile.setLeftData(wavDataLeft);
		newSongFile.setRightData(wavDataRight);
		newSongFile.save();

		// generate a video for the song
		// TODO

		// splice the generated audio together with the generated video
		// TODO

		// upload it to youtube
		// TODO
	}

	private void addDrums() {

		// add rev sound at the beginning at double volume
		addWav(WAV_REV_DRUM, 0, 4);

		// find the beat in the loaded song
		// TODO

		// determine which drums sounds to add where
		// TODO

		// actually put them into the song
		addDrum(wavDataLeft, 3000);
		addDrum(wavDataRight, 3000);
	}

	/**
	 * Add a WAV file by mixing it in left and right at a given position in milliseconds
	 */
	private void addWav(WavFile wav, int posInMillis, int wavVolume) {

		wav.normalizeTo16Bits();

		int[] newLeft = wav.getLeftData();
		int[] newRight = wav.getRightData();

		int pos = millisToBytePos(posInMillis);

		int len = newLeft.length;
		if (len + pos > wavDataLeft.length) {
			len = wavDataLeft.length - pos;
		}

		for (int i = 0; i < len; i++) {
			wavDataLeft[i+pos] = mixin(wavDataLeft[i+pos], wavVolume * newLeft[i]);
			wavDataRight[i+pos] = mixin(wavDataRight[i+pos], wavVolume * newRight[i]);
		}
	}

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

		int pos = millisToBytePos(posInMillis);

		int len = drumData.length;
		if (len + pos > songData.length) {
			len = songData.length - pos;
		}

		for (int i = 0; i < len; i++) {
			songData[pos + i] = mixin(songData[pos + i], drumData[i]);
		}
	}

	/**
	 * Takes a position in milliseconds and returns the exact offset into the byte array at which
	 * this time is occurring in the song data
	 */
	private int millisToBytePos(int posInMillis) {
		return (posInMillis * byteRate) / (1000 * bytesPerSample);
	}

	private int[] generateDrum() {

		// duration: 2 seconds
		int durationMillis = 2000;

		// frequency: 50 Hz
		double frequency = 50;

		// max is 8*16*16*16 - we take half that
		int amplitude = 4*16*16*16;

		int[] data = new int[millisToBytePos(durationMillis)];
		int bytesPerSecond = millisToBytePos(1000);

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
