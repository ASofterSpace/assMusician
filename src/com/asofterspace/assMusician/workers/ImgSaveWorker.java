/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.workers;

import com.asofterspace.toolbox.images.DefaultImageFile;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.Utils;


public class ImgSaveWorker implements Runnable {

	private boolean keepRunning;

	private boolean currentlyWorking;

	private Image img;

	private String filename;


	public ImgSaveWorker() {

		this.keepRunning = true;

		this.currentlyWorking = false;
	}

	public void run() {

		boolean workedLastRound = true;

		while (keepRunning) {

			if (!workedLastRound) {
				Utils.sleep(1000);
			}

			workedLastRound = false;

			if (this.img != null) {

				currentlyWorking = true;
				workedLastRound = true;

				DefaultImageFile curImgFile = new DefaultImageFile(this.filename);
				curImgFile.assign(this.img);
				curImgFile.save();

				this.img = null;
				this.filename = null;
			}

			currentlyWorking = false;
		}
	}

	public void stop() {
		keepRunning = false;
	}

	public synchronized boolean isBusy() {
		if (currentlyWorking) {
			return true;
		}
		return (this.img != null) || currentlyWorking;
	}

	public synchronized void workOn(Image img, String filename) {
		this.img = img;
		this.filename = filename;
	}
}
