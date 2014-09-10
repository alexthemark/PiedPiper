package piedpipers.group2;

import java.util.*;

import piedpipers.sim.Point;

public class Player extends piedpipers.sim.Player {
	static int npipers;
	
	static double pspeed = 0.49;
	static double mpspeed = 0.09;
	
	static Point target = new Point();
	static int[] thetas;
	static boolean finishround = true;
	
	static boolean initi = false;
	
	public void init() {
		thetas = new int[npipers];
	}

	static double distance(Point a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}
	
	static boolean isNearEachOther(double a, double b) {
		int TOLERANCE = 1;
		return Math.abs(a-b) < TOLERANCE;
	}

	public Point move(Point[] pipers, // positions of pipers
			Point[] rats) { // positions of the rats
		npipers = pipers.length;
		System.out.println(initi);
		Point gate = new Point(dimension/2, dimension/2);
		if (!initi) {
			this.init();
			initi = true;
		}
		Point current = pipers[id];
		double ox = 0, oy = 0;
		Point goalPos = new Point(gate); // default position to move to is the gate
		// Get the pied pipers over to the right side
		if (getSide(current) == 0 && !this.music) {
			finishround = true;
			this.music = false;
			Random random = new Random();
			int theta = random.nextInt(180);
			thetas[id]=theta;
			System.out.println("move toward the right side");
		} 
		// Get the pied pipers into their starting net positions
		else if (!closetoWall(current) && finishround && !this.music) { 
			double yGoal = id % 2 == 0 ? 0 : dimension;
			double xGoal = dimension / 2 + (((dimension - 10)* id + 1) / (2 * pipers.length) + 10);
			goalPos = new Point(xGoal, yGoal);
			this.music = false;
			System.out.println("move into starting positions");
		}
		// Bring the pipers/rats into the center
		else if (finishround && !isNearEachOther(current.y, (double) dimension/2)) {
			double xGoal = dimension / 2 + (((dimension - 20)* id + 1) / (2 * pipers.length) + 1);
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			this.music = true;
			System.out.println("pinching towards center");
		}
		// Bring the rats through the gate and into the place they go
		else if (finishround && isNearEachOther(current.y, (double) dimension/2) 
				&& !isNearEachOther(current.x, dimension/2 - 10)) {
			this.music = true;
			double xGoal = dimension / 2 - 10;
			double yGoal = dimension / 2;
			goalPos = new Point(xGoal, yGoal);
			System.out.println("Moving towards left side");
		}
		else {
			finishround = false;
			this.music = false;
			System.out.println("resetting");
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