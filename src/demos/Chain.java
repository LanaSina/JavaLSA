package demos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import models.IzhNeuron;
import startup.Constants;
import communication.MyLog;

public class Chain {

	/** log */
	MyLog mlog = new MyLog("ChainExp", true);
	String name = "";
	//3 neurons
	int numberOfNeurons = 3;
	IzhNeuron[] neurons = new IzhNeuron[numberOfNeurons];
	double[][] weights = new double[numberOfNeurons][numberOfNeurons];
	
	/** stdp window */
	int tau = Constants.tau;
	/**stdp tau*/
	int[] STDPcount;
	
	/**random normal generator*/
	Random noiseRand = new Random();
	
	/** weight range*/
	double weightsRange = 15;
	/** maximum weight*/
	double maxWeight = 50;
	/**noise range (gaussian std deviation)*/
	
	double noise = 5;	
	/** simulation time (age of the network)*/
	int iteration = 0;
	
	/**data directory*/
	String folderName;
	/** records weights separately*/
	int wrLength = numberOfNeurons*(numberOfNeurons-1);
	FileWriter[] weightsWriters = new FileWriter[wrLength];
	/** spikes time series */
	FileWriter spikesWriter;
	
	int stimDelay = 0;
	int stimDelay2 = 0;
	double stim_value = 5;
	boolean stim = true;
	boolean stim2 = false;
	int runTime = 200000;
	/** firing rates*/
	LinkedList<Integer> rates[];
	int rateWindow = 300;
	
	
	public Chain(){
		init();
	}
	
	public Chain(String name){
		this.name = name;
		init();
	}
	
	private void init(){
		
		if((name.compareTo("")==0)){
			//get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    	folderName = Constants.DataPath + "\\" + strDate + "\\";
	    }else{
	    	mlog.setName("chain"+name);
	    	folderName = Constants.DataPath + "\\net" +name + "\\";
	    }
		
		STDPcount = new int[numberOfNeurons];
		rates = (LinkedList<Integer>[])new LinkedList<?>[numberOfNeurons];
	
//		for(int i=0;i<numberOfNeurons;i++){
//			rates[i] = new LinkedList<Integer>();//)//new LinkedList<?>();
//		}
			    
	    initNetwork();
		initDataFiles();
		writeWeights();	
		
		NetworkRunnable net = new NetworkRunnable();
		(new Thread(net)).run();
	}
    
	/**
	 * Initializes the network and the neurons
	 */
	void initNetwork(){

	    for(int i=0; i<numberOfNeurons; i++){
	    	neurons[i] = new IzhNeuron();
			neurons[i].setNeuronType(Constants.NeurExcitatory);
//			for(int j=0; j<rateWindow; j++){
//				rates[i].add(0);
//			}
	    }
	   // neurons[1].setNeuronType(Constants.NeurInhibitory);
	    
		//initialization of weights to 0
		for(int i=0; i<numberOfNeurons; i++){
	        for(int j=0; j<numberOfNeurons; j++){
				weights[i][j] = 0;
			}
		}
	
//		weights[0][2] = 5;//weightsRange;
//		weights[2][0] = 5;//weightsRange;
		
	    for(int i=0; i<numberOfNeurons; i++){
			for(int j=0; j<numberOfNeurons; j++){
				if(i!=j){			
						weights[i][j] = weightsRange;
				}			
			}
	    }
	    
	    weights[2][0] = 0;
	    
//		weights[1][2] = -weightsRange;
//		weights[1][0] = -weightsRange;
//	    
	    iteration = 0;
	}
	
	/** create data files*/
	void initDataFiles(){
	    
	    //first create directory
		File theDir = new File(folderName);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    mlog.say("creating directory: " + folderName);
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
		
		openStream();
		
		//now create csv files
		try {			
			spikesWriter = new FileWriter(folderName+"\\"+Constants.SpikesFileName);
			mlog.say("stream opened "+Constants.SpikesFileName);
        	String str = "neuronID,iteration\n";
        	spikesWriter.append(str);
        	spikesWriter.flush();
			
			/*reacTimeWriter = new FileWriter(folderName+"\\"+Constants.ReacTimeFileName);
			mlog.say("stream opened "+Constants.ReacTimeFileName);
			str = "reactionTime,iteration\n";
        	reacTimeWriter.append(str);
        	reacTimeWriter.flush();*/
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	/** record weights in csv file*/
	void writeWeights(){
		//openStream();
		
		try {
			int a = 0;
			for(int i=0; i<numberOfNeurons;i++){
				for(int j=0; j<numberOfNeurons;j++){
					if(j!=i){
						String str = iteration+","+weights[i][j]+"\n";
						weightsWriters[a].append(str);
						weightsWriters[a].flush();
						a++;
					}
				}				
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	void openStream(){		
		String weightsFileName = "weight_";
		
		try {
			int a = 0;
			for(int i=0; i<numberOfNeurons;i++){
				for(int j=0; j<numberOfNeurons;j++){
					if(j!=i){
						String fname = weightsFileName + i +"_"+j+".csv";
						weightsWriters[a] = new FileWriter(folderName+"\\"+fname);
						String str = "iteration,w\n";
						weightsWriters[a].append(str);
						weightsWriters[a].flush();
						a++;
					}
				}				
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		mlog.say("stream opened "+weightsFileName);
	}
	
	/** update all neurons and their interactions*/
	public synchronized void updateNeurons(){		

	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].checkFiring();
	        //rates[i].removeFirst();
	        if(neurons[i].isFiring()) {
	        	String str = i+","+iteration+"\n";
	        	//rates[i].add(1);	        	
	        	try {
	        		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }else{
	        	//rates[i].add(0);
	        }
	    }

	    STDP();

		for(int i=0; i<numberOfNeurons; i++){
			int sum = 0;
//			for(int j=0; j<rateWindow; j++){
//				sum+= rates[i].get(j);
//			}
//			if(sum==0){
//				sum = 1;
//			}
			double inp = noise;///(sum/2.0);
		//	mlog.say("noise "+inp);
			
			neurons[i].addToI(inp*noiseRand.nextGaussian());
	        for(int j=0; j<numberOfNeurons; j++){
	            if((i != j) && neurons[i].isFiring())
	            	neurons[j].addToI(weights[i][j]);
	        }        
	    }
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].update();
	        neurons[i].setI(0);
	    }
	    
		iteration++;
	   // if((iteration%10000) == 0){	    	
			writeWeights();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		//	mlog.say("step "+iteration);
	   // }	    

	}

	void STDP(){
	    
		//reinitialize tau for the neurons that have fired
	    for(int i=0; i<numberOfNeurons; i++){
	        if(STDPcount[i]>0) STDPcount[i]--;
	        if(neurons[i].isFiring()) STDPcount[i] = tau;
	    }

		for(int i=0; i<numberOfNeurons; i++){

	        if(neurons[i].isFiring()){
				for(int j=0; j<numberOfNeurons; j++){			
	                if((i != j)){// & (i!=1) & (j!=1)){
						double d = (0.1*Math.pow(0.95, (tau-STDPcount[j])));
						//check that neuron linked to i fired less than tau_ms before (but is not firing right now)
						//if j fired before i, then weight j->i ++
	                    if((weights[j][i] != 0.0) && (STDPcount[j]>0) && (STDPcount[j] != tau)){  					
							weights[j][i] += d;
	                    }
	                    
						//now weight from i to j, should be lowered if i fired before j
	                    if(weights[i][j] != 0.0 && STDPcount[j]>0  && STDPcount[j] != tau){
	                        weights[i][j] -= d;
	                    }

	                    if (weights[i][j] > maxWeight) weights[i][j] = maxWeight;
	                    else if(weights[i][j] <= 0) weights[i][j] = 0;
	                    
	                    if (weights[j][i] > maxWeight) weights[j][i] = maxWeight;
	                    else if(weights[j][i] <= 0) weights[j][i] = 0;
	                }
	            }
	        }
	    }
	}
	
	void broadStimulation(){
		
		stimDelay--;
		stimDelay2--;
		boolean c1=false;
		boolean c2=false;
		
		if(neurons[2].isFiring()){
			c1 = true;
			//c2 = true;
		}
		
		if(neurons[0].isFiring()){
			c2 = true;
		}

		if(c1){
			stim2 = true;
			stimDelay2 = 20;
		}
		
		if(c1){
			stim = false;
			mlog.say("OK, rt "+ stimDelay+" \n");
			stimDelay = 100;
		}

		if(stimDelay < -10000){
			stimDelay = 100;
			stim = false;
			mlog.say("reached limit \n");
		}

//		if((stimDelay<0)){//stim && 
//			neurons[0].addToI(stim_value);
//		}
		
		if(stimDelay2>0){
			neurons[0].addToI(stim_value);
		}
		
	}
	
	public void closeStreams(){
		mlog.say("closing streams");
		try {
			spikesWriter.flush();
			spikesWriter.close();	
			
			int a = 0;
			for(int i=0; i<numberOfNeurons;i++){
				for(int j=0; j<numberOfNeurons;j++){
					if(j!=i){
						weightsWriters[a].flush();
						weightsWriters[a].close();
						a++;
					}
				}				
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class NetworkRunnable implements Runnable{
		boolean run = true;
		MyLog mlog = new MyLog("networkRunnable",true);
		
		public void kill(){
			run = false;
		}
		
		//@Override
		public void run() {
			
			while(run){
				
				updateNeurons();
				broadStimulation();
				
				if(iteration>=runTime){
					mlog.say("end of experiment at t="+iteration);
					kill();
				}
				
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
			closeStreams();
		}
	}

}
