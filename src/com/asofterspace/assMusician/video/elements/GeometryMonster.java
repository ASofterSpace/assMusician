/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.assMusician.MusicGenerator;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.images.Image;
import com.asofterspace.toolbox.music.Beat;
import com.asofterspace.toolbox.utils.Line;
import com.asofterspace.toolbox.utils.Pair;
import com.asofterspace.toolbox.utils.Point;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GeometryMonster {

	private List<GeometryPoint> points;
	private List<GeometryLine> lines;
	private Random rand;
	private boolean shapeGuardOn = false;
	private int shapeGuardStillOnFor = 0;
	private int beatNum;
	private int lastShapeGuardStop;


	public GeometryMonster(int width, int height) {
		points = new ArrayList<>();
		lines = new ArrayList<>();

		// start with two points and one line between them
		GeometryPoint topPoint = new GeometryPoint(width / 2.0, height / 3.0);
		GeometryPoint bottomPoint = new GeometryPoint(width / 2.0, (2*height) / 3.0);
		points.add(topPoint);
		points.add(bottomPoint);

		rand = new Random();

		beatNum = 0;

		lastShapeGuardStop = 0;
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
	// TODO :: for each song, load in a picture (some picture, any picture ^^), and make it so that the geo-
	// metry monster goes backwards in time such that in the end of the song the picture appears, but before
	// over the whole duration of the song it was just lines flying around, slowly changing colors, moving,
	// maybe one by one slowly into place... but for that, we would need to be able to take an arbitrary image
	// and divide it into lines of same colors... and we would need to ensure we are not breaking anyone's
	// copyright in taking their images as input for the process
	public void drawOnImage(Image img, int width, int height, int step, int totalFrameAmount, double currentLoudnessScaled,
		ColorRGB color, ColorRGB baseColor, boolean firstChanged, boolean curChanged, boolean encounteredChanged,
		Beat curBeat, int maxSecondsBetweenShapes) {

		if (curBeat != null) {
			beatNum++;
		}

		if (lines.size() < 1) {
			GeometryLine newLine = new GeometryLine(0, 1, baseColor);
			lines.add(newLine);
		}

		double stepDifference = 1.0 / MusicGenerator.frameRate;

		// we want all lines to start slowly changing their colors, five times per second! :)
		for (GeometryLine line : lines) {
			ColorRGB newColor = line.getColor();
			if (rand.nextInt(MusicGenerator.frameRate / 5) == 0) {
				newColor = line.getColor().getSlightlyDifferent();
			}
			int counter = 0;
			while (baseColor.getDifferenceTo(newColor) > 128) {
				newColor = line.getColor().getSlightlyDifferent();
				counter++;
				if (counter > 128) {
					newColor = ColorRGB.intermix(baseColor, line.getColor(), 0.5);
					break;
				}
			}
			line.setColor(newColor);
		}

		// if we have more than two points and the shape guard is not on
		// (during which time many points overlap anyway)...
		if ((points.size() > 2) && !shapeGuardOn) {

			// ... and if a bit of randomness is happening...
			int pointMergeRand = 8;
			if (step % pointMergeRand == 0) {

				// ... then get a list of all points that get drawn on the same pixel
				List<Pair<Integer, Integer>> samePixPoints = new ArrayList<>();
				for (int i = 0; i < points.size(); i++) {
					for (int j = 0; j < i; j++) {
						if (((int) (double) points.get(i).getX() == (int) (double) points.get(j).getX()) &&
							((int) (double) points.get(i).getY() == (int) (double) points.get(j).getY())) {
							samePixPoints.add(new Pair<Integer, Integer>(i, j));
						}
					}
				}

				// and merge one of these pairs, also merging their lines
				if (samePixPoints.size() > 0) {
					Pair<Integer, Integer> mergePair = samePixPoints.get(rand.nextInt(samePixPoints.size()));
					int removePointIndex = mergePair.getLeft();
					int keepPointIndex = mergePair.getRight();

					// move lines from the merge point to the keep point
					List<GeometryLine> newLines = new ArrayList<>();
					for (GeometryLine line : lines) {
						int left = line.getLeft();
						int right = line.getRight();
						if (left == removePointIndex) {
							left = keepPointIndex;
						}
						if (right == removePointIndex) {
							right = keepPointIndex;
						}
						newLines.add(new GeometryLine(left, right, line.getColor()));
					}
					lines = newLines;

					// adjust indexing around the removed point index
					newLines = new ArrayList<>();
					for (GeometryLine line : lines) {
						int left = line.getLeft();
						int right = line.getRight();
						if (left > removePointIndex) {
							left--;
						}
						if (right > removePointIndex) {
							right--;
						}
						newLines.add(new GeometryLine(left, right, line.getColor()));
					}
					lines = newLines;

					// remove duplicate lines
					newLines = new ArrayList<>();
					for (GeometryLine line : lines) {
						int left = line.getLeft();
						int right = line.getRight();
						boolean keepThisLine = true;
						for (GeometryLine newLine : newLines) {
							int newLeft = newLine.getLeft();
							int newRight = newLine.getRight();
							if ((left == newLeft) && (right == newRight)) {
								keepThisLine = false;
								break;
							}
						}
						// also remove lines going from a point to itself, which might happen if we merge
						// a point with a different point that has a line going to it
						if (left == right) {
							keepThisLine = false;
						}
						if (keepThisLine) {
							newLines.add(new GeometryLine(left, right, line.getColor()));
						}
					}
					lines = newLines;

					// actually remove the remove point
					List<GeometryPoint> newPoints = new ArrayList<>();
					for (int i = 0; i < points.size(); i++) {
						if (i != removePointIndex) {
							newPoints.add(points.get(i));
						}
					}
					points = newPoints;
				}
			}
		}

		// after we encountered the first drum sound that we added...
		if (encounteredChanged) {

			// ... every 8 seconds...
			// if (firstChanged || (rand.nextInt(MusicGenerator.frameRate * 8) == 0)) {

			// ... nope, actually, everytime we added a drum...
			if (firstChanged || curChanged) {

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
					GeometryLine newLine1 = null;
					if ((int) duplicatedLine.getLeft() == splitPointIndex) {
						newLine1 = new GeometryLine(duplicatedLine.getRight(), newPointIndex, duplicatedLine.getColor());
					} else {
						newLine1 = new GeometryLine(duplicatedLine.getLeft(), newPointIndex, duplicatedLine.getColor());
					}
					lines.add(newLine1);

					// add a line between the old point and the new point
					GeometryLine newLine2 = new GeometryLine(splitPointIndex, newPointIndex, duplicatedLine.getColor());
					lines.add(newLine2);
				}
			}
		}

		// TODO :: as another fun thing, every once in a while, rotate the entire geometry monster? :D

		// while the shape guard is not on and we are not in the last 16th of the song...
		if ((!shapeGuardOn) && (step < ((15 * totalFrameAmount) / 16))) {

			// ... once every X seconds, do something funny - that is, take on a preconfigured shape...
			// (we have X*frameRate as basis, but subtract the time since the last shape guard start to make it
			// more likely for a funny shape to happen if the last funny shape is already a while ago)
			int shapeRandMax = (MusicGenerator.frameRate * maxSecondsBetweenShapes) - (step - lastShapeGuardStop);
			int shapeRand = 0;
			if (shapeRandMax > 1) {
				shapeRand = rand.nextInt(shapeRandMax);
			}
			System.out.println("Geo monster frame " + step + " out of " + totalFrameAmount + " shape rand: " + shapeRand + " (frames since last shape guard: " + (step - lastShapeGuardStop) + ")");
			if (shapeRand == 0) {
				int shape = rand.nextInt(14);
				System.out.println("Geo monster going into shape " + shape + "!");
				int robin = 0;
				double midX = width / 2.0;
				double midY = height / 2.0;
				double posX = width / 4.0;
				double posY = height / 4.0;
				double posX45 = (45 * width) / 100.0;
				double posY45 = (45 * height) / 100.0;
				double posX33 = (33 * width) / 100.0;
				double posY33 = (33 * height) / 100.0;

				switch (shape) {

					// triangle
					case 0:
						if (points.size() < 3) {
							break;
						}
						activateShapeGuard();
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
						activateShapeGuard();
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
						activateShapeGuard();
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
						activateShapeGuard();
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
						activateShapeGuard();
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
						activateShapeGuard();
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
						activateShapeGuard();
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
					case 7:
						if (points.size() < 7) {
							break;
						}
						activateShapeGuard();
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

					// four-legged starfish
					case 8:
						if (points.size() < 9) {
							break;
						}
						activateShapeGuard();
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(-3.0, -3.0));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width + 3.0, -3.0));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(width + 3.0, height + 3.0));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(-3.0, height + 3.0));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(posX45, posY45));
									break;
								case 5:
									point.setTarget(new Point<Double, Double>(width - posX45, posY45));
									break;
								case 6:
									point.setTarget(new Point<Double, Double>(width - posX45, height - posY45));
									break;
								case 7:
									point.setTarget(new Point<Double, Double>(posX45, height - posY45));
									break;
								default:
									point.setTarget(new Point<Double, Double>(width / 2.0, height / 2.0));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// plus sign
					case 9:
						if (points.size() < 8) {
							break;
						}
						activateShapeGuard();
						for (GeometryPoint point : points) {
							switch (robin) {
								case 0:
									point.setTarget(new Point<Double, Double>(posX, posY33));
									break;
								case 1:
									point.setTarget(new Point<Double, Double>(width - posX, posY33));
									break;
								case 2:
									point.setTarget(new Point<Double, Double>(posX33, posY));
									break;
								case 3:
									point.setTarget(new Point<Double, Double>(posX33, height - posY));
									break;
								case 4:
									point.setTarget(new Point<Double, Double>(posX, height - posY33));
									break;
								case 5:
									point.setTarget(new Point<Double, Double>(width - posX, height - posY33));
									break;
								case 6:
									point.setTarget(new Point<Double, Double>(width - posX33, posY));
									break;
								default:
									point.setTarget(new Point<Double, Double>(width - posX33, height - posY));
									robin = -1;
									break;
							}
							robin++;
						}
						break;

					// circle
					case 10:
						if (points.size() < 2) {
							break;
						}
						activateShapeGuard();
						double radius = (0.8 * height) / 2;
						for (int p = 0; p < points.size(); p++) {
							GeometryPoint point = points.get(p);
							double angle = (2.0 * Math.PI * p) / points.size();
							point.setTarget(new Point<Double, Double>(midX + radius * Math.sin(angle), midY + radius * Math.cos(angle)));
						}
						break;

					// random nonsense
					default:
						int robinMax = 4 + rand.nextInt(8);
						List<Point<Double, Double>> robinTargets = new ArrayList<>();
						for (int i = 0; i < robinMax; i++) {
							robinTargets.add(new Point<Double, Double>(
								(width/10)+(1.0*rand.nextInt((8*width)/10)),
								(height/10)+(1.0*rand.nextInt((8*height)/10))
							));
						}
						activateShapeGuard();
						for (GeometryPoint point : points) {
							point.setTarget(robinTargets.get(robin % robinMax));
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

				// let's try this - we reset the targets for all points on every beat...
				// so that the geo monster is dancing :D
				boolean keepSameQuarter = false;
				if (curBeat != null) {
					resetTarget = true;
					// in case of just dancing with the beat, we want to keep the next target
					// in the same quarter as the current target was, so that the geo monster
					// can still expand to full(ish) size :)
					keepSameQuarter = true;
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

					if (keepSameQuarter) {
						Point<Double, Double> oldTarget = point.getTarget();
						if (oldTarget != null) {
							if (posX < width / 2.0) {
								if (oldTarget.getX() > width / 2.0) {
									posX = width - posX;
								}
							}
							if (posX > width / 2.0) {
								if (oldTarget.getX() < width / 2.0) {
									posX = width - posX;
								}
							}
							if (posY < height / 2.0) {
								if (oldTarget.getY() > height / 2.0) {
									posY = height - posY;
								}
							}
							if (posY > height / 2.0) {
								if (oldTarget.getY() < height / 2.0) {
									posY = height - posY;
								}
							}
						}
					}

					point.setTarget(new Point<Double, Double>(posX, posY));
				}
			}
		}

		// move all the points towards their targets
		for (GeometryPoint point : points) {
			point.moveToTarget((width * stepDifference) / 16);
		}

		// keep the shape guard activated as long as not all points have moved to their designated targets
		if (shapeGuardOn) {
			for (GeometryPoint point : points) {
				if (point.getTarget() != null) {
					activateShapeGuard();
					break;
				}
			}
			shapeGuardStillOnFor--;
			if (shapeGuardStillOnFor < 0) {
				shapeGuardOn = false;
				lastShapeGuardStop = step;
			}
		}

		boolean drawWhite = false;
		ColorRGB white = new ColorRGB(255, 255, 255);
		if (color.equals(white)) {
			drawWhite = true;
		}

		for (GeometryLine line : lines) {
			ColorRGB lineColor = line.getColor();
			if (drawWhite) {
				lineColor = white;
			}
			img.drawLine(
				(int) (double) points.get(line.getLeft()).getX(),
				(int) (double) points.get(line.getLeft()).getY(),
				(int) (double) points.get(line.getRight()).getX(),
				(int) (double) points.get(line.getRight()).getY(),
				lineColor
			);
		}
	}

	private void activateShapeGuard() {

		// turn on the shape guard
		shapeGuardOn = true;

		// and keep it on for at least two more secondss
		shapeGuardStillOnFor = MusicGenerator.frameRate * 2;
	}
}
