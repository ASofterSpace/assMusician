/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;


public class MusicGenerator {

	private Database database;

	private Directory inputDir;
	private Directory outputDir;
	private Directory workDir;


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
		// bit rate 192 kbps, sample rate 22050 Hz, audio channels: 1 (mono), audio codec: PCM 16, no video
		ffmpegInvocation += "\" -ab 192000 -ar 22050 -ac 1 -acodec pcm_s16le -vn \"";
		ffmpegInvocation += workSong.getAbsoluteFilename();
		ffmpegInvocation += "\"";
		System.out.println("Executing " + ffmpegInvocation);
		IoUtils.execute(ffmpegInvocation);

		// load the extracted audio
		// TODO

		// add drums

			// find the beat in the loaded song
			// TODO

			// determine which drums sounds to add where
			// TODO

			// actually put them into the song
			// TODO

		// save the new song as audio
		// TODO

		// generate a video for the song
		// TODO

		// splice the generated audio together with the generated video
		// TODO

		// upload it to youtube
		// TODO
	}
}
