/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.workers;

import com.asofterspace.toolbox.sound.SoundData;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.List;


public class FourierWorker implements Runnable {

	private boolean keepRunning;

	private SoundData soundData;

	private List<FourierInstruction> instructions;

	private List<FourierResult> results;

	private int fourierMax;

	private boolean currentlyWorking;


	public FourierWorker(SoundData soundData) {

		this.soundData = soundData;

		this.instructions = new ArrayList<>();

		this.results = new ArrayList<>();

		this.fourierMax = 0;

		this.keepRunning = true;

		this.currentlyWorking = false;
	}

	public void run() {

		boolean workedLastRound = true;

		while (keepRunning) {

			if (!workedLastRound) {
				Utils.sleep(1000);
			}

			List<FourierInstruction> myInstructions = new ArrayList<>();

			synchronized (this.instructions) {
				if (this.instructions.size() > 0) {
					currentlyWorking = true;
				}
				myInstructions.addAll(this.instructions);
				this.instructions.clear();
			}

			workedLastRound = myInstructions.size() > 0;

			for (FourierInstruction inst : myInstructions) {

				int fourierNum = inst.getFourierNum();
				int fourierLen = inst.getFourierLen();

				int[] fourier = soundData.getSmallFourier(fourierNum*fourierLen, (fourierNum+1)*fourierLen);

				for (int k = 0; k < fourier.length / 2; k++) {
					if (fourier[k] > fourierMax) {
						fourierMax = fourier[k];
					}
				}

				results.add(new FourierResult(fourierNum, fourierLen, fourier));
			}

			currentlyWorking = false;
		}
	}

	public void stop() {
		keepRunning = false;
	}

	public boolean isBusy() {
		if (currentlyWorking) {
			return true;
		}
		synchronized (this.instructions) {
			return (this.instructions.size() > 0) || currentlyWorking;
		}
	}

	public void workOn(List<FourierInstruction> instructions) {
		synchronized (this.instructions) {
			this.instructions.addAll(instructions);
		}
	}

	public List<FourierResult> getResults() {
		return results;
	}

	public int getFourierMax() {
		return fourierMax;
	}
}
