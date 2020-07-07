/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.music;

import com.asofterspace.toolbox.sound.Beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class BeatGenerator {

	private List<String> debugLog;

	private List<Integer> beats;

	private int alignmentQuality;

	private Random rand;


	public BeatGenerator(List<String> debugLog) {
		this.debugLog = debugLog;
		debugLog.add("  :: beat generator created");
		rand = new Random();
	}

	public int getAlignmentQuality() {
		return alignmentQuality;
	}

	public List<Integer> getBeats() {
		return beats;
	}

	public void generateBeatsFor(AbsMaxPos absMax, List<AbsMaxPos> absMaxPositions, int dataLength, int generatedBeatDistance,
		int uncertaintyFrontSetting, int uncertaintyBackSetting) {

		boolean doLog = false;
		if (rand.nextInt(128) == 0) {
			doLog = true;
		}

		if (doLog) {
			debugLog.add("  :: generating beats starting at " + absMax);
		}

		beats = new ArrayList<>();

		// add the beat that we are starting the alignment with
		beats.add(absMax.getOrigPosition());
		alignmentQuality = (int) absMax.getQuality();
		int insertedAlignedBeats = 1;
		int insertedUnalignedBeats = 0;

		// add beats from here to the front
		int maxPosI = absMaxPositions.size() - 1;
		for (int i = absMax.getOrigPosition() - generatedBeatDistance; i >= 0; i -= generatedBeatDistance) {

			int uncertaintyFront = (generatedBeatDistance * uncertaintyFrontSetting) / 10;
			int uncertaintyBack = (generatedBeatDistance * uncertaintyBackSetting) / 10;

			while ((maxPosI >= 0) &&
				(absMaxPositions.get(maxPosI).getOrigPosition() > i + uncertaintyBack)) {
				maxPosI--;
			}
			if (maxPosI < 0) {
				insertedUnalignedBeats++;
			} else {
				if ((absMaxPositions.get(maxPosI).getOrigPosition() <= i + uncertaintyBack) &&
					(absMaxPositions.get(maxPosI).getOrigPosition() > i - uncertaintyFront)) {
					while ((maxPosI >= 0) &&
						(absMaxPositions.get(maxPosI).getOrigPosition() > i)) {
						maxPosI--;
					}
					if (maxPosI < 0) {
						insertedUnalignedBeats++;
					} else {
						AbsMaxPos alignTo = absMaxPositions.get(maxPosI);
						if (maxPosI + 1 < absMaxPositions.size()) {
							if (absMaxPositions.get(maxPosI + 1).getOrigPosition() - i < i - absMaxPositions.get(maxPosI).getOrigPosition()) {
								alignTo = absMaxPositions.get(maxPosI+1);
							} else {
								alignTo = absMaxPositions.get(maxPosI);
							}
						}
						i = alignTo.getOrigPosition();
						insertedAlignedBeats++;
						alignmentQuality += (int) alignTo.getQuality();
					}
				} else {
					insertedUnalignedBeats++;
				}
			}
			beats.add(i);
		}

		// add beats from here to the back
		maxPosI = 0;
		for (int i = absMax.getOrigPosition() + generatedBeatDistance; i < dataLength; i += generatedBeatDistance) {

			int uncertaintyFront = (generatedBeatDistance * uncertaintyFrontSetting) / 10;
			int uncertaintyBack = (generatedBeatDistance * uncertaintyBackSetting) / 10;

			while ((maxPosI < absMaxPositions.size()) &&
				(absMaxPositions.get(maxPosI).getOrigPosition() < i - uncertaintyFront)) {
				maxPosI++;
			}
			if (maxPosI >= absMaxPositions.size()) {
				insertedUnalignedBeats++;
			} else {
				if ((absMaxPositions.get(maxPosI).getOrigPosition() >= i - uncertaintyFront) &&
					(absMaxPositions.get(maxPosI).getOrigPosition() < i + uncertaintyBack)) {
					while ((maxPosI < absMaxPositions.size()) &&
						(absMaxPositions.get(maxPosI).getOrigPosition() < i)) {
						maxPosI++;
					}
					if (maxPosI >= absMaxPositions.size()) {
						insertedUnalignedBeats++;
					} else {
						AbsMaxPos alignTo = absMaxPositions.get(maxPosI);
						if (maxPosI > 0) {
							if (absMaxPositions.get(maxPosI).getOrigPosition() - i < i - absMaxPositions.get(maxPosI-1).getOrigPosition()) {
								alignTo = absMaxPositions.get(maxPosI);
							} else {
								alignTo = absMaxPositions.get(maxPosI-1);
							}
						}
						i = alignTo.getOrigPosition();
						insertedAlignedBeats++;
						alignmentQuality += (int) alignTo.getQuality();
					}
				} else {
					insertedUnalignedBeats++;
				}
			}
			beats.add(i);
		}

		Collections.sort(beats);

		if (doLog) {
			debugLog.add("    ::: generated " + insertedAlignedBeats + " aligned beats");
			debugLog.add("    ::: generated " + insertedUnalignedBeats + " unaligned beats");
			debugLog.add("    ::: overall alignment quality: " + alignmentQuality);
		}
	}
}
