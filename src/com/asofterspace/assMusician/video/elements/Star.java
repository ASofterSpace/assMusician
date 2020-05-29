/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import java.util.Random;


public class Star {

	private int x;
	private int y;
	private double brightness;

	// 0 .. do nothing
	// 1 .. twinkle (grow brighter)
	// 2 .. twinkle (grow less bright)
	private int mode;

	private Random rand;


	public Star(int x, int y) {
		this.x = x;
		this.y = y;
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

	public void calcFrame(double step) {
		switch (mode) {
			case 1:
				brightness += step / 20;
				if (brightness > 1) {
					mode = 2;
					brightness = 1;
				}
				break;
			case 2:
				brightness -= step / 50;
				if (brightness < 0) {
					mode = 0;
					brightness = 0;
				}
				break;
			default:
				if (rand.nextInt(1024) < 1) {
					mode = 1;
				}
				break;
		}
	}

}
