/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician.music;


public class DrumSoundAtPos {

	private int beatPos;

	private int beatLength;

	private double baseLoudness;

	private long intensity;

	private int drumPatternIndicator;

	private int beatPattern = 0;


	public DrumSoundAtPos(int beatPos, int beatLength, double baseLoudness, long intensity,
		int drumPatternIndicator) {

		this.beatPos = beatPos;
		this.beatLength = beatLength;
		this.baseLoudness = baseLoudness;
		this.intensity = intensity;
		this.drumPatternIndicator = drumPatternIndicator;
	}

	public int getBeatPos() {
		return beatPos;
	}

	public void setBeatPos(int beatPos) {
		this.beatPos = beatPos;
	}

	public int getBeatLength() {
		return beatLength;
	}

	public void setBeatLength(int beatLength) {
		this.beatLength = beatLength;
	}

	public double getBaseLoudness() {
		return baseLoudness;
	}

	public void setBaseLoudness(double baseLoudness) {
		this.baseLoudness = baseLoudness;
	}

	public int getDrumPatternIndicator() {
		return drumPatternIndicator;
	}

	public void setDrumPatternIndicator(int drumPatternIndicator) {
		this.drumPatternIndicator = drumPatternIndicator;
	}

	public int getBeatPattern() {
		return beatPattern;
	}

	public void setBeatPattern(int beatPattern) {
		this.beatPattern = beatPattern;
	}

	public long getIntensity() {
		return intensity;
	}

}
