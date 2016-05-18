package models;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.FileWriter;
import java.io.IOException;

import Jama.Matrix;
import communication.MyLog;
import startup.Constants;
import visualization.Display;
import visualization.GraphicalComponent;

/**
 * A small wheeled robot with distance sensors.
 * 
 * @author lana
 *
 */
public class Robot implements GraphicalComponent {
	/** log*/
	MyLog mlog = new MyLog("Robot", true);
	/** write experiment's data to output folder or not*/
	boolean save = false;
	
	//graphics
	/**graphics displaying object*/
	Display display;
	/**shoud bot be drawn or not*/
	boolean draw = true;
	/** bot tracker */
	FileWriter trackerWriter;
	
	/**arena size**/
	int arena_size = 1000;//1000
	/**offset from panel*/
	int[] arena_offset = {50,50};
	
	/**robot position (in pixels, convert to int each time not to loose precision)**/
	double[] coordinates = new double[2];
	/**robot size (total diameter)**/
	int robot_size = 50;//50;
	
	/**robot orientation angle (rad)*/
	double r_angle = Math.PI/6;
	/**robot orientation: normalized matrix (robot's coordinates system). 
	 * starts facing up
	 * field 0,0 = top left*/
	Matrix orientation;
	double[][] f = {{0},
					{1}};
	/**just a vector pointing default front (y)*/
	Matrix front = new Matrix(f);
	/**vector pointing current direction of the robot*/
	Matrix robot_front = new Matrix(f);

	
	/** size of drawn vectors, in pixels*/
	int frontv_size = 50;
	/** distance sensor's range (pixels)*/
	int distance_srange = 80;//80;
	
	//sensors
	DistanceSensor[] distSensors = new DistanceSensor[2];
	
	/**iteration*/
	int iter = 0;
	
	/**
	 * Builds a robot
	 * @param folderName name of data folder
	 * @param d draw the bot or not
	 * @param save write experiment's data to output file or not
	 */
	public Robot(String folderName, boolean d, boolean save){
		draw = d;
		this.save = save;
		
		if(draw){
			display = new Display();
			display.setName("Robot");
			display.addComponent(this);
		}
		
		coordinates[0] = 250;
		coordinates[1] = 250;
		
		orientation = makeRotationMatrix(r_angle);
		robot_front = orientation.times(front);
		
		//left
		distSensors[0] = new DistanceSensor(Math.PI/4);
		//right
		distSensors[1] = new DistanceSensor(-Math.PI/4);
		
		mlog.say("distance left "+distSensors[0].getValue());
		mlog.say("distance right "+distSensors[1].getValue());
		
		if(save){		
			String str;
	    	try {
				trackerWriter = new FileWriter(folderName+"/robotTrack.csv");
				str = "iteration, x, y, frontAngle\n";
				trackerWriter.append(str);
				trackerWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * move forward by x (pixels)
	 */
	public void goForward(double x){
		//calculate forward vector
		Matrix f = robot_front.times(x);
		//add to coordinates
		coordinates[0]+=f.get(0, 0);
		coordinates[1]+=f.get(1, 0);
		//check limit conditions
		cropToLimits(coordinates, robot_size/2);
		if(save){
			writeInfo();
		}
		iter++;
	}
	
	/**
	 * write data in output file.
	 */
	private void writeInfo(){
		//str = "x, y, frontAngle\n";
		String str = iter + "," + coordinates[0] + "," + coordinates[1] + "," + Math.toDegrees(r_angle) + "\n";
		try {
			trackerWriter.append(str);	        	
			trackerWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * makes the robot turn counter-clockwise
	 * @param a angle in radian
	 */
	public void turn(double a){
		r_angle+=a;
		//r_angle = cropAngleToLimits(r_angle);
		orientation = makeRotationMatrix(r_angle);
		robot_front = orientation.times(front);
	}
	
	/**
	 * makes the angle go back to 0, 2pi
	 * @param a
	 * @return
	 */
	private double cropAngleToLimits(double a) {
		double r = a%(2*Math.PI);
		return r;
	}

	/**
	 * crops a 2d vector to keep it in arena limits
	 * @param v coordinates of center of object
	 * @param o_size object's radius
	 * @return [0,0] or [(signed)collision range x, (signed)collision range y]
	 */
	private int[] cropToLimits(double[] v, int o_size) {
		int[] r = {0,0};
		int lowLimit = 0+o_size;
		int highLimit = arena_size-o_size;// /2
		for(int i=0; i<v.length; i++){
			if(v[i]<lowLimit){
				r[i] = (int) (v[i]-lowLimit);
				v[i] = lowLimit;
			}else if(v[i]>highLimit){
				r[i] = (int) (v[i]-highLimit);
				v[i] = highLimit;
			}
		}
		
		return r;
	}

	/**
	 * the robot's sensor to detect walls,
	 * @author lana
	 *
	 */
	private class DistanceSensor{
		/** angle from front (rad)*/
		double angle = 0;
		/** rotation matrix */
		Matrix m;
		
		/**
		 * 
		 * @param a angle relative to the front of the robot, in radian
		 */
		public DistanceSensor(double a){
			angle = a;
			m = makeRotationMatrix(a);
		}
		
		/**
		 * distance is calculated from robot's center.
		 * @return distance from sensor if < range, -1 if >range.
		 */
		public double getValue(){
			double d = -1;
			
			//check distance from each wall
			double dist = (double)distance_srange;
			//angle sensor-y axis
			//mlog.say("angle s "+Math.toDegrees(angle));
			double beta = angle+r_angle;
			
			//wall left
			double s = Math.sin(beta);
			if(s>0){
				double d2 =  coordinates[0]/(s); //never 0
				//mlog.say("dleft "+d2);
				//distance from wall left
				dist = d2;
			}
			//right
			if(s<0){
				double d2 =  (arena_size-coordinates[0])/(-s);//never 0
				//mlog.say("dright "+d2);
				dist = d2;
			}
			
			//top
			//mlog.say("beta y "+Math.toDegrees(beta));
			beta = beta + (Math.PI / 2);//angle with x
			//mlog.say("beta x "+Math.toDegrees(beta));
			s = Math.sin(beta);
			if(s>0){
				double d2 =  (arena_size-coordinates[1])/s;
				//mlog.say("dtop "+d2);
				//distance from top
				if(d2<dist)
					dist = d2;
			}
			//distance from down
			if(s<0){
				double d2 =  (coordinates[1])/(-s);//never 0
				//mlog.say("ddown "+d2);
				if(d2<dist)
					dist = d2;
			}
			
			
			if(dist<distance_srange)
				d = dist;

			return d;//SHOULD ALWAYS BE POSITIVE
		}
		
		public void draw(Graphics2D g2d){
			g2d.setColor(Color.red);
			
			int[] startPoint = toPanelCoordinates(coordinates);
			//end point of the vector = rotation*coordinateSystem*frontVector*drawingSize
			Matrix vec = m.times(robot_front.times(distance_srange));
			//do not use panel-wise coordinates directly
			double[] endPoint = {(vec.get(0, 0)+coordinates[0]), (vec.get(1, 0)+coordinates[1])};
			int[] end = toPanelCoordinates(endPoint);
			g2d.drawLine(startPoint[0],startPoint[1],end[0], end[1]);
		}
	}
	
	/**
	 * 
	 * @param a angle
	 * @return rotation matrix
	 */
	private Matrix makeRotationMatrix(double a){
		double[][] ro = {{Math.cos(a), -Math.sin(a)},	//this is line
				{Math.sin(a), Math.cos(a)}};
		Matrix r = new Matrix(ro);
		return r;
	}
	
	/** transforms regular x y system into weird JPanel system
	 * regular: x left right, y bottom up
	 * JPanel: x left right, y top down -> non regular, screws up all rotations.
	 * @param c 2D coordinates 
	 * @return
	 */
	private int[] toPanelCoordinates(double[] c){
		int[] r = {(int)(c[0]+arena_offset[0]), (int)(arena_offset[1]+arena_size-c[1])};
		return r;
	}
	
	/**
	 * 
	 * @return array of sensor values (-1 if sensor not ativated);
	 */
	public double[] getDistSensorsValues(){
		double[] d = new double[distSensors.length];
		for(int i=0; i<distSensors.length ;i++){
			d[i] = distSensors[i].getValue();
		}
		
		return d;
	}
	
	@Override
	public void draw(Graphics g, int gridStep) {
		
		Graphics2D g2d = (Graphics2D) g;

		//cleanup
		g2d.clearRect(0,0,1500,1000);

		//draw arena
		g2d.setColor(Color.black);
		g2d.draw(new Rectangle2D.Double(arena_offset[0],arena_offset[1],arena_size,arena_size));
		
		//draw robot
		g2d.setColor(Color.blue);
		
		//upper left corner of circle
		double[] startPoint = {(coordinates[0] - (robot_size/2.0)), (coordinates[1] + (robot_size/2.0))};
		int[] rs = toPanelCoordinates(startPoint);		
		//g2d.fillOval(coordinates[0],coordinates[1], robot_size,robot_size);
		g2d.fillOval(rs[0],rs[1], robot_size,robot_size);
		
		//draw front
		g2d.setColor(Color.green);
		Matrix sfront = robot_front.times(frontv_size);
		//startPoint = offsetIntVector(coordinates);
		rs = toPanelCoordinates(coordinates);
		double[] endPoint = {(coordinates[0]+sfront.get(0, 0)),(coordinates[1]+sfront.get(1, 0))};
		int[] end = toPanelCoordinates(endPoint);
		g2d.drawLine(rs[0],rs[1], end[0], end[1]);
		
		//draw distance sensors
		for(int i=0; i<distSensors.length; i++){
			distSensors[i].draw(g2d);
		}
	}

}
