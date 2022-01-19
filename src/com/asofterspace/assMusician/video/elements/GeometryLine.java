/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.toolbox.images.ColorRGBA;
import com.asofterspace.toolbox.utils.Pair;


public class GeometryLine extends Pair<Integer, Integer> {

	private ColorRGBA color;


	public GeometryLine(Integer left, Integer right, ColorRGBA color) {
		super(left, right);

		setColor(color);
	}

	public ColorRGBA getColor() {
		return color;
	}

	public void setColor(ColorRGBA color) {
		this.color = color;
	}

}
