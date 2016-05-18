package demos;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import communication.MyLog;
import models.IzhNeuron;
import startup.Constants;

/**
 * Test the optimal mean / max weight for bistable neural assemblies
 * @author lana
 *
 */
public class NeuralAssembly {

	/** log */
	MyLog mlog = new MyLog("Assembly", true);
	
	/** total number of neurons */
	int numberOfNeurons = 50;//in the assembly 
	
	/** simulation time (age of the network)*/
	int iteration = 0;
	/** array of neurons representing the network */
	IzhNeuron[] neurons;
	/** synaptic weights*/
	double[][] weights;
	//frequency
	int e_sum = 0;	
	
	//STP according to Science
	double[] sci_u;
	double[] sci_x;
	double[] wf = new double[numberOfNeurons];
	
	/**random normal generator*/
	Random noiseRand = new Random();	
	/** assembly weights */
	double strongW = 10;
	/** not assembly*/
	double weakW = 5;
	/**noise range (gaussian std deviation)*/
	double noise = 4;//5
	
	//stim
	/**is the network being stimulated or not*/
	boolean stim = false;
	int stimDelay = 500;
	
	/**data directory*/
	String folderName;
	/** records weight matrix*/
	FileWriter weightsWriter;
	//csv files
	/** spikes time series */
	FileWriter spikesWriter;	

	public NeuralAssembly(){
		init();
		NetworkRunnable netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	private void init(){

		neurons = new IzhNeuron[numberOfNeurons];
		weights = new double[numberOfNeurons][numberOfNeurons];
		sci_x = new double[numberOfNeurons];
		sci_u = new double[numberOfNeurons];

	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);

	    folderName = Constants.DataPath + "/" + strDate + "/";

	    
	    initNetwork();
		initDataFiles();
		//writeWeights();		
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
		
		//now create csv files
		try {			
			spikesWriter = new FileWriter(folderName+"/"+Constants.SpikesFileName);
			mlog.say("stream opened "+Constants.SpikesFileName);
        	String str = "neuronID,iteration\n";
        	spikesWriter.append(str);
        	spikesWriter.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Initializes the network and the neurons
	 */
	void initNetwork(){
		

		//init neurons
	    for(int i=0; i<numberOfNeurons; i++){
	    	neurons[i] = new IzhNeuron();	  
			neurons[i].setNeuronType(Constants.NeurExcitatory);
	    }
	    
		//initialization of weights to 0
		for(int i=0; i<numberOfNeurons; i++){			
	        for(int j=0; j<numberOfNeurons; j++){
				weights[i][j] = 0;
			}
		}
	
		//random weights
	    for(int i=0; i<numberOfNeurons; i++){
			for(int j=0; j<numberOfNeurons; j++){
				if(i<30 && j<30){//assembly
					weights[i][j] = strongW;
				} else{
					weights[i][j] = weakW;
				}
			}
	    }
	    
	    iteration = 0;    
	}
	
	/** update all neurons and their interactions*/
	public synchronized void updateNeurons(){		
		//current spikes
		int[] currentSpikes = new int[numberOfNeurons];

	    for(int i=0; i<numberOfNeurons; i++){
	    	
	        neurons[i].checkFiring();
	        if(neurons[i].isFiring()) {
	        	currentSpikes[i] = 1;	        	
	        	//write spike in file
				String str = i+","+iteration+"\n";
		    	try {
		    		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else{
	        	currentSpikes[i] = 0;
	        }
	    }
	    
		for(int i=0; i<numberOfNeurons; i++){
			neurons[i].addToI(noise*noiseRand.nextGaussian());
			
			wf[i] = sciSTP(i);
			if(currentSpikes[i] == 1){		
		        for(int j=0; j<numberOfNeurons; j++){
		            if(i != j){
		            	neurons[j].addToI(wf[i]*weights[i][j]);
		            }     	
		        } 
			}				              
	    }
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].update();
	        neurons[i].setI(0);
	    }
	    
		iteration++;
		
	    //stimulation
		//stimulate 10 times in one seconds
		if(iteration>5000 && iteration<6000){
			int r = iteration%100;//(10Hz)
			if(r<30){ 
				stimulate(0+r,1,50);
			}
			mlog.say(" wk 25 " + wf[25]);
		}
//		
//		if(iteration<300000){ //
//			if(r<=1000){
//				int r2 = r%100;//(10Hz)
//				if(r2<30){ 
//					stimulate(0+r2,1,50);
//				}
//			}
//		}
		
	}
	
	//STP according to Science paper "Synaptic Theory of Working Memory-support"
		/**
		 * returns weight factor
		 * @param j neuron id
		 * @return
		 */
		double sciSTP(int j){
			double wf = 0;
			double bigU = 0.2;
			double tauF = 1500;//ms (nat: 600)
			double tauD = 200;//ms
			double du = 0;
			
			double s = 0;
			if(neurons[j].isFiring()){
				s = 1;
			}
			double u = sci_u[j];
			double x = sci_x[j];
			
			//update u
			du = (bigU-u)/tauF + bigU*(1-u)*s;		  
			double nu = u+du;
			
			//update x
			double dx = (1-x)/tauD - u*x*s;
			double nx = x+dx;
			
			wf = nu*nx;//no delay
			sci_u[j] = nu;
			sci_x[j] = nx;
			//double[] res = {wf,nu,nx};
			return wf;
		}
	
		
		/** 
		 * @param begin neuron index
		 * @param size size of the group of neurons to be stimulated
		 * @param stimValue stimulation value (mV)
		 */
		public void stimulate(int begin, int size, double stimValue) {
			for(int i=begin; i<begin+size; i++){			
				neurons[i].addToI(stimValue);
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
				}
			}
		}
		
}
