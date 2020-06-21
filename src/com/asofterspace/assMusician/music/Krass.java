/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.music;



/**
 * Just a bunch of algorithms that didn't make the cut
 */
public class Krass {

		/*
		// ALGORITHM 1

		// actually put them into the song
		addDrum(wavDataLeft, 3000);
		addDrum(wavDataRight, 3000);
		*/

		/*
		// ALGORITHM 2

		// find the beat in the loaded song - by sliding a window of several samples,
		// and only when all samples within the window are increasing, calling it
		// a maximum
		int windowLength = 32;
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
			}
		}
		*/

/*

		// ALGORITHM 3

		debugOut.add(": Starting Algorithm 3");

		// We first iterate over the entire song and try to find maxima, by finding
		// first the maxima over each 0.025 s region, and then we go over all the maxima
		// from end to start and keep only the ones which are preceded by lower ones
		// For this (for now) we only use the left channel...
		List<Integer> maximumPositions = new ArrayList<>();
		List<Integer> potentialMaximumPositions = new ArrayList<>();
		int localMaxPos = 0;
		int localMax = 0;
		int regionSizeMS = 25;
		int regionSize = millisToChannelPos(regionSizeMS);
		debugOut.add("  :: region size: " + regionSizeMS + " ms");
		debugOut.add("  :: region size: " + regionSize + " pos");

		for (int i = 0; i < wavDataLeft.length; i++) {
			if (wavDataLeft[i] > localMax) {
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
			if (i % regionSize == 0) {
				potentialMaximumPositions.add(localMaxPos);
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
		}

		debugOut.add("  :: " + potentialMaximumPositions.size() + " potential maximum positions found");

		Map<Integer, Integer> moreLikelyMaximumPositions = new HashMap<>();
		for (int i = potentialMaximumPositions.size() - 1; i > 2; i--) {

				// if the previous maximum is smaller
			if ((wavDataLeft[potentialMaximumPositions.get(i-1)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-2)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-3)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and this maximum is above volume 1/8
				(wavDataLeft[potentialMaximumPositions.get(i)] > 16*16*16)) {

				// then we actually fully accept it as maximum :)
				moreLikelyMaximumPositions.put(potentialMaximumPositions.get(i),
					(3*wavDataLeft[potentialMaximumPositions.get(i)])-(wavDataLeft[potentialMaximumPositions.get(i-1)]+
					wavDataLeft[potentialMaximumPositions.get(i-2)]+wavDataLeft[potentialMaximumPositions.get(i-3)]));
			}
		}

		debugOut.add("  :: " + moreLikelyMaximumPositions.size() + " more likely maximum positions found");

		// we now iterate once more, getting the highest / most maximum-y of the maxima
		for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos(100)) {
			int highestPos = -1;
			int highestAmount = -1;
			for (int k = i; k < i + millisToChannelPos(100); k++) {
				Integer val = moreLikelyMaximumPositions.get(k);
				if (val != null) {
					if (val > highestAmount) {
						highestAmount = val;
						highestPos = k;
					}
				}
			}
			if (highestPos >= 0) {
				maximumPositions.add(highestPos);
			}
		}

		debugOut.add("  :: " + maximumPositions.size() + " most maximum-y maxima found");

		Collections.sort(maximumPositions);
*/






		/*
		List<Integer> maximumPositions = new ArrayList<>();
		List<Integer> potentialMaximumPositions = new ArrayList<>();
		int localMaxPos = 0;
		int localMax = 0;
		int regionSizeMS = 25;
		int regionSize = millisToChannelPos(regionSizeMS);
		debugOut.add("  :: region size: " + regionSizeMS + " ms");
		debugOut.add("  :: region size: " + regionSize + " pos");

		for (int i = 0; i < wavDataLeft.length; i++) {
			if (wavDataLeft[i] > localMax) {
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
			if (i % regionSize == 0) {
				potentialMaximumPositions.add(localMaxPos);
				localMax = wavDataLeft[i];
				localMaxPos = i;
			}
		}

		debugOut.add("  :: " + potentialMaximumPositions.size() + " potential maximum positions found");

		Map<Integer, Integer> moreLikelyMaximumPositions = new HashMap<>();
		for (int i = potentialMaximumPositions.size() - 1; i > 2; i--) {

				// if the previous maximum is smaller
			if ((wavDataLeft[potentialMaximumPositions.get(i-1)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-2)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and the previous-previous-previous maximum is smaller
				(wavDataLeft[potentialMaximumPositions.get(i-3)] < wavDataLeft[potentialMaximumPositions.get(i)]) &&
				// and this maximum is above volume 1/8
				(wavDataLeft[potentialMaximumPositions.get(i)] > 16*16*16)) {

				// then we actually fully accept it as maximum :)
				moreLikelyMaximumPositions.put(potentialMaximumPositions.get(i),
					(3*wavDataLeft[potentialMaximumPositions.get(i)])-(wavDataLeft[potentialMaximumPositions.get(i-1)]+
					wavDataLeft[potentialMaximumPositions.get(i-2)]+wavDataLeft[potentialMaximumPositions.get(i-3)]));
			}
		}

		debugOut.add("  :: " + moreLikelyMaximumPositions.size() + " more likely maximum positions found");

		// we now iterate once more, getting the highest / most maximum-y of the maxima
		for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos(100)) {
			int highestPos = -1;
			int highestAmount = -1;
			for (int k = i; k < i + millisToChannelPos(100); k++) {
				Integer val = moreLikelyMaximumPositions.get(k);
				if (val != null) {
					if (val > highestAmount) {
						highestAmount = val;
						highestPos = k;
					}
				}
			}
			if (highestPos >= 0) {
				maximumPositions.add(highestPos);
			}
		}

		debugOut.add("  :: " + maximumPositions.size() + " most maximum-y maxima found");
		*/





/*
		// now iterate over all the found maximum positions, and whenever the distance between some is small-ish
		// (let's say 0.1s), we mush them together into one
		int smooshSize = millisToChannelPos(100);
		List<Integer> smooshedMaximumPositions = new ArrayList<>();

		for (int i = maximumPositions.size() - 1; i >= 0; i--) {
			int j = i - 1;
			int smooshedVal = maximumPositions.get(i);
			while (j >= 0) {
				if (maximumPositions.get(i) - maximumPositions.get(j) < smooshSize) {
					smooshedVal += maximumPositions.get(j);
					j--;
				} else {
					break;
				}
			}
			int amountSmooshed = i - j;
			j++;
			i = j;
			smooshedMaximumPositions.add(smooshedVal / amountSmooshed);
		}

		maximumPositions = smooshedMaximumPositions;
*/

//		Collections.sort(maximumPositions);








/*
		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm

		debugOut.add("  :: smoothening the buckets");
		int SMOOTHENING_WIDTH = 1500;
		debugOut.add("  :: smoothening width: " + SMOOTHENING_WIDTH);

		Map<Integer, Integer> smoothBpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			int curAmount = 0;
			for (int i = 1; i < SMOOTHENING_WIDTH; i++) {
				if (bpmCandidates.get(curBpm-i) != null) {
					curAmount += bpmCandidates.get(curBpm-i);
				}
				if (bpmCandidates.get(curBpm+i) != null) {
					curAmount += bpmCandidates.get(curBpm+i);
				}
			}
			if (bpmCandidates.get(curBpm) != null) {
				curAmount += bpmCandidates.get(curBpm) * 2;
			}
			smoothBpmCandidates.put(curBpm, curAmount);
		}
		bpmCandidates = smoothBpmCandidates;

		// output smoothened buckets as histogram
		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_histogram_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");
*/





/*
		// ALGORITHM 4

		debugOut.add(": Starting Algorithm 4");

		// Instead of keeping the maximumPositions which we had so far, we use new ones which we
		// base on the Fourier transform and the lowest frequencies we get from it...
		// to do so, we split the entire song into pieces the size of two beats, and take the highest
		// Fourier of the lowest frequency as beat to align within that two-beat window!
		// (However, we only take the louder half of these to not get too much noise)

		maximumPositions = new ArrayList<>();

		// we could just use the highest Fouriers directly:
		//for (int i = 0; i < fouriers.length; i++) {
		//	if (fouriers[i][fouriers[i].length - 1] > 4*3*5000) {
		//		maximumPositions.add(i * fourierLen);
		//	}
		//}

		debugOut.add("  :: using " + fouriers.length + " Fourier levels of size " + fouriers[0].length);

		List<Pair<Integer, Integer>> possibleMaximumPositions = new ArrayList<>();
		List<Integer> allMaximumPositionsForAlignment = new ArrayList<>();
		int lastStart = 0;
		debugOut.add("  :: using generated beat distance " + generatedBeatDistance + " pos");
		debugOut.add("  :: using Fourier length " + fourierLen + " pos");
		debugOut.add("  :: resulting resolution: " + (generatedBeatDistance / fourierLen) + " (higher is better)");
		for (int i = generatedBeatDistance; i < wavDataLeft.length; i += generatedBeatDistance) {
			int maxVal = 0;
			int maxPos = 0;
			for (int j = lastStart; j < i; j += fourierLen) {
				int f = (j / fourierLen);
				// go for fouriers[f].length - 1 as second index such that we get the one with the lowest frequency
				if (fouriers[f][fouriers[f].length - 1] > maxVal) {
					maxPos = j;
					maxVal = fouriers[f][fouriers[f].length - 1];
				}
			}
			possibleMaximumPositions.add(new Pair<Integer, Integer>(maxPos, maxVal));
			allMaximumPositionsForAlignment.add(maxPos);
			lastStart = i;
		}

		debugOut.add("  :: found " + possibleMaximumPositions.size() + " possible maximum positions");

		Collections.sort(possibleMaximumPositions, new Comparator<Pair<Integer, Integer>>() {
			public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
				return a.getRight() - b.getRight();
			}
		});

		int midVal = possibleMaximumPositions.get(possibleMaximumPositions.size() / 2).getRight();

		debugOut.add("  :: mid val: " + midVal);

		// only get the louder half of them for now (for bpm detection)
		for (int i = 0; i < possibleMaximumPositions.size(); i++) {
			if (possibleMaximumPositions.get(i).getRight() >= midVal) {
				maximumPositions.add(possibleMaximumPositions.get(i).getLeft());
			}
		}

		debugOut.add("  :: louder half of maximum positions containing " + maximumPositions.size() + " values");

		Collections.sort(maximumPositions);

		// output new file containing these new positions

		GraphImage graphWithFourierImg = new GraphImage();
		graphWithFourierImg.setInnerWidthAndHeight(channelPosToMillis(wavDataLeft.length) / 100, graphImageHeight);

		List<GraphDataPoint> wavData = new ArrayList<>();
		int position = 0;
		for (Integer wavInt : wavDataLeft) {
			wavData.add(new GraphDataPoint(position, wavInt));
			position++;
		}
		graphWithFourierImg.setDataColor(new ColorRGB(0, 0, 255));
		graphWithFourierImg.setAbsoluteDataPoints(wavData);

		for (Integer pos : maximumPositions) {
			graphWithFourierImg.drawVerticalLineAt(pos, new ColorRGB(255, 0, 128));
		}

		DefaultImageFile wavFourierImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_beats.png");
		wavFourierImgFile.assign(graphWithFourierImg);
		wavFourierImgFile.save();


		// ALGORITHM 4.1

		debugOut.add(": Starting Algorithm 4.1");

		// now do beat detection AGAIN - this time, based on the fourier-based maxima
		// TODO :: algo 4.1 has been replaced by algo 4.2, stop calculating it!

		bpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			bpmCandidates.put(curBpm, 0);
		}

		candFound = 0;
		candDistinct = 0;

		for (int i = 0; i < maximumPositions.size(); i++) {
			// actually, instead of looking at all pairs...
			// for (int j = 0; j < i; j++) {
			// we just want to look at the closest other beat, having a lookback of just 1
			// (after looking at a histogram of the bpm for different lookback values)
			for (int j = i - LOOKBACK; j < i; j++) {
				if (j < 0) {
					continue;
				}
				int curDist = maximumPositions.get(i) - maximumPositions.get(j);
				int curDiffInMs = channelPosToMillis(10*curDist);

				// a difference of 1 ms means that there are 60*1000 beats per minute,
				// and we have the additional *10 to get more accuracy
				if (curDiffInMs == 0) {
					continue;
				}
				int curBpm = (60*1000*BUCKET_ACCURACY_FACTOR*10) / curDiffInMs;

				// scale the bpm into the range that we are interested in
				while (curBpm < MIN_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm *= 2;
				}
				while (curBpm > MAX_BPM*1000*BUCKET_ACCURACY_FACTOR) {
					curBpm /= 2;
				}

				if (bpmCandidates.get(curBpm) != null) {
					if (bpmCandidates.get(curBpm) == 0) {
						candDistinct++;
					}
					bpmCandidates.put(curBpm, bpmCandidates.get(curBpm) + 1);
				} else {
					bpmCandidates.put(curBpm, 1);
					candDistinct++;
				}
				candFound++;
			}
		}

		debugOut.add("  :: " + candFound + " bpm candidates found overall");
		debugOut.add("  :: " + candDistinct + " distinct bpm candidates found");

		// output buckets as histogram
		graphWidth = (MAX_BPM - MIN_BPM) * BUCKET_ACCURACY_FACTOR * 10;
		histogramImg = new GraphImage();
		histogramImg.setInnerWidthAndHeight(graphWidth, graphImageHeight);

		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_bpm.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm

		debugOut.add("  :: smoothening the buckets");
		SMOOTHENING_WIDTH = 1500;
		debugOut.add("  :: smoothening width: " + SMOOTHENING_WIDTH);

		smoothBpmCandidates = new HashMap<>();
		for (int curBpm = MIN_BPM*1000*BUCKET_ACCURACY_FACTOR; curBpm < MAX_BPM*1000*BUCKET_ACCURACY_FACTOR + 1; curBpm++) {
			int curAmount = 0;
			for (int i = 1; i < SMOOTHENING_WIDTH; i++) {
				if (bpmCandidates.get(curBpm-i) != null) {
					curAmount += bpmCandidates.get(curBpm-i);
				}
				if (bpmCandidates.get(curBpm+i) != null) {
					curAmount += bpmCandidates.get(curBpm+i);
				}
			}
			if (bpmCandidates.get(curBpm) != null) {
				curAmount += bpmCandidates.get(curBpm) * 2;
			}
			smoothBpmCandidates.put(curBpm, curAmount);
		}
		bpmCandidates = smoothBpmCandidates;

		// output smoothened buckets as histogram
		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_bpm_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		largestBucketBpm = 0;
		bucketAmount = 0;

		for (Map.Entry<Integer, Integer> entry : bpmCandidates.entrySet()) {
			bucketAmount++;
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBpm = entry.getKey();
			}
		}

		bpm = largestBucketBpm / (BUCKET_ACCURACY_FACTOR*1000.0);

		debugOut.add("  :: " + bucketAmount + " buckets used");
		debugOut.add("  :: largest bucket containing " + largestBucketContentAmount + " values");
		debugOut.add("  :: largest bucket value: " + largestBucketBpm);
		debugOut.add("  :: bpm based on largest bucket: " + bpm);
		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

		System.out.println("We detected " + bpm + " beats per minute based on the Fourier beats, " +
			"with the largest bucket containing " + largestBucketContentAmount + " values...");


		// ALGORITHM 4.2

		debugOut.add(": Starting Algorithm 4.2");

		// now do beat detection AGAIN - this time, based on the fourier-based maxima, and with a different
		// algorithm - instead of putting bpms into buckets, we want to look at the histogram of all the
		// one-beat distances and see if we can find some truth in that...

		int maxDist = 0;
		int minDist = Integer.MAX_VALUE;
		List<Integer> distances = new ArrayList<>();
		System.out.println("\n\nDistances unsorted:\n");
		for (int i = 1; i < maximumPositions.size(); i++) {
			int curDist = maximumPositions.get(i) - maximumPositions.get(i - 1);
			if (curDist > maxDist) {
				maxDist = curDist;
			}
			if (curDist < minDist) {
				minDist = curDist;
			}
			distances.add(curDist);
			System.out.println("  "+ curDist + " from " + maximumPositions.get(i) + " to " + maximumPositions.get(i-1));
		}

		debugOut.add("  :: " + distances.size() + " distances calculated");
		debugOut.add("  :: max dist: " + maxDist);
		debugOut.add("  :: min dist: " + minDist);
		System.out.println("\n\n");

		Collections.sort(distances);
		System.out.println("\n\nDistances sorted:\n");
		for (Integer i : distances) {
			System.out.println("  "+i);
		}
		System.out.println("\n\n");

		Map<Integer, Integer> beatLenCandidates = new HashMap<>();
		for (int i = 1; i <= maxDist; i++) {
			beatLenCandidates.put(i, 0);
		}

		int beatLenAmount = 0;
		int beatLenDistinct = 0;
		for (int i = 1; i < maximumPositions.size(); i++) {
			int curDist = maximumPositions.get(i) - maximumPositions.get(i - 1);
			if (beatLenCandidates.get(curDist) != null) {
				beatLenCandidates.put(curDist, beatLenCandidates.get(curDist) + 1);
			} else {
				beatLenCandidates.put(curDist, 1);
				beatLenDistinct++;
			}
			beatLenAmount++;
		}
		debugOut.add("  :: calculated " + beatLenDistinct + " distinct best length candidates");
		debugOut.add("  :: calculated " + beatLenAmount + " best length candidates overall");

		// output buckets as histogram
		graphWidth = maxDist;
		histogramImg = new GraphImage();
		histogramImg.setInnerWidthAndHeight(graphWidth / 20, graphImageHeight);

		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		debugOut.add("  :: obtained " + beatLenCandidates.entrySet().size() + " buckets");
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_len_beats.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// smoothen the buckets a little bit - we do not lose accuracy (as we do not just widen
		// the buckets into less precise ones), but we gain resistance to small variations in bpm
		Map<Integer, Integer> smoothBeatLenCandidates = new HashMap<>();
		for (int curLen = 1; curLen <= maxDist; curLen++) {
			smoothBeatLenCandidates.put(curLen, 0);
		}
		int smoothenBy = 4096;
		debugOut.add("  :: smoothening the buckets by " + smoothenBy);
		for (int curLen = 1; curLen <= maxDist; curLen++) {
			if (beatLenCandidates.get(curLen) != null) {
				if (beatLenCandidates.get(curLen) > 0) {
					for (int i = 0; i < smoothenBy; i++) {
						if (smoothBeatLenCandidates.get(curLen+i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen+i);
							cur += (smoothenBy - i) * beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen+i, cur);
						}
						if (i == 0) {
							continue;
						}
						if (smoothBeatLenCandidates.get(curLen-i) != null) {
							int cur = smoothBeatLenCandidates.get(curLen-i);
							cur += (smoothenBy - i) * beatLenCandidates.get(curLen);
							smoothBeatLenCandidates.put(curLen-i, cur);
						}
					}
				}
			}
		}
		beatLenCandidates = smoothBeatLenCandidates;

		// output smoothened buckets as histogram
		histData = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			histData.add(new GraphDataPoint(entry.getKey(), entry.getValue()));
		}
		histogramImg.setDataColor(new ColorRGB(0, 0, 255));
		histogramImg.setAbsoluteDataPoints(histData);
		histogramImgFile = new DefaultImageFile(workDir, "waveform_drum_beat_plus_fourier_histogram_for_len_beats_smoothened.png");
		histogramImgFile.assign(histogramImg);
		histogramImgFile.save();

		// now find the largest bucket
		largestBucketContentAmount = 0;
		int largestBucketBeatLen = 0;

		for (Map.Entry<Integer, Integer> entry : beatLenCandidates.entrySet()) {
			if (entry.getValue() >= largestBucketContentAmount) {
				largestBucketContentAmount = entry.getValue();
				largestBucketBeatLen = entry.getKey();
			}
		}

		debugOut.add("  :: " + largestBucketContentAmount + " values in the largest bucket");
		debugOut.add("  :: beat length based on largest bucket: " + largestBucketBeatLen + " pos");

		int largestBucketBeatLenInMS = channelPosToMillis(largestBucketBeatLen);
		debugOut.add("  :: beat length based on largest bucket: " + largestBucketBeatLenInMS + " ms");

		if (largestBucketBeatLenInMS != 0) {
			bpm = 1000 * 60.0 / largestBucketBeatLenInMS;
			debugOut.add("  :: bpm: " + bpm);

			System.out.println("We detected " + bpm + " beats per minute based on the Fourier beats length histogram, " +
				"with the largest bucket containing " + largestBucketContentAmount + " values...");
		} else {
			System.out.println("Falling back on " + bpm + " beats per minute due to a division by zero!");
			debugOut.add("  :: division by zero - fallback bpm: " + bpm);
		}

		generatedBeatDistance = millisToChannelPos((long) ((1000*60) / bpm));
		debugOut.add("  :: generated beat distance: " + generatedBeatDistance + " pos");

*/


		// generate beats based on the detected bpm, but still try to locally align to the closest
		// detected beat, e.g. align to the next one to the right if there is one to the left or
		// right of the current beat and the distance to it is less than 1/10 of a beat...
	//	List<Beat> mayBeats = new ArrayList<>();
		/*for (int i = 0; i < wavDataLeft.length; i += millisToChannelPos((long) ((1000*60) / bpm))) {
			bpmBasedBeats.add(i);
			wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
		}*/
/*
		int maxPosI = 0;
		// such that we do not have to worry about overflowing the maximumPositions list,
		// we just add an extra beat far after everything else
		// maximumPositions = allMaximumPositionsForAlignment;
		Collections.sort(maximumPositions);
		maximumPositions.add(wavDataLeft.length * 10);

*/




	/*
		// ALGORITHM 5

		debugOut.add(": Starting Algorithm 5");

		// in addition to all else, here keep a rolling average of the last 10 beat distances that we actually
		// put into the song, and use as predictor for the next beat distances the average of that and the
		// generated distance rather than just the generated distance purely
		int ACTUAL_GEN_RING_SIZE = 10;
		debugOut.add("  :: generator ring size: " + ACTUAL_GEN_RING_SIZE);

		int[] actualGeneratedDistances = new int[ACTUAL_GEN_RING_SIZE];
		int actualGeneratedI = 0;
		for (int i = 0; i < ACTUAL_GEN_RING_SIZE; i++) {
			actualGeneratedDistances[i] = generatedBeatDistance;
		}

		int prevI = -generatedBeatDistance;
		int origGeneratedBeatDistance = generatedBeatDistance;

		System.out.println("");

		int uncertaintyFrontSetting = 4;
		int uncertaintyBackSetting = 5;
		int uncertaintyBackInsertBeatSetting = 10;
		debugOut.add("  :: uncertainty front setting: " + uncertaintyFrontSetting + " / 10");
		debugOut.add("  :: uncertainty back setting: " + uncertaintyBackSetting + " / 10");
		debugOut.add("  :: uncertainty back insert beat setting: " + uncertaintyBackInsertBeatSetting + " / 10");

		debugOut.add("  :: orig generated beat distance: " + origGeneratedBeatDistance + " pos");

		int insertedMidAlignedBeats = 0;
		int insertedAlignedBeats = 0;
		int insertedUnalignedBeats = 0;

		for (int i = 0; i < wavDataLeft.length; i += generatedBeatDistance) {

			// regular alignment: 20% to the front, 20% to the back will be aligned, 60% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 5;
			// semi-aggressive alignment: 40% to the front, 50% to the back will be aligned, 50% to 100% to the back we align but add a beat
			int uncertaintyFront = (generatedBeatDistance * uncertaintyFrontSetting) / 10;
			int uncertaintyBack = (generatedBeatDistance * uncertaintyBackSetting) / 10;
			int uncertaintyBackInsertBeat = (generatedBeatDistance * uncertaintyBackInsertBeatSetting) / 10;
			// aggressive alignment: 50% to the front, 50% to the back will be aligned, 0% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 2;

			while (maximumPositions.get(maxPosI) < i - uncertaintyFront) {
				maxPosI++;
			}
			Beat mayBeat = new Beat(i);
			if (maximumPositions.get(maxPosI) < i + uncertaintyBackInsertBeat) {
				while (maximumPositions.get(maxPosI) < i) {
					maxPosI++;
				}
				int newI = i;
				if (maxPosI < 1) {
					newI = maximumPositions.get(maxPosI);
				} else {
					if (maximumPositions.get(maxPosI) - i < i - maximumPositions.get(maxPosI-1)) {
						newI = maximumPositions.get(maxPosI);
					} else {
						newI = maximumPositions.get(maxPosI-1);
					}
				}
				// if we are over the regular uncertainty towards the back, add an extra beat halfway in between
				if (newI > i + uncertaintyBack) {
					int extraI = (newI + (i - generatedBeatDistance)) / 2;
					Beat extraBeat = new Beat(extraI);
					extraBeat.setIsAligned(false);
					mayBeats.add(extraBeat);
					wavGraphImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
					// graphWithFourierImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));

					actualGeneratedDistances[actualGeneratedI] = extraI - prevI;
					prevI = extraI;
					actualGeneratedI++;
					if (actualGeneratedI >= ACTUAL_GEN_RING_SIZE) {
						actualGeneratedI = 0;
					}
					insertedMidAlignedBeats++;
				}
				i = newI;
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				// graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				mayBeat.setPosition(i);
				mayBeat.setIsAligned(true);
				insertedAlignedBeats++;
			} else {
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				// graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				mayBeat.setIsAligned(false);
				insertedUnalignedBeats++;
			}
			mayBeats.add(mayBeat);

			actualGeneratedDistances[actualGeneratedI] = i - prevI;
			actualGeneratedI++;
			if (actualGeneratedI >= ACTUAL_GEN_RING_SIZE) {
				actualGeneratedI = 0;
			}
			generatedBeatDistance = 0;
			for (int g = 0; g < ACTUAL_GEN_RING_SIZE; g++) {
				generatedBeatDistance += actualGeneratedDistances[g];
			}
			generatedBeatDistance = generatedBeatDistance / ACTUAL_GEN_RING_SIZE;
			generatedBeatDistance = (origGeneratedBeatDistance + generatedBeatDistance) / 2;

			// DEBUG - prevent speedup or speeddown
			generatedBeatDistance = origGeneratedBeatDistance;

			System.out.println("generatedBeatDistance: " + generatedBeatDistance + " orig: " + origGeneratedBeatDistance);
			if (i % (wavDataLeft.length / 32) == 0) {
				debugOut.add("    ::: [" + i + "] generated beat distance: " + generatedBeatDistance + " pos");
			}
			prevI = i;
		}
		System.out.println("");

		debugOut.add("  :: generated " + insertedAlignedBeats + " aligned beats");
		debugOut.add("  :: generated " + insertedMidAlignedBeats + " mid aligned beats");
		debugOut.add("  :: generated " + insertedUnalignedBeats + " unaligned beats");
	*/












		/*
		for (int i = 0; i < wavDataLeft.length; i += generatedBeatDistance) {

			// regular alignment: 20% to the front, 20% to the back will be aligned, 60% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 5;
			// semi-aggressive alignment: 40% to the front, 50% to the back will be aligned, 50% to 100% to the back we align but add a beat
			int uncertaintyFront = (generatedBeatDistance * 4) / 10;
			int uncertaintyBack = (generatedBeatDistance * 5) / 10;
			int uncertaintyBackInsertBeat = (generatedBeatDistance * 10) / 10;
			// aggressive alignment: 50% to the front, 50% to the back will be aligned, 0% of a beat would be unaligned
			// int uncertainty = generatedBeatDistance / 2;

			while (maximumPositions.get(maxPosI) < i - uncertaintyFront) {
				maxPosI++;
			}
			Beat mayBeat = new Beat(i);
			if (maximumPositions.get(maxPosI) < i + uncertaintyBackInsertBeat) {
				while (maximumPositions.get(maxPosI) < i) {
					maxPosI++;
				}
				int newI = i;
				if (maxPosI < 1) {
					newI = maximumPositions.get(maxPosI);
				} else {
					if (maximumPositions.get(maxPosI) - i < i - maximumPositions.get(maxPosI-1)) {
						newI = maximumPositions.get(maxPosI);
					} else {
						newI = maximumPositions.get(maxPosI-1);
					}
				}
				// if we are over the regular uncertainty towards the back, add an extra beat halfway in between
				if (newI > i + uncertaintyBack) {
					int extraI = (newI + (i - generatedBeatDistance)) / 2;
					Beat extraBeat = new Beat(extraI);
					extraBeat.setIsAligned(false);
					mayBeats.add(extraBeat);
					wavGraphImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
					graphWithFourierImg.drawVerticalLineAt(extraI, new ColorRGB(128, 196, 0));
				}
				i = newI;
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 255, 0));
				mayBeat.setPosition(i);
				mayBeat.setIsAligned(true);
			} else {
				wavGraphImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				graphWithFourierImg.drawVerticalLineAt(i, new ColorRGB(128, 128, 0));
				mayBeat.setIsAligned(false);
			}
			mayBeats.add(mayBeat);
		}
		*/

		/*
		DefaultImageFile wavImgFileFourier = new DefaultImageFile(workDir, "waveform_drum_extra_beat_addition_fourier_before_smoothen.png");
		wavImgFileFourier.assign(graphWithFourierImg);
		wavImgFileFourier.save();
		*/











		/*

		// ALGORITHM 3.8

		debugOut.add(": Starting Algorithm 3.8");

		// Aaaaand - you thought we were done, huh? :D - we continue... now we are looking at this:
		//  detected beats: |  |  |
		// generated beats: | | | |
		// so the first and last are aligned, but in the middle it would actually fit quite nicely...
		// if we had one less being generated, and they were put to equi-distance - so let's!

		List<Integer> bpmBasedBeats = new ArrayList<>();

		for (int i = 0; i < mayBeats.size(); i++) {
			Beat beat = mayBeats.get(i);

			bpmBasedBeats.add(beat.getPosition());

			/*
			// fun idea, but actually it ends up sounding nicer without this algo ._.'

			if (beat.getIsAligned()) {
				// we accept 2 until 5 beats in between
				for (int k = 2; k < 6; k++) {
					boolean foundSituation = true;
					for (int m = i + 1; m <= i + k; m++) {
						if (m >= mayBeats.size()) {
							foundSituation = false;
							break;
						}
						if (mayBeats.get(m).getIsAligned()) {
							foundSituation = false;
							break;
						}
					}
					// we found such a situation for k beats in between - that is, we have
					// k mayBeats generated in between which are all unaligned...
					if (foundSituation) {
						if (i + k + 1 < mayBeats.size()) {
							if (mayBeats.get(i + k + 1).getIsAligned()) {
								// ... now let's see if we have exactly k-1 or k+1 detected
								// beats in between these!
								int detectedBeatsFound = 0;
								for (int maxPosIter = 0; maxPosIter < maximumPositions.size(); maxPosIter++) {
									if ((maximumPositions.get(maxPosIter) > beat.getPosition()) &&
										(maximumPositions.get(maxPosIter) < mayBeats.get(i+k+1).getPosition())) {
										// TODO :: in addition to all that, also check if the beats detected here
										// are somewhat nicely aligned already - not just |   ||   |, but more
										// like |  |  |  |
										detectedBeatsFound++;
									}
								}
								int startPos = beat.getPosition();
								int endPos = mayBeats.get(i+k+1).getPosition();
								// here we generated one more than we detected - so let's remove one!
								if ((detectedBeatsFound == k - 1) ||
									//  we generated one less than we detected - so let's add one!
									(detectedBeatsFound == k + 1)) {
									System.out.println("At startPos " + startPos + " (" + channelPosToMillis(startPos) + " ms)" +
										" and endPos " + endPos + " (" + channelPosToMillis(endPos) + " ms) we detected " +
										detectedBeatsFound + " beats, but generated " + k + " so let's do something about that!");
									for (int subGenI = 0; subGenI < detectedBeatsFound; subGenI++) {
										bpmBasedBeats.add(startPos + (((subGenI+1)*(endPos - startPos))/(detectedBeatsFound+1)));
									}
									i += k;
									break;
								}
							}
						}
					}
				}
			}
			*/
		/*
		}

		debugOut.add("  :: " + bpmBasedBeats.size() + " beats generated");

		Collections.sort(bpmBasedBeats);
		*/



		/*

			switch (useDrumSounds) {
				case 1:


					break;
				case 2:
					beat.setChanged(true);
					switch (instrumentRing) {
						case 0:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
							addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
							break;
						case 1:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + (curBeatLen / 8), baseLoudness);
							addFadedWavMono(WAV_TOM2_DRUM, curBeat + ((2 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM3_DRUM, curBeat + ((3 * curBeatLen) / 8), baseLoudness);
							addFadedWavMono(WAV_TOM4_DRUM, curBeat + ((4 * curBeatLen) / 8), baseLoudness);
							break;
						case 2:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), 2*baseLoudness);
							break;
						case 3:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 4*baseLoudness);
							break;
						case 4:
							break;
						case 5:
							addFadedWavMono(WAV_TOM1_DRUM, curBeat, 2*baseLoudness);
							addFadedWavMono(WAV_TOM1_DRUM, curBeat + ((4 * curBeatLen) / 8), 2*baseLoudness);
							instrumentRing = -1;
							break;
					}
					break;
			}

			instrumentRing++;
			*/
}
