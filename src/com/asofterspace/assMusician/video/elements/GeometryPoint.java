/**
 * Code created by A Softer Space, 2020
 */
package com.asofterspace.assMusician.video.elements;

import com.asofterspace.toolbox.utils.Point;


public class GeometryPoint extends Point<Double, Double> {

	private Point<Double, Double> target;


	public GeometryPoint(Double x, Double y) {
		super(x, y);
	}

	public Point<Double, Double> getTarget() {
		return target;
	}

	public void setTarget(Point<Double, Double> target) {
		this.target = target;
	}

	public void moveToTarget(double maxDistance) {

		if (target == null) {
			return;
		}

		int targetDone = 0;

		double curX = getX();
		double tarX = target.getX();
		if (curX < tarX) {
			double dist = tarX - curX;
			if (dist > maxDistance) {
				curX += maxDistance;
			} else {
				curX = tarX;
				targetDone++;
			}
		} else {
			double dist = curX - tarX;
			if (dist > maxDistance) {
				curX -= maxDistance;
			} else {
				curX = tarX;
				targetDone++;
			}
		}

		double curY = getY();
		double tarY = target.getY();
		if (curY < tarY) {
			double dist = tarY - curY;
			if (dist > maxDistance) {
				curY += maxDistance;
			} else {
				curY = tarY;
				targetDone++;
			}
		} else {
			double dist = curY - tarY;
			if (dist > maxDistance) {
				curY -= maxDistance;
			} else {
				curY = tarY;
				targetDone++;
			}
		}

		setX(curX);
		setY(curY);

		if (targetDone >= 2) {
			target = null;
		}
	}

}
