/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.assMusician.MusicGenerator;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.utils.Line;
import com.asofterspace.toolbox.utils.Pair;
import com.asofterspace.toolbox.utils.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GeometryMonster {

	private List<GeometryPoint> points;
	private List<Pair<Integer, Integer>> lines;
	private Random rand;


	public GeometryMonster(int width, int height) {
		points = new ArrayList<>();
		lines = new ArrayList<>();

		// start with two points and one line between them
		GeometryPoint topPoint = new GeometryPoint(width / 2.0, height / 3.0);
		GeometryPoint bottomPoint = new GeometryPoint(width / 2.0, (2*height) / 3.0);
		points.add(topPoint);
		points.add(bottomPoint);
		lines.add(new Pair<Integer, Integer>(0, 1));

		rand = new Random();
	}

	public void drawOnImage(Image img, int width, int height, int step, ColorRGB color) {

		double stepDifference = 1.0 / MusicGenerator.frameRate;

		if (rand.nextInt(MusicGenerator.frameRate * 8) == 0) {

			// get a point at random
			int splitPointIndex = rand.nextInt(points.size());
			int newPointIndex = points.size();

			// create a new one at the same position
			GeometryPoint newPoint = new GeometryPoint(points.get(splitPointIndex));

			// add the new point itself
			points.add(newPoint);

			// choose one line connecting the old point to another point
			List<Pair<Integer, Integer>> affectedLines = new ArrayList<>();
			for (Pair<Integer, Integer> line : lines) {
				if ((int) line.getLeft() == splitPointIndex) {
					affectedLines.add(line);
				}
				if ((int) line.getRight() == splitPointIndex) {
					affectedLines.add(line);
				}
			}

			// duplicate that one line with the next point instead of the old point as target
			Pair<Integer, Integer> duplicatedLine = affectedLines.get(rand.nextInt(affectedLines.size()));
			if ((int) duplicatedLine.getLeft() == splitPointIndex) {
				lines.add(new Pair<Integer, Integer>(duplicatedLine.getRight(), newPointIndex));
			} else {
				lines.add(new Pair<Integer, Integer>(duplicatedLine.getLeft(), newPointIndex));
			}

			// add a line between the old point and the new point
			lines.add(new Pair<Integer, Integer>(splitPointIndex, newPointIndex));
		}

		for (GeometryPoint point : points) {
			if (point.getTarget() == null) {
				point.setTarget(
					new Point<Double, Double>((double) rand.nextInt(width), (double) rand.nextInt(height))
				);
			}
			point.moveToTarget((width * stepDifference) / 25);
		}

		for (Pair<Integer, Integer> line : lines) {
			img.drawLine(
				(int) (double) points.get(line.getLeft()).getX(),
				(int) (double) points.get(line.getLeft()).getY(),
				(int) (double) points.get(line.getRight()).getX(),
				(int) (double) points.get(line.getRight()).getY(),
				color
			);
		}
	}
}
