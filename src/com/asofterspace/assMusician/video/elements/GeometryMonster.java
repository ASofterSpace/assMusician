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
import java.util.Random;


public class GeometryMonster {

	private List<GeometryPoint> points;
	private List<Line<Double>> lines;
	private Random rand;


	public GeometryMonster(int width, int height) {
		points = new ArrayList<>();
		lines = new ArrayList<>();

		// start with two points and one line between them
		GeometryPoint topPoint = new GeometryPoint(width / 2.0, height / 3.0);
		GeometryPoint bottomPoint = new GeometryPoint(width / 2.0, (2*height) / 3.0);
		points.add(topPoint);
		points.add(bottomPoint);
		lines.add(new Line<Double>(topPoint, bottomPoint));

		rand = new Random();
	}

	public void drawOnImage(Image img, int width, int height, int step, ColorRGB color) {

		double doubleStep = (step * 1.0) / MusicGenerator.frameRate;

		for (GeometryPoint point : points) {
			if (point.getTarget() == null) {
				point.setTarget(
					new Point<Double, Double>((double) rand.nextInt(width), (double) rand.nextInt(height))
				);
			}
			point.moveToTarget((width * doubleStep) / 250);
		}

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
