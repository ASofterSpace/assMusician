/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician.workers;


public class FourierInstruction {

	private int fourierNum;

	private int fourierLen;


	public FourierInstruction(int fourierNum, int fourierLen) {
		this.fourierNum = fourierNum;
		this.fourierLen = fourierLen;
	}

	public int getFourierNum() {
		return fourierNum;
	}

	public void setFourierNum(int fourierNum) {
		this.fourierNum = fourierNum;
	}

	public int getFourierLen() {
		return fourierLen;
	}

	public void setFourierLen(int fourierLen) {
		this.fourierLen = fourierLen;
	}
}
