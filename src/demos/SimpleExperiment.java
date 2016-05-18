/**
 * 
 */
package demos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import models.IzhNeuron;
import startup.Constants;
import visualization.NetworkGraph;

import communication.MyLog;

/**
 * @author lana
 *	Experiment with 100 neurons to demonstrate simple LSA.
 */
public class SimpleExperiment {
	/** log */
	MyLog mlog = new MyLog("simpleExp", true);
	/** visualization*/
	NetworkGraph netGraph;
	
	/** total number of neurons in the network */
	int numberOfNeurons = 100;
	
	/**data directory*/
	String folderName;

	/**records weight of first synapse*/
	FileWriter wsyn1;//= new FileWriter(fileName);
	/**records weight of second synapse*/
	FileWriter wsyn2;
	/** records weight matrix*/
	FileWriter weightsWriter;

	//csv files
	/** spikes time series */
	FileWriter spikesWriter;
	/** reaction time */
	FileWriter reacTimeWriter;

	/** array of neurons representing the network */
	IzhNeuron[] neurons;
	/** synaptic weights*/
	double[][] weights;
	/**delay between 2 stimulations (sim ms)*/
	int stimDelay = 30;
	/**delay between 2 real time cycles (real ms)*/
	int it_delay = 300;	
	/**noise range (gaussian std deviation)*/
	double noise = 3;
	/** weight range*/
	double weights_range = 5;
	/** maximum weight*/
	int maxWeight = 15;
	double decay_rate = Constants.DecayRate;
	/** stimulation value (mV)*/
	double stim_value = 0.8;
	/** stdp window */
	int tau = Constants.tau;
	/** number of inhibitory neurons in the network*/
	int numberOfInhibitory = 20;
	/** simulation time*/
	int iteration = 0;
	
	/**stdp tau*/
	int[] STDPcount = new int[numberOfNeurons];
	/** pattern checking */
	int nclus  = 10;
	int[] clus_fired = new int[nclus];
	
	/**is the network being stimulated or not*/
	boolean stim = false;
	/**random normal generator - should it be decorrelated?*/
	Random noiseRand = new Random();
	
	/**name of data file if running several experiments simultaneously*/
	String name="";
	
	public SimpleExperiment(){	
			run();
	}
	
	private void  run(){
		Thread netThread = new Thread(new NetworkRunnable());
		netThread.run();
	}
	
	
	public SimpleExperiment(String name){
		this.name = name;
		mlog.setName("simpleExp "+name);
		run();
	}
	
	/**
	 * setting up the demo
	 */
	void setup(){		
		//initialization
		neurons = new IzhNeuron[numberOfNeurons];
		weights = new double[numberOfNeurons][numberOfNeurons];
	    
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	
	    if((name.compareTo("")==0)){
	    	folderName = Constants.DataPath + "\\" + strDate + "\\";
	    }else{
	    	folderName = Constants.DataPath + "\\" +name + "\\";
	    }
		
	    
	    initNetwork();
		initDataFiles();
		writeWeights();
	}


	/**
	 * Initializes the network and the neurons
	 */
	void initNetwork(){

	    for(int i=0; i<numberOfNeurons; i++){
	    	neurons[i] = new IzhNeuron();
	         if(i<numberOfInhibitory){
				 neurons[i].setNeuronType(Constants.NeurInhibitory);
			 } else{ 
				neurons[i].setNeuronType(Constants.NeurExcitatory);
			 }
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
				if(i!=j){			
					if(i<numberOfInhibitory){
						weights[i][j] = -weights_range*Constants.uniformDouble();
					} else{
						weights[i][j] = weights_range*Constants.uniformDouble();
					}
				}			
			}
	    }
	    
	    iteration = 0;
	    
	    //create graph visualizer
	    netGraph = new NetworkGraph(numberOfNeurons, weights);
	    netGraph.show();
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
			spikesWriter = new FileWriter(folderName+"\\"+Constants.SpikesFileName);
			mlog.say("stream opened "+Constants.SpikesFileName);
        	String str = "neuronID,iteration\n";
        	spikesWriter.append(str);
        	spikesWriter.flush();
			
			reacTimeWriter = new FileWriter(folderName+"\\"+Constants.ReacTimeFileName);
			mlog.say("stream opened "+Constants.ReacTimeFileName);
			str = "reactionTime,iteration\n";
        	reacTimeWriter.append(str);
        	reacTimeWriter.flush();

			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	/** record weights in csv file*/
	void writeWeights(){
		openStream();
		
		try {
			for(int i=0; i<numberOfNeurons; i++){
		        for(int j=0; j<numberOfNeurons; j++){
		        	String str = i+","+j+","+weights[i][j]+"\n";
		        	weightsWriter.append(str);					
		        	weightsWriter.flush();
		        }
		    }
			weightsWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//mlog.say("weights written");
	}
	
	
	void openStream(){		
		String weightsFileName = "weight_"+iteration+".csv";
		
		try {
			weightsWriter = new FileWriter(folderName+"\\"+weightsFileName);
			String str = "i,j,weight_i_j\n";
        	weightsWriter.append(str);
        	weightsWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mlog.say("stream opened "+weightsFileName);
	}
	
	
	/** update all neurons and their interactions*/
	void updateNeurons(){
		
		

		for(int i =0 ; i<nclus; i++){
			clus_fired[i] = 0;
		}

	    //spikedNeuronID.clear();
		int n = 0;
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].checkFiring();
			//check on clusters
			n = i/10;
	        if(neurons[i].isFiring()) {
	            //spikedNeuronID.push_back(i);
	        	String str = i+","+iteration+"\n";
	        	try {
	        		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				clus_fired[n]++;
	        }
	    }

		decay();
	    STDP();

		for(int i=0; i<numberOfNeurons; i++){
			neurons[i].addToI(noise*noiseRand.nextGaussian());
	        for(int j=0; j<numberOfNeurons; j++){
	            if((i != j) && neurons[i].isFiring()) neurons[j].addToI(weights[i][j]);
	        }        
	    }
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].update();
	        neurons[i].setI(0);
	    }
	    
		iteration++;
	    if((iteration%10000) == 0){	    	
			writeWeights();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mlog.say("step "+iteration);
	    }	    
	    
	    if((iteration%2000) == 0){
	    	//repaint synapses
	    	netGraph.update(weights);
	    }
	    
	    netGraph.updateNeurons(neurons);
	}
	
	
	void broadStimulation(){
		stimDelay--;

		boolean c1=false,c2=false;
		
		if(clus_fired[3]>3){
			c1 = true;
		}
		
		if(clus_fired[4]<4){
			c2 = true;
		}

//TODO stop at random time
		if(c1 && c2){
			stim = false;
			String str = stimDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mlog.say("OK, rt "+ stimDelay+" \n");
			stimDelay = 500;
		}

		if(stimDelay < -10000){
			String str = stimDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stimDelay = 500;
			stim = false;
			mlog.say("reached limit \n");
		}

		if(stimDelay<0){//??(stim && 
			for(int i=0; i<10; i++){
				neurons[i+20].addToI(stim_value);
			}
		}
	}

	
	void decay(){
	    for(int i=numberOfInhibitory; i<numberOfNeurons; i++){
	        for(int j=numberOfInhibitory; j<numberOfNeurons; j++){
	           if(i != j) weights[i][j] *= decay_rate;
	            if(weights[i][j] < 0.) {
						weights[i][j] = 0.;
	            }
	        }
	    }
	}
	
	void STDP(){
	    
		//reinitialize tau for the neurons that have fired
	    for(int i=0; i<numberOfNeurons; i++){
	        if(STDPcount[i]>0) STDPcount[i]--;
	        if(neurons[i].isFiring()) STDPcount[i] = tau;
	    }

		for(int i=numberOfInhibitory; i<numberOfNeurons; i++){
	        if(neurons[i].isFiring()){
				for(int j=numberOfInhibitory; j<numberOfNeurons; j++){
	                if(i != j){
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
	
	void closeStreams(){
		mlog.say("closing streams");
		try {
			spikesWriter.flush();
			spikesWriter.close();		
			
			
			reacTimeWriter.flush();
			reacTimeWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			
			setup();
			int iter = 0;
			
			while(run){
				
				updateNeurons();
				broadStimulation();
				iter++;
				
				/*if(iter>=600000){
					mlog.say("end of experiment"+name+" at t="+iter);
					kill();
				}*/
				
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

