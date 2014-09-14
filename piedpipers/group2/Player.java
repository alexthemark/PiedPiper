package piedpipers.group2;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	enum GameStrategy {
		SWEEPING, SEARCHING
	}
	enum PiperStatus {
		GOING_TO_GATE, MOVING_TO_SWEEP, SWEEPING, RETURNING_TO_GATE
	}
	
	static int npipers;
	
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	
	static Point target = new Point();
	
	static boolean initi = false;
	static GameStrategy gameStrategy;
	PiperStatus piperStatus;
	
	
	
	public void init() {
		gameStrategy = GameStrategy.SWEEPING;
		piperStatus = PiperStatus.GOING_TO_GATE;
	}

	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}
	
	static boolean isNearEachOther(double a, double b) {
		int TOLERANCE = 1;
		return Math.abs(a-b) < TOLERANCE;
	}
	
	void updatePiperStatus(Point currentLocation) {
		if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
			// Piper has made it to the other side
			if (getSide(currentLocation) == 1) {
				piperStatus = PiperStatus.MOVING_TO_SWEEP;
			}
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_SWEEP)) {
			// Piper has made it to their starting position
			if (closetoWall(currentLocation)) {
				piperStatus = PiperStatus.SWEEPING;
			}
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING)) {
			// Piper has made it back to the middle
			if (isNearEachOther(currentLocation.y, (double) dimension/2)) {
				piperStatus = PiperStatus.RETURNING_TO_GATE;
			}
		}
		else if (piperStatus.equals(PiperStatus.RETURNING_TO_GATE)) {
			// Piper has reached 4 meters into the left side
			if (isNearEachOther(currentLocation.x, (double) dimension/2 - 4)) {
				piperStatus = PiperStatus.GOING_TO_GATE;
			}
		}
	}
	
	void updateGameStrategy(Point[] rats) {
		int remainingRats = 0;
		for (Point rat : rats) {
			if (getSide(rat) == 1)
				remainingRats++;
		}
		// TODO: Choose a criterion to start searching out individual rats
		boolean shouldSearchForIndividualRats = false;
		if (shouldSearchForIndividualRats) {
			gameStrategy = GameStrategy.SEARCHING;
		} else {
			gameStrategy = GameStrategy.SWEEPING;
		}
	}

	public Point move(Point[] pipers, // positions of pipers
			Point[] rats) { // positions of the rats
		npipers = pipers.length;
		Point gate = new Point(dimension/2, dimension/2);
		if (!initi) {
			this.init();
			initi = true;
		}
		Point current = pipers[id];
		double ox = 0, oy = 0;
		Point goalPos = new Point(gate); // default position to move to is the gate
		updateGameStrategy(rats);
		if (gameStrategy.equals(GameStrategy.SWEEPING)) {
			updatePiperStatus(current);
			// Get the pied pipers over to the right side
			if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
				this.music = false;
				System.out.println("move toward the right side");
			} 
			// Get the pied pipers into their starting net positions
			else if (piperStatus.equals(PiperStatus.MOVING_TO_SWEEP)) { 
				double yGoal = id % 2 == 0 ? 0 : dimension;
				double xGoal = dimension / 2 + (((dimension - 10) * id + 1) / (2 * npipers) + 5);
				goalPos = new Point(xGoal, yGoal);
				this.music = false;
				System.out.println("move into starting positions");
			}
			// Bring the pipers/rats into the center
			else if (piperStatus.equals(PiperStatus.SWEEPING)) {
				this.music = true;
				double xGoal = dimension / 2 + (((dimension - 10) * id + 1) / (2 * npipers) + 5);
				double yGoal = dimension / 2;
				goalPos = new Point(xGoal, yGoal);
				System.out.println("pinching towards center");
			}
			// Bring the rats through the gate and into the place they go
			else if (piperStatus.equals(PiperStatus.RETURNING_TO_GATE)) {
				this.music = true;
				double xGoal = dimension / 2 - 4;
				double yGoal = dimension / 2;
				goalPos = new Point(xGoal, yGoal);
				System.out.println("Moving towards left side");
			}
		}
		else if (gameStrategy.equals(GameStrategy.SEARCHING)) {
			// Logic for searching out individual rats goes here
		}
		double dist = distance(current, goalPos);
		assert dist > 0;
		double speed = this.music ? mpspeed : pspeed;
		ox = (goalPos.x - current.x) / dist * speed;
		oy = (goalPos.y - current.y) / dist * speed;
		current.x += ox;
		current.y += oy;
		return current;
	}
	
	boolean closetoWall (Point current) {
		boolean wall = false;
		if (Math.abs(current.x-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(current.y-dimension)<pspeed) {
			wall = true;
		}
		if (Math.abs(current.y)<pspeed) {
			wall = true;
		}
		return wall;
	}
	
	int getSide(double x, double y) {
		if (x < dimension * 0.5)
			return 0;
		else if (x > dimension * 0.5)
			return 1;
		else
			return 2;
	}

	int getSide(Point p) {
		return getSide(p.x, p.y);
	}

}