package piedpipers.group2;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	enum PiperStatus {
		GOING_TO_GATE, MOVING_TO_POSITION, IN_POSITION, SWEEPING_LEFT, 
		HUNTING, GOING_HOME, MOVING_TO_SWEEP, SWEEPING, DROPPING_OFF
	}
	enum GameStrategy {
		MAGNET_WITH_NET, MAGNET_WITHOUT_NET, INTERCEPT
	}
	
	static int npipers;
	static int nrats;
	
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	
	static boolean initi = false;
	PiperStatus piperStatus;
	static boolean allRatsCaptured = false;
	static double percentMagnetPipers = .125;
	static boolean[] magnetPipers;
	static Point[] piperPositions;
	static int nMagnetPipers;
	static int magnetFloor;
	static int magnetCeiling;
	static boolean[] playedLastTurn;
	static boolean[] movingLeft;
	static boolean[] inPosition;
	static Rectangle2D magnet;
	static int oscillation_distance;
	static GameStrategy currentStrategy;
	
	public void init() {
		piperStatus = PiperStatus.GOING_TO_GATE;
		oscillation_distance = dimension/100;
		nMagnetPipers = (int) (percentMagnetPipers * npipers);
		magnetPipers = new boolean[npipers];
		int firstMagnetX = (dimension / 2 + 20);
		int bottomMagnetY = dimension/2 - oscillation_distance;
		magnetFloor = bottomMagnetY;
		magnetCeiling = magnetFloor + 2 * oscillation_distance;
		piperPositions = new Point[npipers];
		//set magnet pipers
		for (int i = 0; i < nMagnetPipers; i++) {
			magnetPipers[i] = true;
			piperPositions[i] = new Point(firstMagnetX + 20 * i, bottomMagnetY);
		}
		//set hunter pipers
		for (int i = nMagnetPipers; i < npipers; i++) {
			piperPositions[i] = new Point(firstMagnetX + (dimension/50) * i * (i % 2), bottomMagnetY + (dimension/50)*i * (i % 2));
		}
		playedLastTurn = new boolean[npipers];
		movingLeft = new boolean[npipers];
		inPosition = new boolean[npipers];
		magnet = new Rectangle2D.Double(firstMagnetX - 10, bottomMagnetY - 10, (20 *nMagnetPipers), oscillation_distance*2 + 20);
		currentStrategy = getStrategy(npipers, nrats, dimension);
	}
	
	static GameStrategy getStrategy(int nPipers, int nRats, int dimension) {
		return GameStrategy.MAGNET_WITH_NET;
	}
	
	void updatePiperStatus(Point currentLocation) {
		if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
			// Piper has made it to the other side
			if (getSide(currentLocation) == 1) {
				if (currentStrategy.equals(GameStrategy.MAGNET_WITHOUT_NET) || magnetPipers[id]) {
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
			if (isNearEachOther(currentLocation.x, dimension/2 + 20)) {
				piperStatus = PiperStatus.MOVING_TO_POSITION;
			}
		}
		else if (piperStatus.equals(PiperStatus.HUNTING)){
			if (allRatsCaptured) {
				piperStatus = PiperStatus.SWEEPING_LEFT;
			}
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) {
			// Piper has made it to their starting position
			if (isNearEachOther(currentLocation.x, piperPositions[id].x)) {
				if (magnetPipers[id]) {
					piperStatus = PiperStatus.IN_POSITION;
				}
				else {
					piperStatus = PiperStatus.HUNTING;
				}
			}
		}
		else if (piperStatus.equals(PiperStatus.IN_POSITION)) {
			// Piper has made it back to the middle
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
	
	/*
	 * Get the number of rats that have been captured by the pipers
	 */
	private int ratsCaptured(Point[] pipers, Point[] rats) {
		int capturedRats = 0;
		for (Point rat : rats) {
			if (getSide(rat) == 0) {
				capturedRats++;
			}
			else {
				for (Point piper : pipers) {
					if (distance(piper, rat) < 10) {
						capturedRats++;
						break;
					}
				}
			}
		}
		System.out.println("Rats remaining: " + (rats.length - capturedRats));
		return capturedRats;
	}

	public Point move(Point[] pipers, // positions of pipers
			Point[] rats, boolean[] pipermusic, int[] ratThetas) { // positions of the rats
		npipers = pipers.length;
		nrats = rats.length;
		Point gate = new Point(dimension/2, dimension/2);
		if (!initi) {
			this.init();
			initi = true;
		}
		Point current = pipers[id];
		double ox = 0, oy = 0;
		Point goalPos = new Point(gate); // default position to move to is the gate
		updatePiperStatus(current);
		allRatsCaptured = ratsCaptured(pipers, rats) == rats.length;
		// we're dealing with magnet pipers here
		// Get the pied pipers over to the right side
		if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
			this.music = false;
			System.out.println("move toward the right side");
		} 
		else if (piperStatus.equals(PiperStatus.MOVING_TO_SWEEP)) { 
			double yGoal = 0;
			double xGoal = dimension / 2 + 20 * id;
			if (xGoal > dimension) {
				xGoal = xGoal % (dimension / 2) + dimension / 2;
				yGoal = dimension;
			}
			System.out.println("Piper " + id + "going to x coord" + xGoal);
			goalPos = new Point(xGoal, yGoal);
			this.music = false;
			System.out.println("move into starting positions");
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING)) {
			this.music = true;
			double xGoal = dimension / 2 + 20 * id;
			if (xGoal > dimension) {
				xGoal = xGoal % (dimension / 2) + dimension / 2;
			}
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("pinching towards center");
		}
		else if (piperStatus.equals(PiperStatus.DROPPING_OFF)) {
			this.music = true;
			double xGoal = dimension / 2 + 20;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("dropping the kids off at school");
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) { 
			goalPos = piperPositions[id];
			this.music = false;
			System.out.println("move into starting positions");
		}
		else if (piperStatus.equals(PiperStatus.IN_POSITION)) {
			goalPos = piperPositions[id];
			this.music = true;
			if (movingLeft[id]) {
				if (current.y > magnetFloor)
					goalPos.y = magnetFloor;
				else {
					movingLeft[id] = false;
					goalPos.y = magnetCeiling;
				}
			}
			else {
				if (current.y < magnetCeiling)
					goalPos.y = magnetCeiling;
				else {
					movingLeft[id] = true;
					goalPos.y = magnetFloor;
				}
			}
			System.out.println("Holding in position");
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING_LEFT)) {
			this.music = true;
			double xGoal = dimension / 2;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("Moving towards left side");
		}
		else if (piperStatus.equals(PiperStatus.GOING_HOME)) {
			this.music = true;
			double xGoal = dimension / 2 - 10;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("GOING HOME");
		}
		else if (piperStatus.equals(PiperStatus.HUNTING)) {
			int nearestRatIndex = 0;
			double nearestRatDist = (double) 9999999;
			ArrayList<Integer> nearbyRatIndeces = new ArrayList<Integer>();
			for (int i = 0; i < rats.length; i++) {
				Point rat = rats[i];
				double distanceToRat = distance(rat, current);
				boolean otherPiperCloser = false;
				for (int j = nMagnetPipers; j < pipers.length; j++) {
					Point piper = pipers[j];
					if (distance(rat, piper) < distanceToRat)
						otherPiperCloser = true;
				}
				if (distanceToRat < 10)
					nearbyRatIndeces.add(i);
				if (distance(rat, current) < nearestRatDist && !otherPiperCloser && getSide(rat) != 0 && !magnet.contains(rat.x, rat.y) && !doesRatTrajectoryHitMagnet(rats[i], ratThetas[i], dimension)) {
					nearestRatDist = distance(rat, current);
					nearestRatIndex = i;
				}
			}
			goalPos = rats[nearestRatIndex];
			if (distance(goalPos, current) < 10) {
				boolean ratOnPath = false;
				for (int ratIndex : nearbyRatIndeces) {
					if (doesRatTrajectoryHitMagnet(rats[ratIndex], ratThetas[ratIndex], dimension)) {
						ratOnPath = true;
					}
				}
				if (playedLastTurn[id] || ratOnPath) {
					this.music = false;
					playedLastTurn[id] = false;
					System.out.println(id + " Not magnetting");
				}
				else {
					this.music = true;
					playedLastTurn[id] = true;
					System.out.println(id + " Magnetting");
				}
			}
			else {
				this.music = false;
			}
		}
		else if (piperStatus.equals(PiperStatus.SWEEPING_LEFT)) {
			this.music = true;
			double xGoal = dimension / 2;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("Moving towards left side");
		}
		else if (piperStatus.equals(PiperStatus.GOING_HOME)) {
			this.music = true;
			double xGoal = dimension / 2 - 10;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("GOING HOME");
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
	
	public static Point getGoalPointForPiperForRat(Point piper, Point rat, double theta){
		//find distance between the rat and the piper
		double distToRat=distance(rat, piper)-10;
		
		//iterative algorithm:
		//calculate where the rat will be with respect to piper covering initial distance(minus 10)
		double t=distToRat/5;
		double ox = t * Math.sin(theta * Math.PI / 180) + rat.x;
		double oy = t * Math.cos(theta * Math.PI / 180) + rat.y;
		Point projectedRatPoint=new Point(ox,oy);
		//if piper goes to that point, how far will he be in the given time
		double distFromProjectedPoint=distance(piper, projectedRatPoint);
			
		//if within 10, then return point
		while (Math.abs(distFromProjectedPoint-distToRat)>10){
			distToRat=distance(projectedRatPoint, piper);
			t=distToRat/5;
			ox = t * Math.sin(theta * Math.PI / 180) + rat.x;
			oy = t * Math.cos(theta * Math.PI / 180) + rat.y;
			projectedRatPoint.x=ox;
			projectedRatPoint.y = oy;
			distFromProjectedPoint=distance(piper, projectedRatPoint);	
		}
		//else see time it would take to get to rat exactly
		//re do calculation
		return projectedRatPoint;
	}

	public static boolean doesRatTrajectoryHitMagnet(Point rat, double theta, int dimensions){
		Line2D ratLine=new Line2D.Double(rat.x, rat.y, (dimensions*Math.sin(theta * Math.PI / 180) + rat.x), (dimensions*Math.cos(theta * Math.PI / 180) + rat.y));
		return ratLine.intersects(magnet);
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
	
	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}
	
	static boolean isNearEachOther(double a, double b) {
		int TOLERANCE = 1;
		return Math.abs(a-b) < TOLERANCE;
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