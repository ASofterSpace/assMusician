/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import java.util.Random;


public class Star {

	private int x;
	private int y;
	private double brightness;
	private double visibleUntil;
	private int offset;

	// 0 .. invisible
	// 1 .. twinkle (grow brighter)
	// 2 .. twinkle (grow less bright)
	// 3 .. visible
	private int mode;

	private Random rand;


	public Star(int x, int y) {
		this.x = x;
		this.y = y;
		this.offset = 0;
		this.mode = 0;
		this.brightness = 0;
		this.rand = new Random();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public double getBrightness() {
		return brightness;
	}

	public void calcFrame(int step) {
		switch (mode) {
			case 1:
				brightness += (step - offset) / 64.0;
				if (brightness > 1) {
					visibleUntil = rand.nextInt(320);
					mode = 3;
					brightness = 1;
				}
				break;
			case 2:
				brightness -= (step - offset) / 128.0;
				if (brightness < 0) {
					mode = 0;
					brightness = 0;
				}
				break;
			case 3:
				if (step - offset > visibleUntil) {
					mode = 2;
				}
				break;
			default:
				if (rand.nextInt(512) < 1) {
					mode = 1;
					offset = step;
				}
				break;
		}
	}

}
