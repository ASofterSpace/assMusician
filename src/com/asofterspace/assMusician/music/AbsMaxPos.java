/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.music;




public class AbsMaxPos {

	private int pos;

	private double quality;

	private int origPosition;


	public AbsMaxPos(int pos) {
		this.pos = pos;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public int getOrigPosition() {
		return origPosition;
	}

	public void setOrigPosition(int origPosition) {
		this.origPosition = origPosition;
	}

	@Override
	public String toString() {
		return "abs max pos " + getPos() + " with quality " + getQuality();
	}
}
