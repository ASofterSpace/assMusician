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
	private List<GeometryLine> lines;
	private Random rand;
	private boolean shapeGuardOn = false;


	public GeometryMonster(int width, int height) {
		points = new ArrayList<>();
		lines = new ArrayList<>();

		// start with two points and one line between them
		GeometryPoint topPoint = new GeometryPoint(width / 2.0, height / 3.0);
		GeometryPoint bottomPoint = new GeometryPoint(width / 2.0, (2*height) / 3.0);
		points.add(topPoint);
		points.add(bottomPoint);
		lines.add(new GeometryLine(0, 1));

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
	// TODO :: with a certain (quite small though!) probability, split one geometry monster into two, that
	// are then next to each other and are dancing independently... :D (they start as the same and then
	// get split apart and move sideways, starting to wobble on their own rather than same same - for that,
	// maybe just keep individual target points until reached, even while splitting, just change the origin
	// from which we are calculating)
	// TODO :: add even more fun-ness (e.g. the geometry monster transforming into breakout blocks and then
	// a ball and paddle playing, or transforming into space invaders that all looks like the geometry monster
	// and attack, or even transforming into Game of Life somehow... ^^)
	public void drawOnImage(Image img, int width, int height, int step, double currentLoudnessScaled,
		ColorRGB color) {

		double stepDifference = 1.0 / MusicGenerator.frameRate;

		// every 8 seconds...
		if (rand.nextInt(MusicGenerator.frameRate * 8) == 0) {

			// ... if the shape guard is not currently on (so if we are not currently drawing a special shape)...
			if (!shapeGuardOn) {

				// get a point at random
				int splitPointIndex = rand.nextInt(points.size());
				int newPointIndex = points.size();

				// create a new one at the same position
				GeometryPoint newPoint = new GeometryPoint(points.get(splitPointIndex));

				// add the new point itself
				points.add(newPoint);

				// choose one line connecting the old point to another point
				List<GeometryLine> affectedLines = new ArrayList<>();
				for (GeometryLine line : lines) {
					if ((int) line.getLeft() == splitPointIndex) {
						affectedLines.add(line);
					}
					if ((int) line.getRight() == splitPointIndex) {
						affectedLines.add(line);
					}
				}

				// duplicate that one line with the next point instead of the old point as target
				GeometryLine duplicatedLine = affectedLines.get(rand.nextInt(affectedLines.size()));
				if ((int) duplicatedLine.getLeft() == splitPointIndex) {
					lines.add(new GeometryLine(duplicatedLine.getRight(), newPointIndex));
				} else {
					lines.add(new GeometryLine(duplicatedLine.getLeft(), newPointIndex));
				}

				// add a line between the old point and the new point
				lines.add(new GeometryLine(splitPointIndex, newPointIndex));
			}
		}

		// TODO :: as another fun thing, every once in a while, rotate the entire geometry monster? :D

		// while the shape guard is not on...
		if (!shapeGuardOn) {

			// ... once every 42 seconds, do something funny - that is, take on a preconfigured shape...
			if (rand.nextInt(MusicGenerator.frameRate * 42) == 0) {
				int shape = rand.nextInt(8);
				int robin = 0;
				double posX = width / 4.0;
				double posY = height / 4.0;

				switch (shape) {

					// triangle
					case 0:
						if (points.size() < 3) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(width / 2.0, posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, height - posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(posX, height - posY));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// triangle upside-down
					case 1:
						if (points.size() < 3) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(width / 2.0, height - posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(posX, posY));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// square
					case 2:
						if (points.size() < 4) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
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
						break;

					// sixpoint square
					case 3:
						if (points.size() < 6) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(posX, posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, posY));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(posX, height / 2.0));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(width - posX, height / 2.0));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(width - posX, height - posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(posX, height - posY));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// caro
					case 4:
						if (points.size() < 4) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(width / 2.0, posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, height / 2.0));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(width / 2.0, height - posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(posX, height / 2.0));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// sixangle
					case 5:
						if (points.size() < 6) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(posX, posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, posY));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(width - (posX / 2.0), height / 2.0));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(width - posX, height - posY));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(posX, height - posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(posX / 2.0, height / 2.0));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// diamond
					case 6:
						if (points.size() < 7) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(width / 2.0, posY));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - (posX / 2.0), height / 2.0));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(((width - (posX / 2.0)) + (width / 2.0)) / 2.0, height / 2.0));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(width / 2.0, height / 2.0));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(((posX / 2.0) + (width / 2.0)) / 2.0, height / 2.0));
									break;
								case 5:
									point.setTarget(new Point<Double, Double>(posX / 2.0, height / 2.0));
									break;
								default:
									point.setTarget(new Point<Double, Double>(width / 2.0, height - posY));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// diamond sideways
					default:
						if (points.size() < 7) {
							break;
						}
						shapeGuardOn = true;
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(posX, height / 2.0));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width / 2.0, height - (posY / 2.0)));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(width / 2.0, ((height - (posY / 2.0)) + (height / 2.0)) / 2.0));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(width / 2.0, height / 2.0));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(width / 2.0, ((posY / 2.0) + (height / 2.0)) / 2.0));
									break;
								case 5:
									point.setTarget(new Point<Double, Double>(width / 2.0, posY / 2.0));
									break;
								default:
									point.setTarget(new Point<Double, Double>(width - posX, height / 2.0));
									robin = -1;
									break;
							}
							robin++;
						}
						break;
				}
			}
		}

		// if the shape guard is not on...
		if (!shapeGuardOn) {

			int scaledW = (int) (width * currentLoudnessScaled);
			int scaledH = (int) (height * currentLoudnessScaled);
			double minX = ((width - scaledW) / 2.0);
			double minY = ((height - scaledH) / 2.0);

			// ... check all the current targets to see if they
			// (1) exist, and
			// (2) are within the current loudness envelope
			for (GeometryPoint point : points) {

				boolean resetTarget = false;

				if (point.getTarget() == null) {
					resetTarget = true;

				} else {
					if (point.getTarget().getX() < minX) {
						resetTarget = true;
					}
					if (point.getTarget().getX() > minX + scaledW) {
						resetTarget = true;
					}
					if (point.getTarget().getY() < minY) {
						resetTarget = true;
					}
					if (point.getTarget().getY() > minY + scaledH) {
						resetTarget = true;
					}
				}

				if (resetTarget) {
					// make the target area in which target points can spawn based on percentage of loudness
					// of the current beat as percent of max loudness - so if the current beat is only 50%
					// as loud, then the target points can only spawn from 0.25*width until 0.75*width, same
					// for height...
					double posX = width / 2.0;
					if (scaledW > 0) {
						posX = minX + rand.nextInt(scaledW);
					}
					double posY = height / 2.0;
					if (scaledH > 0) {
						posY = minY + rand.nextInt(scaledH);
					}

					point.setTarget(new Point<Double, Double>(posX, posY));
				}
			}
		}

		// move all the points towards their targets
		for (GeometryPoint point : points) {
			point.moveToTarget((width * stepDifference) / 25);
		}

		// keep the shape guard activated as long as not all points have moved to their designated targets
		if (shapeGuardOn) {
			shapeGuardOn = false;
			for (GeometryPoint point : points) {
				if (point.getTarget() != null) {
					shapeGuardOn = true;
					break;
				}
			}
		}

		for (GeometryLine line : lines) {
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
