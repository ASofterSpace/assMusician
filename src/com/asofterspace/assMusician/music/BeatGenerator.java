/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.music;

import com.asofterspace.toolbox.music.Beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BeatGenerator {

	private List<String> debugLog;

	private List<Integer> beats;

	private int alignmentQuality;


	public BeatGenerator(List<String> debugLog) {
		this.debugLog = debugLog;
		debugLog.add("  :: beat generator created");
	}

	public int getAlignmentQuality() {
		return alignmentQuality;
	}

	public List<Integer> getBeats() {
		return beats;
	}

	public void generateBeatsFor(AbsMaxPos absMax, List<AbsMaxPos> absMaxPositions, int dataLength, int generatedBeatDistance,
		int uncertaintyFrontSetting, int uncertaintyBackSetting) {

		debugLog.add("  :: generating beats starting at " + absMax);

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
			if (maxPosI < 1) {
				insertedUnalignedBeats++;
			} else {
				if ((absMaxPositions.get(maxPosI).getOrigPosition() <= i + uncertaintyBack) &&
					(absMaxPositions.get(maxPosI).getOrigPosition() > i - uncertaintyFront)) {
					while ((maxPosI >= 0) &&
						(absMaxPositions.get(maxPosI).getOrigPosition() > i)) {
						maxPosI--;
					}
					if (maxPosI < 1) {
						insertedUnalignedBeats++;
					} else {
						AbsMaxPos alignTo = absMaxPositions.get(maxPosI);
						if (maxPosI > 0) {
							if (i - absMaxPositions.get(maxPosI).getOrigPosition() < absMaxPositions.get(maxPosI-1).getOrigPosition() - i) {
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

		debugLog.add("    ::: generated " + insertedAlignedBeats + " aligned beats");
		debugLog.add("    ::: generated " + insertedUnalignedBeats + " unaligned beats");
		debugLog.add("    ::: overall alignment quality: " + alignmentQuality);
	}
}
