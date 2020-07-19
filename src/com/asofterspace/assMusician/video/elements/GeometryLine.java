/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.utils.Pair;


public class GeometryLine extends Pair<Integer, Integer> {

	private ColorRGB color;


	public GeometryLine(Integer left, Integer right, ColorRGB color) {
		super(left, right);

		setColor(color);
	}

	public ColorRGB getColor() {
		return color;
	}

	public void setColor(ColorRGB color) {
		this.color = color;
	}

}
