package demos;

import java.io.FileWriter;
import java.io.IOException;

import communication.MyLog;
import models.IzhNetwork;
import models.IzhNetworkSTP;
import models.Robot;
import startup.Constants;
import visualization.Display;

public class SimpleExperiment2 {
	/** log*/
	MyLog mlog = new MyLog("wallDemo", true);
	
	IzhNetwork net;
	String name = "";
	/** reaction time sensor left*/
	FileWriter realRateWriter;	
	/** reaction time */
	FileWriter dummyRateWriter;
	/** time and value of input for right and left sensor*/
	FileWriter inputWriter;
	/**reaction time*/
	FileWriter reacTimeWriter;
	
	
	/**graphics displaying object*/
	Display display;
	/** draw spiking graph or not*/
	boolean drawNet = false;
	/** duration of the experiment*/
	int experimentTime = 500000;
	
	/**size of input zone*/
	int input_size = 10;
	/** start input from this neuron id*/
	int input_start = 20;
	/**size of output zone*/
	int output_size = 10;
	/** start input from this neuron id*/
	int output_start = 50;
	/**max strength of stimulation*/
	int stim_value = 1;
	/**unit angle for rotation*/
	double uangle = Math.PI/6;
	/** minimal firing for output (not included)*/
	int min_spikes = 3;
	
	/** are we being stimulated continuously*/
	boolean stuck;
	boolean stim = false;
	
	/**current iteration*/
	int iter = 0;
	NetworkRunnable netThread;
	String supFolder = "";
	double wvar = 0;
	int connections = 100;
	
	//
	//String rootFolder = "";
		
	public SimpleExperiment2(String n){
		name = n;
	}
	
	public void setSupFolder(String f){
		supFolder = f;
	}
	
	public void setConnections(int c){
		connections = c;
	}
	
	public void setWVar(double v){
		wvar = v;
	}
	
	public void makeNet(){		
		if((name.compareTo("")==0)){
			net = new IzhNetwork(drawNet);
		} else {
			mlog.setName("bot"+name);
			net = new IzhNetwork(name, drawNet);
		}		
		net.setWVar(wvar);
		net.setConnections(connections);
		net.init(supFolder);	
	}
	
	public void launch(){
		init();
		run();
	}
	
	private void init(){
		String folderName = net.getDataFolder();
					
		//now create csv files
		try {					
			String str;
        	inputWriter = new FileWriter(folderName+"/"+Constants.InputValueFileName);
			str = "input, iteration\n";
			inputWriter.append(str);
			inputWriter.flush();
			
			reacTimeWriter = new FileWriter(folderName+"/reacTime.csv");
			str = "reactionTime, iteration\n";
			reacTimeWriter.append(str);
			reacTimeWriter.flush();
			
			realRateWriter = new FileWriter(folderName+"/realRate.csv");
			str = "rate, iteration\n";
			realRateWriter.append(str);
			realRateWriter.flush();
			
			dummyRateWriter = new FileWriter(folderName+"/dummyRate.csv");
			str = "rate, iteration\n";
			dummyRateWriter.append(str);
			dummyRateWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	int stimDelay = 30;
	/**
	 * update the sensory info, neurons and actions
	 */
	private void update(){
		
		stimDelay--;
		int iteration = net.getIteration();
		if(stimDelay<0){
			stim = true;
		}
		
		/**conditions*/
		boolean c1=false;
		int clusFired[] = net.getFiringClusters();
		
		if(clusFired[3]>3){
			c1 = true;
		}
		
		String strR = clusFired[3]+","+iteration+"\n";
		String strD = clusFired[4]+","+iteration+"\n";
    	try {
    		realRateWriter.append(strR);	        	
    		realRateWriter.flush();
    		dummyRateWriter.append(strD);
    		dummyRateWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if(c1){
			if(stim){
				stim = false;
				String str = stimDelay+","+iteration+"\n";
				stimDelay = (int) (Constants.uniformDouble(1000,2000) +0.5);		
	        	try {
	        		reacTimeWriter.append(str);	        	
					reacTimeWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}			
			}
		}
    	
    	if(stimDelay <= -10000){
			String str = -stimDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stimDelay = 500;
			stim = false;
			mlog.say("reached limit \n");
		}

    	String str = "";
		if(stim){
			net.stimulate(input_start, input_size, stim_value);
			str = stim_value+","+iteration+"\n";	
		}else{
			str = 0+","+iteration+"\n";	
		}
		try {
    		inputWriter.append(str);	        	
    		inputWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
    					
		net.updateNeurons();	
	}

	void closeStreams(){
		mlog.say("closing streams");
		try {
			inputWriter.flush();
			inputWriter.close();		
			
			reacTimeWriter.flush();
			reacTimeWriter.close();
			
			realRateWriter.flush();
			realRateWriter.close();
			
			dummyRateWriter.flush();
			dummyRateWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void  run(){
		netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	private class NetworkRunnable implements Runnable{
		boolean run = true;
		MyLog mlog = new MyLog("networkRunnable",true);
		
		public void kill(){
			run = false;
		}
		
		public void run() {			
			
			while(run){				
				update();
				iter++;
				
				if(iter>=experimentTime){
					mlog.say("end of experiment"+net.getName()+" at t="+iter);
					kill();
				}				
				
//				try {
//					Thread.sleep(2);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}			
			}
			closeStreams();
		}
	}


	public void globalKill() {
		mlog.say("kill");
		netThread.kill();
	}
}
