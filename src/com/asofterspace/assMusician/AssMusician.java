/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.Utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;


public class AssMusician {

	public final static String PROGRAM_TITLE = "assMusician";
	public final static String VERSION_NUMBER = "0.0.2.5(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "27. May 2020 - 26. June 2020";

	/*
	Kalli asked for some details, so here they are:
	Squeeeeeee :D
	Soooo I did the Moya thing, where I developed everything myself - obviously
	So I have exactly two dependencies:
	* java (because I have written it in java and it needs a jdk to compile and run)
	* ffmpeg (because I want to be able to put in any video - mpg, mp4, avi, etc., and I use ffmpeg to extract the audio as wav file)
	And then everything from there is done with pure Moya-code!
	So first I had to write a Moya-code for loading a wav file
	And that is actually harder than expected (like there is a very simple "default" wav format, but the stuff that comes out of ffmpeg is in a more complicated format, where you have to iterate over property headers first and figure out where the data actually lies...)
	But yea, so I am doing all that
	And then I need to detect where the drums should go
	So I first wrote a very stupid algorithm - and it did not work well at all :D
	(It detected basically whenever something was a local maximum, after increasing for a few steps, and said "here a beat could be")
	Then I improved it by making a histogram of the differences between these maxima
	So always the position of maximum i minus the position of maximum i - 1
	And the difference that is in there the most often, that one I assume is the "correct" distance between beats
	Based on which I generate a number for bpm - beats per minute
	I then go from the front of the song through to the end, always in bpm steps, to add a drum sound there
	Buuut
	Then I noticed that these are not aligned to the actual maxima
	So I made it so that whenever it goes bpm steps forwards, it has a bit of wiggle room and if it finds a detected maximum, it aligns to that instead, having a slightly different value for bpm for that one beat
	But then it sounded really weird, so I added an extra smoothening to the very end, which goes over all the beats a few times and smoothens them in time, so that the real bpm is a bit more similar in a local neighbourhood
	So this is about the algorithm for the very first video I uploaded on youtube
	But then I noticed that I also want to not always play the same drums
	So for each beat, I iterate over all wave values between this an the next beat
	And I get two values: a loudness (relative to the loudness of the entire video - not just the maximum, but actually the integral)
	And second, a jitterieness
	(which is the value how often it switches from going-up to going-down, or the other way around)
	And based on these two, I made some cutoffs for no drums, a few drums, lots of drums, and lots and lots of drums
	This was already a bit better, but still sometimes the drums were very loud or very low
	So I then made it so the actual drum volume is relative to the loudness of the beat
	So if the song gets louder, the drums get louder :D
	And all that was already a little bit fun
	But then I had the idea to actually look at different frequencies (so far, it was all just with the amplitude from the wav file directly, no frequency stuff at all!)
	Sooooo I implemented my own Fourier analysis (of course I did :D )
	Buuut it is actually very slow, because I was too lazy to implement Fast Fourier Transform
	Now I looked at the low frequency values only, as input for the beats in the beginning, but kept the rest of the algorithm the same
	And now it took quite a long time to generate the song (due to the slow Fourier), and... it sounded horrible xD
	I actually have not put a single one of these videos on youtube, because they were all bad xD
	So a few days ago, I had the bestest idea:
	I got rid of the Fourier again (well, I still generate it for the video, but I don't use it in the audio algorithm anymore at all)
	Instead, I improved the pure amplitude beat detection by sliding a window of 500 ms, and checking if the current step is the maximum within this window
	And if it is, I generate a few statistics, e.g. how fast it goes up in front of the maximum, and how slow it goes down
	Assuming that a real "beat" should be going up fast, then be at a maximum, then going down slowly
	So I now have beats with qualities assigned to them
	Then I again generate a bpm
	But instead of aligning from the beginning of the song, I try to align starting at every single beat
	going backwards and forwards from it
	And for each possible assignment, I generate a score of how good it is
	The score is based on how many of the beats I "hit" while aligning, and on how high the quality of the ones that I hit is
	And then I take the best out of all the possible start values
	And I do this again for several slightly larger and slightly smaller bpms than the one I originally detected
	Aaaand again, I make it choose the one with the highest score
	Then, finally, yesterday I had the idea to also dynamically change the levels at which different drum patterns are generated
	So loudness and jitterieness, as before
	But now if I notice that I am putting too few drums, I set the threshold down, or if I notice that I am putting too many, I put the threshold up
	And then all is shiny :D
	(And then I generate the video... which is of course also 100% Moya code, even made my own Image class and my own Color class, and have my own Moya pixels which can do fun things, and they look soooo pretty and are so easy to use... squeee ^^)
	Aaaaand that is that! :D
	Now you know EVERYTHING :D
	*/
	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		System.out.println("Loading database...");

		Database database = new Database();

		System.out.println("Saving database...");

		database.save();

		Directory inputDir = new Directory("input");
		inputDir.create();

		Directory outputDir = new Directory("output");
		outputDir.create();

		String fontName = "neuropol.ttf";
		try {
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			File fontFile = new File("fonts/" + fontName);
			env.registerFont(Font.createFont(Font.TRUETYPE_FONT, fontFile.getJavaFile()));
		} catch (Exception e) {
			System.out.println("Could not load the font " + fontName + "!");
		}

		MusicGenerator musicGenny = new MusicGenerator(database, inputDir, outputDir);
		boolean recursively = false;

		for (File songFile : inputDir.getAllFiles(recursively)) {
			musicGenny.addDrumsToSong(songFile);
		}

		System.out.println("Done! Have a nice day! :)");
	}

}
