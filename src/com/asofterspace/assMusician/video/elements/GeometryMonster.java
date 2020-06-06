/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.assMusician.MusicGenerator;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.utils.Line;
import com.asofterspace.toolbox.utils.Point;

import java.util.ArrayList;
import java.util.List;


public class GeometryMonster {

	List<Point<Double, Double>> points;
	List<Line<Double>> lines;

	public GeometryMonster(int width, int height) {
		points = new ArrayList<>();
		lines = new ArrayList<>();

		// start with two points and one line between them
		Point<Double, Double> topPoint = new Point<Double, Double>(width / 2.0, height / 3.0);
		Point<Double, Double> bottomPoint = new Point<Double, Double>(width / 2.0, (2*height) / 3.0);
		points.add(topPoint);
		points.add(bottomPoint);
		lines.add(new Line<Double>(topPoint, bottomPoint));
	}

	public void drawOnImage(Image img, int width, int height, int step, ColorRGB color) {

		double doubleStep = (step * 1.0) / MusicGenerator.frameRate;

		/*
		for (Point<Double, Double> point : points) {

		}
		*/

		for (Line<Double> line : lines) {
			img.drawLine(
				(int) (double) line.getFrom().getX(),
				(int) (double) line.getFrom().getY(),
				(int) (double) line.getTo().getX(),
				(int) (double) line.getTo().getY(),
				color
			);
		}
	}
}
