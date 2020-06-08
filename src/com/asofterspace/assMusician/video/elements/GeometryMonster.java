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

	// TODO :: maybe give different nodes (or edges?) different colors - maybe just slightly different,
	// so that it changes over time...
	// TODO :: maybe have different predefined shapes (such as a pirate flag, or a bunny head, or something),
	// and once we have that exact amount of edges we put that shape as target for all nodes and do not add
	// any more nodes until the target is reached, and then free-wobble again...
	// TODO :: maybe have something exactly like this geometry monster, but smaller (so a full geometry monster
	// is generated, but then is scaled to something smaller), and have that wobble around, as it would look
	// really intricate and cool?
	public void drawOnImage(Image img, int width, int height, int step, double currentLoudnessScaled,
		ColorRGB color) {

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

		// once per minute, do something funny...
		if (rand.nextInt(MusicGenerator.frameRate * 60) == 0) {
			int robin = 0;
			for (GeometryPoint point : points) {
				double posX = width / 4.0;
				double posY = height / 4.0;
				switch (robin) {
					case 0:
						point.setTarget(new Point<Double, Double>(posX, posY));
						break;
					case 1:
						point.setTarget(new Point<Double, Double>(width - posX, posY));
						break;
					case 2:
						point.setTarget(new Point<Double, Double>(width - posX, height - posY));
						break;
					default:
						point.setTarget(new Point<Double, Double>(posX, height - posY));
						robin = -1;
						break;
				}
				robin++;
			}
		}

		for (GeometryPoint point : points) {
			if (point.getTarget() == null) {
				// make the target area in which target points can spawn based on percentage of loudness
				// of the current beat as percent of max loudness - so if the current beat is only 50%
				// as loud, then the target points can only spawn from 0.25*width until 0.75*width, same
				// for height...
				int scaledW = (int) (width * currentLoudnessScaled);
				int scaledH = (int) (height * currentLoudnessScaled);
				double posX = width / 2.0;
				if (scaledW > 0) {
					posX = ((width - scaledW) / 2.0) + rand.nextInt(scaledW);
				}
				double posY = height / 2.0;
				if (scaledH > 0) {
					posY = ((height - scaledH) / 2.0) + rand.nextInt(scaledH);
				}
				point.setTarget(new Point<Double, Double>(posX, posY));
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
