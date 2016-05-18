package demos;

import java.io.FileWriter;
import java.io.IOException;

import communication.MyLog;
import models.IzhNetwork;
import models.IzhNetworkSTP;
import models.Robot;
import startup.Constants;
import visualization.Display;

/**
 * An experiemnt where a robot learns to avoid walls.
 * @author lana
 *
 */
public class WallAvoidance {
	/** log*/
	MyLog mlog = new MyLog("wallDemo", true);
	
	IzhNetworkSTP net;
	String name = "";
	/** reaction time sensor left*/
	FileWriter leftTimeWriter;	
	/** reaction time */
	FileWriter rightTimeWriter;
	/** time and value of input for right and left sensor*/
	FileWriter inputWriter;
	/** time and value of output for right and left*/
	FileWriter outputWriter;
	
	
	/**graphics displaying object*/
	Display display;
	/** draw spiking graph or not*/
	boolean drawNet = false;
	/** draw bot or not*/
	boolean drawBot = true;
	/** duration of the experiment*/
	int experimentTime = 1000000;
	
	/**size of input zone*/
	int input_size = 10;
	/** start input from this neuron id*/
	int input_start = 20;
	/**size of output zone*/
	int output_size = 10;
	/** start input from this neuron id*/
	int output_start = 50;
	/**max strength of stimulation*/
	int max_stim_value = 9;//20
	/**unit angle for rotation*/
	double uangle = Math.PI/6;
	/** minimal firing for output (not included)*/
	int min_spikes = 0;//1
	
	/** are we being stimulated continuously*/
	boolean leftStuck;
	/**when did the continuous stimulation start?*/
	int leftStarted;
	/** are we being stimulated continuously*/
	boolean rightStuck;
	/**when did the continuous stimulation start?*/
	int rightStarted;
	
	/**current iteration*/
	int iter = 0;
	
	Robot robot;
	
	NetworkRunnable netThread;
	
	
	/**
	 * Builds the demo
	 * @param n name used for logging.
	 */
	public WallAvoidance(String n){
		name = n;
		init();
		run();	
	}
	
	/**
	 * initialization of all variables.
	 */
	private void init(){
	
		if((name.compareTo("")==0)){
			net = new IzhNetworkSTP(drawNet);
		} else {
			mlog.setName("bot"+name);
			net = new IzhNetworkSTP(name, drawNet);
		}		

		String folderName = net.getDataFolder();
		robot = new Robot(folderName, drawBot);
					
		//now create csv files
		try {					
			String str;
        	inputWriter = new FileWriter(folderName+"/"+Constants.InputValueFileName);
			str = "inputLeft, inputRight, iteration\n";
			inputWriter.append(str);
			inputWriter.flush();
			
			outputWriter = new FileWriter(folderName+"/output.csv");
			str = "outputLeft, outputRight, iteration\n";
			outputWriter.append(str);
			outputWriter.flush();
			
			leftTimeWriter = new FileWriter(folderName+"/leftSensorReacTime.csv");
			str = "startTime, stopTime\n";
			leftTimeWriter.append(str);
			leftTimeWriter.flush();
			
			rightTimeWriter = new FileWriter(folderName+"/rightSensorReacTime.csv");
			str = "startTime, stopTimen";
			rightTimeWriter.append(str);
			rightTimeWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	int stimDelay = 30;
	/**
	 * update the sensory info, neurons and actions
	 */
	private void update(){
//		stimDelay--;
//		
//		int clusFired[] = net.getFiringClusters();
//		int cleft = output_start/10;
//		if(clusFired[cleft]>min_spikes){
//			stimDelay = 500;
//			mlog.say("OK");
//		}
//
//		if(stimDelay < -10000){
//			stimDelay = 500;
//			mlog.say("reached limit");
//		}
//
//		if(stimDelay<0){
//			net.stimulate(input_start,input_size,max_stim_value);
//		}
//	
//		net.updateNeurons();
		
		//get robot's values
		double[] d = robot.getDistSensorsValues();
		
		double stimL = 0;
		double stimR = 0; 
		//stimulate network: left
		if(d[0]>0){
			stimL = max_stim_value/(d[0]-24);
			net.stimulate(input_start,input_size,stimL);		
			if(!leftStuck){
				leftStuck = true;
				leftStarted = iter;
			}
		}else{
			if(leftStuck){//got unstuck
				leftStuck = false;
				//str = "startTime, stopTime\n";
				String str = leftStarted + "," + iter  + "\n";
				try {
		    		leftTimeWriter.append(str);	        	
		    		leftTimeWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
		//right
		if(d[1]>0){
			stimR = max_stim_value/(d[1]-24);
			net.stimulate(input_start+input_size,input_size,stimR);
			if(!rightStuck){
				rightStuck = true;
				rightStarted = iter;
			}
		}else{
			if(rightStuck){//got unstuck
				rightStuck = false;
				//str = "startTime, stopTime\n";
				String str = rightStuck + "," + iter + "\n";
				try {
		    		rightTimeWriter.append(str);	        	
		    		rightTimeWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}			
			}
		}
		
		//net.stimulate(input_start,input_size*2,max_stim_value);
		
		net.updateNeurons();
		
		//update output
		double angle = 0;
		int clusFired[] = net.getFiringClusters();
		int cleft = output_start/10;
		int cright = (output_start+output_size)/10;
		if(clusFired[cleft]>min_spikes){
			//turn left
			angle+=clusFired[cleft]*uangle;
		}
		if(clusFired[cright]>min_spikes){
			//turn right
			angle-=clusFired[cright]*uangle;
		}
		
		robot.turn(angle);	
		
		//write other info
		//"inputLeft, inputRight, iteration\n";
		String str1 = stimL + "," + stimR + "," + iter + "\n";
		//"outputLeft, outputRight, iteration\n";
		String str2 = clusFired[cleft] + "," + clusFired[cright] + "," + iter + "\n";
		try {
    		inputWriter.append(str1);	        	
    		inputWriter.flush();
    		
    		outputWriter.append(str2);	        	
    		outputWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}


	/**
	 * close output files.
	 */
	void closeStreams(){
		mlog.say("closing streams");
		try {
			inputWriter.flush();
			inputWriter.close();		
			
			outputWriter.flush();
			outputWriter.close();
			
			rightTimeWriter.flush();
			rightTimeWriter.close();
			
			leftTimeWriter.flush();
			leftTimeWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void  run(){
		netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	/**
	 * main thread that runs the demo.
	 * @author lana
	 *
	 */
	private class NetworkRunnable implements Runnable{
		boolean run = true;
		MyLog mlog = new MyLog("networkRunnable",true);
		
		public void kill(){
			run = false;
		}
		
		public void run() {			
			
			while(run){				
				update();
				robot.goForward(1);
				iter++;
				
				if(iter>=experimentTime){
					mlog.say("end of experiment"+net.getName()+" at t="+iter);
					kill();
				}				
				
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}			
			}
			closeStreams();
		}
	}


	/**
	 * kill this thread
	 */
	public void globalKill() {
		mlog.say("kill");
		netThread.kill();
	}
}
