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

	private Database database;

	private Directory inputDir;
	private Directory outputDir;
	private Directory workDir;

	private int byteRate;
	private int bytesPerSample;


	public MusicGenerator(Database database, Directory inputDir, Directory outputDir) {
		this.database = database;
		this.inputDir = inputDir;
		this.outputDir = outputDir;

		workDir = new Directory("work");
		workDir.create();
	}

	public void addDrumsToSong(File originalSong) {

		System.out.println("Adding drums to " + originalSong.getLocalFilename());

		String ffmpegPath = database.getRoot().getString("ffmpegPath");
		File workSong = new File(workDir, "song.wav");

		// extract the audio of the original song (e.g. in case the original is a music video)
		String ffmpegInvocation = ffmpegPath;
		ffmpegInvocation += " -i \"";
		ffmpegInvocation += originalSong.getAbsoluteFilename();
		// bit rate 192 kbps, sample rate 22050 Hz, audio channels: 2 (stereo), audio codec: PCM 16, no video
		ffmpegInvocation += "\" -ab 192000 -ar 22050 -ac 2 -acodec pcm_s16le -vn \"";
		ffmpegInvocation += workSong.getAbsoluteFilename();
		ffmpegInvocation += "\"";
		System.out.println("Executing " + ffmpegInvocation);
		IoUtils.execute(ffmpegInvocation);
		Utils.sleep(500);

		// load the extracted audio
		WavFile wav = new WavFile(workSong);
		this.byteRate = wav.getByteRate();
		this.bytesPerSample = wav.getBitsPerSample() / 8;
		int[] wavDataLeft = wav.getLeftData();
		int[] wavDataRight = wav.getRightData();
		/*
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < wavDataLeft.length; i++) {
			sb.append(""+wavDataLeft[i]);
			sb.append("\n");
		}
		TextFile outfile = new TextFile(workDir, "debugLeft.txt");
		outfile.saveContent(sb);
		*/

		// add drums

			// find the beat in the loaded song
			// TODO

			// determine which drums sounds to add where
			// TODO

			// actually put them into the song
			addDrum(wavDataLeft, 5000);
			addDrum(wavDataRight, 5000);

		// save the new song as audio
		wav.setLeftData(wavDataLeft);
		wav.setRightData(wavDataRight);
		wav.save();

		// generate a video for the song
		// TODO

		// splice the generated audio together with the generated video
		// TODO

		// upload it to youtube
		// TODO
	}

	private void addDrum(int[] songData, int posInMillis) {

		int[] drumData = generateDrum();

		int pos = millisToBytePos(posInMillis);

		int len = drumData.length;
		if (len + pos > songData.length) {
			len = songData.length - pos;
		}

		for (int i = 0; i < len; i++) {
			songData[pos + i] = drumData[i];
		}
	}

	/**
	 * Takes a position in milliseconds and returns the exact offset into the byte array at which
	 * this time is occurring in the song data
	 */
	private int millisToBytePos(int posInMillis) {
		return (posInMillis * byteRate) / bytesPerSample;
	}

	private int[] generateDrum() {

		// duration: 1 second
		int durationMillis = 1000;

		// frequency: 50 Hz
		double frequency = 50;

		// max is 8*16*16*16
		int amplitude = 8*16*16*16;

		int[] data = new int[millisToBytePos(durationMillis)];

		for (int i = 0; i < data.length; i++) {
			double inout = 1.0;
			if (i < data.length / 10) {
				inout = (10.0 * i) / data.length;
			}
			if (i > data.length / 2) {
				inout = 2 - ((2.0 * i) / data.length);
			}
			data[i] = (int) (inout * amplitude * Math.sin((6.2831853 * i) / (frequency * 1000)));
		}

		return data;
	}
}
