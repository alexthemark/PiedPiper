package piedpipers.group2;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	enum PiperStatus {
		GOING_TO_GATE, MOVING_TO_POSITION, IN_MAGNET_POSITION, SWEEPING_LEFT, 
		HUNTING, GOING_HOME, MOVING_TO_SWEEP, SWEEPING, DROPPING_OFF,
		INTERCEPTING
	}
	enum GameStrategy {
		MAGNET_WITH_NET, MAGNET_WITHOUT_NET, INTERCEPT
	}
	enum Side {
		GOAL, TARGET, FENCE
	}
	
	static int totalPipers;
	static int totalRats;
	
	static final double pspeed = 0.49;
	static final double mpspeed = 0.09;
	
	static boolean initi = false;
	PiperStatus piperStatus;
	static boolean allRatsCaptured = false;
	static boolean[] magnetPipers;
	static Point[] piperPositions;
	static int nMagnetPipers;
	static int magnetFloor;
	static int magnetCeiling;
	static boolean[] playedLastTurn;
	static boolean[] movingDown;
	static Rectangle2D magnet;
	static GameStrategy currentStrategy;
	static final int CENTER_MAGNET_THRESHOLD = 1200;
	static final int OSCILLATION_DISTANCE = 5;
	static final int PIPER_RADIUS = 10;
	static final int PIPER_DIAMETER = 2 * PIPER_RADIUS;
	static final int SWEEP_THRESHOLD = 2;
	static final int MIN_PIPERS_FOR_SWEEP = 4;
	static final int NUMBER_OF_MAGNET_PIPERS = 1;
	
	public void init() {
		piperStatus = PiperStatus.GOING_TO_GATE;
		nMagnetPipers = totalPipers > 0 ? NUMBER_OF_MAGNET_PIPERS : 0;
		magnetPipers = new boolean[totalPipers];
		int firstMagnetX = dimension > CENTER_MAGNET_THRESHOLD ? 3*dimension/4 : (dimension / 2) + PIPER_DIAMETER;
		int bottomMagnetY = dimension/2 - OSCILLATION_DISTANCE;
		magnetFloor = bottomMagnetY;
		magnetCeiling = magnetFloor + 2 * OSCILLATION_DISTANCE;
		piperPositions = new Point[totalPipers];
		//set magnet pipers
		for (int i = 0; i < nMagnetPipers; i++) {
			magnetPipers[i] = true;
			piperPositions[i] = new Point(firstMagnetX + PIPER_DIAMETER * i, bottomMagnetY);
		}
		//set hunter pipers
		for (int i = nMagnetPipers; i < totalPipers; i++) {
			double theta = (2 * Math.PI * i) / totalPipers;
			double xOffset = PIPER_RADIUS * Math.cos(theta);
			double yOffset = PIPER_RADIUS * Math.sin(theta);
			piperPositions[i] = new Point(firstMagnetX + xOffset, bottomMagnetY + yOffset);
		}
		playedLastTurn = new boolean[totalPipers];
		movingDown = new boolean[totalPipers];
		magnet = new Rectangle2D.Double(firstMagnetX - PIPER_RADIUS, bottomMagnetY - PIPER_RADIUS, (PIPER_DIAMETER *nMagnetPipers), OSCILLATION_DISTANCE * 2 + PIPER_DIAMETER);
		currentStrategy = getStrategy(totalPipers, totalRats, dimension);
	}

	static boolean continueSweeping(int nPipers, int nRats, int dimension) {
		return (nRats/dimension > SWEEP_THRESHOLD && nPipers >= MIN_PIPERS_FOR_SWEEP) || (nPipers > 0 && dimension / nPipers < PIPER_DIAMETER && totalRats > nPipers * 5);
	}
	
	static GameStrategy getStrategy(int nPipers, int nRats, int dimension) {
		if (nPipers == 1)
			return GameStrategy.INTERCEPT;
		else if (continueSweeping(nPipers, nRats, dimension))
			return GameStrategy.MAGNET_WITH_NET;
		else return GameStrategy.MAGNET_WITHOUT_NET;
	}
	
	void updatePiperStatus(Point currentLocation, int nRatsLeft) {
		if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
			if (getSide(currentLocation) == Side.TARGET) {
				if (currentStrategy.equals(GameStrategy.INTERCEPT)) {
					piperStatus = PiperStatus.INTERCEPTING;
				}
				else if (currentStrategy.equals(GameStrategy.MAGNET_WITHOUT_NET) || magnetPipers[id]) {
					piperStatus = PiperStatus.MOVING_TO_POSITION;
				}
				else if (currentStrategy.equals(GameStrategy.MAGNET_WITH_NET)) {
					piperStatus = PiperStatus.MOVING_TO_SWEEP;
				}
			}
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_SWEEP)) {
			if (closetoWall(currentLocation)) {
				piperStatus = PiperStatus.SWEEPING;
			}
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING)) {
			if (isNearEachOther(currentLocation.y, dimension /2)) {
				piperStatus = PiperStatus.DROPPING_OFF;
			}
		}
		else if (piperStatus.equals(PiperStatus.DROPPING_OFF)) {
			if (allRatsCaptured) {
				piperStatus = PiperStatus.SWEEPING_LEFT;
			}
			if (isNearEachOther(currentLocation.x, dimension/2 + PIPER_DIAMETER)) {
				if (continueSweeping(totalPipers, nRatsLeft, dimension))
					piperStatus = PiperStatus.MOVING_TO_SWEEP;
				else
					piperStatus = PiperStatus.MOVING_TO_POSITION;
			}
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) {
			if (distance(currentLocation, piperPositions[id]) < 1) {
				if (magnetPipers[id]) {
					piperStatus = PiperStatus.IN_MAGNET_POSITION;
				}
				else {
					piperStatus = PiperStatus.HUNTING;
				}
			}
		}
		else if (piperStatus.equals(PiperStatus.HUNTING) 
				|| piperStatus.equals(PiperStatus.INTERCEPTING)
				|| piperStatus.equals(PiperStatus.IN_MAGNET_POSITION)){
			if (allRatsCaptured) {
				piperStatus = PiperStatus.SWEEPING_LEFT;
			}
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING_LEFT)) {
			if (isNearEachOther(currentLocation.y, dimension /2)) {
				piperStatus = PiperStatus.GOING_HOME;
			}
		}
	}

	public Point move(Point[] pipers,
			Point[] rats, boolean[] pipermusic, int[] ratThetas) { 
		totalPipers = pipers.length;
		totalRats = rats.length;
		int nRatsCaptured = ratsCaptured(pipers, rats);
		allRatsCaptured = nRatsCaptured == totalRats;
		Point gate = new Point(dimension/2, dimension/2);
		if (!initi) {
			this.init();
			initi = true;
		}
		Point current = pipers[id];
		double ox = 0, oy = 0;
		Point goalPos = new Point(gate); 
		updatePiperStatus(current, totalRats - nRatsCaptured);
		
		// use the current piper's status to decide where it should go
		// and if it should play music
		if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
			this.music = false;
		} 
		else if (piperStatus.equals(PiperStatus.INTERCEPTING)) {
			Double distanceToNearestRat = Double.MAX_VALUE;
			Point goalRat = gate;
			for (Point rat : rats) {
				if (distance(rat, current) < distanceToNearestRat && distance(rat,current) > PIPER_RADIUS && getSide(rat) != Side.GOAL) {
					goalRat = rat;
					distanceToNearestRat = distance(rat, current);
				}
			}
			goalPos = goalRat;
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_SWEEP)) { 
			double yGoal = id % 2 == 0 ? dimension : 0;
			double xGoal = dimension / 2 + 9.9 * (id - nMagnetPipers);
			if (xGoal > dimension) {
				xGoal = xGoal % (dimension / 2) + dimension / 2;
				yGoal = dimension;
			}
			goalPos = new Point(xGoal, yGoal);
			this.music = false;
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING)) {
			double xGoal = dimension / 2 + 9.9 * (id - nMagnetPipers);
			if (xGoal > dimension) {
				xGoal = xGoal % (dimension / 2) + dimension / 2;
			}
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.DROPPING_OFF)) {
			double xGoal = dimension / 2 + PIPER_DIAMETER;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) { 
			goalPos = piperPositions[id];
			this.music = false;
		}
		else if (piperStatus.equals(PiperStatus.IN_MAGNET_POSITION)) {
			goalPos = piperPositions[id];
			if (movingDown[id]) {
				if (current.y > magnetFloor)
					goalPos.y = magnetFloor;
				else {
					movingDown[id] = false;
					goalPos.y = magnetCeiling;
				}
			}
			else {
				if (current.y < magnetCeiling)
					goalPos.y = magnetCeiling;
				else {
					movingDown[id] = true;
					goalPos.y = magnetFloor;
				}
			}
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING_LEFT)) {
			double xGoal = dimension / 2;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.GOING_HOME)) {
			double xGoal = dimension / 2 - PIPER_RADIUS;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			this.music = true;
		}
		else if (piperStatus.equals(PiperStatus.HUNTING)) {
			int nearestRatIndex = 0;
			double nearestRatDist = Double.MAX_VALUE;
			ArrayList<Integer> nearbyRatIndeces = new ArrayList<Integer>();
			for (int ratIndex = 0; ratIndex < rats.length; ratIndex++) {
				Point rat = rats[ratIndex];
				double distanceToRat = distance(rat, current);
				boolean otherPiperCloser = false;
				for (int j = nMagnetPipers; j < pipers.length; j++) {
					Point piper = pipers[j];
					if (distance(rat, piper) < distanceToRat)
						otherPiperCloser = true;
				}
				if (distanceToRat < PIPER_RADIUS)
					nearbyRatIndeces.add(ratIndex);
				if (distance(rat, current) < nearestRatDist && !otherPiperCloser && getSide(rat) != Side.GOAL && !magnet.contains(rat.x, rat.y) && !ratTrajectoryHitsMagnet(rats[ratIndex], ratThetas[ratIndex], dimension)) {
					nearestRatDist = distance(rat, current);
					nearestRatIndex = ratIndex;
				}
			}
			goalPos = rats[nearestRatIndex];
			if (distance(goalPos, current) < PIPER_RADIUS) {
				boolean ratOnPath = false;
				for (int ratIndex : nearbyRatIndeces) {
					if (ratTrajectoryHitsMagnet(rats[ratIndex], ratThetas[ratIndex], dimension)) {
						ratOnPath = true;
					}
				}
				if (playedLastTurn[id] || ratOnPath) {
					this.music = false;
					playedLastTurn[id] = false;
				}
				else {
					this.music = true;
					playedLastTurn[id] = true;
				}
			}
			else {
				this.music = false;
			}
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

	private static boolean ratTrajectoryHitsMagnet(Point rat, double theta, int dimensions){
		Line2D ratLine=new Line2D.Double(rat.x, rat.y, (dimensions*Math.sin(theta * Math.PI / 180) + rat.x), (dimensions*Math.cos(theta * Math.PI / 180) + rat.y));
		return ratLine.intersects(magnet);
	}
	
	private boolean closetoWall (Point current) {
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
	
	private static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}
	
	private static boolean isNearEachOther(double a, double b) {
		int TOLERANCE = 1;
		return Math.abs(a-b) < TOLERANCE;
	}
	
	private Side getSide(double x, double y) {
		if (x < dimension * 0.5)
			return Side.GOAL;
		else if (x > dimension * 0.5)
			return Side.TARGET;
		else
			return Side.FENCE;
	}

	private Side getSide(Point p) {
		return getSide(p.x, p.y);
	}
	
	private int ratsCaptured(Point[] pipers, Point[] rats) {
		int capturedRats = 0;
		for (Point rat : rats) {
			if (getSide(rat) == Side.GOAL) {
				capturedRats++;
			}
			else {
				for (Point piper : pipers) {
					if (distance(piper, rat) < 9) {
						capturedRats++;
						break;
					}
				}
			}
		}
		return capturedRats;
	}

}
