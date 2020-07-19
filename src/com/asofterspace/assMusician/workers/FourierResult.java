/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician.workers;


public class FourierResult extends FourierInstruction {

	private int[] fourier;


	public FourierResult(int fourierNum, int fourierLen, int[] fourier) {
		super(fourierNum, fourierLen);

		this.fourier = fourier;
	}

	public int[] getFourier() {
		return fourier;
	}

	public void setFourier(int[] fourier) {
		this.fourier = fourier;
	}
}
