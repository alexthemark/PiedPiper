package piedpipers.group2;

import java.awt.geom.Line2D;
import java.util.*;

import piedpipers.sim.Piedpipers;
import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	enum PiperStatus {
		GOING_TO_GATE, MOVING_TO_POSITION, IN_POSITION, SWEEPING_LEFT, HUNTING, GOING_HOME
	}
	
	static int npipers;
	
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	
	static Point target = new Point();
	
	static boolean initi = false;
	PiperStatus piperStatus;
	static boolean allRatsCaptured = false;
	static double percentMagnetPipers = .8;
	static boolean[] magnetPipers;
	static Point[] magnetPiperPositions;
	static int nMagnetPipers;
	static int magnetFloor;
	static int magnetCeiling;
	static ThetaCalc ratThetas;
	static boolean[] playedLastTurn;
	static boolean[] movingLeft;
	static boolean[] inPosition;
	
	public void init() {
		piperStatus = PiperStatus.GOING_TO_GATE;
		nMagnetPipers = (int) (percentMagnetPipers * npipers);
		magnetPipers = new boolean[npipers];
		int firstMagnetX = (3*dimension/4) - (20 * nMagnetPipers / 2);
		int firstMagnetY = dimension/2 - (20 * nMagnetPipers / 2);
		magnetFloor = firstMagnetX;
		magnetCeiling = magnetFloor + 10* nMagnetPipers;
		magnetPiperPositions = new Point[nMagnetPipers];
		for (int i = 0; i < nMagnetPipers; i++) {
			magnetPipers[i] = true;
			magnetPiperPositions[i] = new Point(firstMagnetX + 20 * i, firstMagnetY + 20 * i);
		}
		playedLastTurn = new boolean[npipers];
		movingLeft = new boolean[npipers];
		inPosition = new boolean[nMagnetPipers];
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
				if (magnetPipers[id])
					piperStatus = PiperStatus.MOVING_TO_POSITION;
				else
					piperStatus = PiperStatus.HUNTING;
			}
		}
		else if (piperStatus.equals(PiperStatus.HUNTING)){
			if (allRatsCaptured) {
				piperStatus = PiperStatus.SWEEPING_LEFT;
			}
		}
		else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) {
			// Piper has made it to their starting position
			if (isNearEachOther(currentLocation.x, magnetPiperPositions[id].x)) {
				piperStatus = PiperStatus.IN_POSITION;
				inPosition[id] = true;
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
	
	int ratsCaptured(Point[] pipers, Point[] rats) {
		//this function currently has a bug in that it doesn't check if the pipers are playing.
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
		if (magnetPipers[id]) {
			// Get the pied pipers over to the right side
			if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
				this.music = false;
				System.out.println("move toward the right side");
			} 
			// Get the pied pipers into their starting net positions
			else if (piperStatus.equals(PiperStatus.MOVING_TO_POSITION)) { 
				goalPos = magnetPiperPositions[id];
				this.music = false;
				System.out.println("move into starting positions");
			}
			else if (piperStatus.equals(PiperStatus.IN_POSITION)) {
				goalPos = magnetPiperPositions[id];
				this.music = true;
				if (true) {
						if (movingLeft[id]) {
							if (current.x > magnetFloor)
								goalPos.x = magnetFloor;
							else {
								movingLeft[id] = false;
								goalPos.x = magnetCeiling;
							}
						}
						else {
							if (current.x < magnetCeiling)
								goalPos.x = magnetCeiling;
							else {
								movingLeft[id] = true;
								goalPos.x = magnetFloor;
							}
						}
				}
				System.out.println("Holding in position");
			}
			// Bring the pipers/rats into the center
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
		}
		// we're a hunting piper
		else {
			// Get the pied pipers over to the right side
			if (piperStatus.equals(PiperStatus.GOING_TO_GATE)) {
				this.music = false;
				System.out.println("move toward the right side");
			}
			else if (piperStatus.equals(PiperStatus.HUNTING)) {
				int nearestRatIndex = 0;
				double nearestRatDist = Double.MAX_VALUE;
				ArrayList<Integer> nearbyRatIndeces = new ArrayList<Integer>();
				for (int i = 0; i < rats.length; i++) {
					Point rat = rats[i];
					if (distance(rat, current) < 10)
						nearbyRatIndeces.add(i);
					if (distance(rat, current) < nearestRatDist && !capturedByMagnet(rat, pipers)) {
						nearestRatDist = distance(rat, current);
						nearestRatIndex = i;
					}
				}
				goalPos = getGoalPointForPiperForRat(current, rats[nearestRatIndex], ratThetas[nearestRatIndex]);
				if (distance(goalPos, current) < 10) {
					boolean ratOnPath = false;
					for (int ratIndex : nearbyRatIndeces) {
						if (doesRatTrajectoryHitMagnet(magnetFloor, magnetCeiling, magnetFloor, magnetCeiling, rats[ratIndex], ratThetas[ratIndex], dimension)) {
							ratOnPath = true;
						}
					}
					if (playedLastTurn[id] || ratOnPath) {
						this.music = false;
						playedLastTurn[id] = false;
						for (int i = 0; i < 50; i++) {
							System.out.println(id + " Not magnetting");
						}
					}
					else {
						this.music = true;
						playedLastTurn[id] = true;
						System.out.println(id + " Magnetting");
					}
				}
				else
					this.music = false;
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
	
	static boolean capturedByMagnet(Point rat, Point[] pipers) {
		int pipersHolding = 0;
		for (Point piper : pipers) {
			if (distance(rat, piper) < 10)
				pipersHolding++;
		}
		if (pipersHolding > 1)
			return true;
		return false;
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

	public static boolean doesRatTrajectoryHitMagnet(int x1, int y1, int x2, int y2, Point rat, double theta, int dimensions){
		Line2D line1=new Line2D.Double(x1, y1, x2, y2);
		Line2D line2=new Line2D.Double(rat.x, rat.y, (dimensions*Math.sin(theta * Math.PI / 180) + rat.x), (dimensions*Math.cos(theta * Math.PI / 180) + rat.y));
		return line1.intersectsLine(line2);
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