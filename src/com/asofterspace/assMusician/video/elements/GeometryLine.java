/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.utils.Pair;


public class GeometryLine extends Pair<Integer, Integer> {

	private ColorRGB color;


	public GeometryLine(Integer left, Integer right) {
		super(left, right);
	}

	public ColorRGB getColor() {
		return color;
	}

	public void setColor(ColorRGB color) {
		this.color = color;
	}

}
