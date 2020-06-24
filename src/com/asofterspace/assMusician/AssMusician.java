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
	public final static String VERSION_NUMBER = "0.0.1.7(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "27. May 2020 - 24. June 2020";

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
