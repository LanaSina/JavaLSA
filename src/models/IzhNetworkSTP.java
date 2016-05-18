package models;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import startup.Constants;
import visualization.NetworkGraph;
import communication.MyLog;

public class IzhNetworkSTP {	
	/** log */
	MyLog mlog = new MyLog("IzhNetwork", true);
	/** visualization*/
	NetworkGraph netGraph;
	/** draw graphics or not*/
	boolean drawGraph = false;
	/** synchronization lock*/
	boolean ready;
	
	/** total number of neurons in the network */
	int numberOfNeurons = 100;
	/** number of inhibitory neurons in the network*/
	int numberOfInhibitory = 20;
	/** simulation time (age of the network)*/
	int iteration = 0;
	/** array of neurons representing the network */
	IzhNeuron[] neurons;
	/** synaptic weights*/
	double[][] weights;
	/** weights decay*/
	double decay_rate = Constants.DecayRate;
	/** stdp window */
	int tau = Constants.tau;
	/**stdp tau*/
	int[] STDPcount;
	/** pattern checking */
	int nclus;
	int[] clusFired;
	/**is the network being stimulated or not*/
	boolean stim = false;
	/**random normal generator*/
	Random noiseRand = new Random();
	
	/** weight range*/
	double weightsRange = 5;//5;
	/** maximum weight*/
	double maxWeight = 10;//weightsRange;//10;//10
	/**noise range (gaussian std deviation)*/
	double noise = 3;//3;	


	int connections = 100;
	
	/**data directory*/
	String folderName;
	/** records weight matrix*/
	FileWriter weightsWriter;
	//csv files
	/** spikes time series */
	FileWriter spikesWriter;	
	/**name of data file if running several experiments simultaneously*/
	String name="";
	
	
	//STP according to Science
	double[] sci_u = new double[numberOfNeurons];
	double[] sci_x= new double[numberOfNeurons];
	double[] wf = new double[numberOfNeurons];
	
	/**
	 * Builds a network with default parameters
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhNetworkSTP(boolean draw){
		drawGraph = draw;
		init();		
	}
	
	/**
	 * @param name
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhNetworkSTP(String name, boolean draw){
		this.name = name;
		drawGraph = draw;
		init();
	}

	/**
	 * 
	 * @param nn total number of neurons
	 * @param ni number of inhibitory neurons
	 * @param wr weights range
	 * @param maxw max weight
	 * @param nr noise range
	 * @param name use if running parallel experiments; set to "" if no name necessary
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhNetworkSTP(int nn, int ni, double wr, double maxw, double nr, String name, boolean draw){
		numberOfNeurons = nn;
		numberOfInhibitory = ni;
		weightsRange = wr;
		maxWeight = maxw;
		noise = nr;
		this.name = name;
		drawGraph = draw;
		
		init();
	}
	
	private void init(){
		mlog.setName("simpleExp "+name);
		
		STDPcount = new int[numberOfNeurons];
		nclus  = numberOfNeurons/10;
		clusFired = new int[nclus];		
		neurons = new IzhNeuron[numberOfNeurons];
		weights = new double[numberOfNeurons][numberOfNeurons];
	    
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	
	    if((name.compareTo("")==0)){
	    	folderName = Constants.DataPath + "/" + strDate + "/";
	    }else{
	    	folderName = Constants.DataPath + "/net" +name + "/";
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
	    	wf[i] = 1;
			//STP factors 
			sci_x[i] = 1;
			sci_u[i] = 0;
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
			for(int j=0; j<connections; j++){//numberOfNeurons
				int j2 = j;
//				while(i==j2){
//					j2=(int) (numberOfNeurons*Constants.uniformDouble()); //00 to 99			
//				}
				if(i!=j2){			
					if(i<numberOfInhibitory){
						weights[i][j2] = -weightsRange*Constants.uniformDouble();//
					} else{
						weights[i][j2] = weightsRange*Constants.uniformDouble();
					}
				}			
			}
	    }
	    
	    iteration = 0;
	    
	    //create graph visualizer
	    if(drawGraph){
	    	netGraph = new NetworkGraph(numberOfNeurons, weights);
	    	netGraph.show();
	    }
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
			e.printStackTrace();
		}
	}
	
	
	void openStream(){		
		String weightsFileName = "weight_"+iteration+".csv";
		
		try {
			weightsWriter = new FileWriter(folderName+"/"+weightsFileName);
			String str = "i,j,weight_i_j\n";
        	weightsWriter.append(str);
        	weightsWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mlog.say("stream opened "+weightsFileName);
	}
	
	
	/** update all neurons and their interactions*/
	public synchronized void updateNeurons(){		

		for(int i =0 ; i<nclus; i++){
			clusFired[i] = 0;
		}

		int n = 0;
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].checkFiring();
			//check on clusters
			n = i/10;
	        if(neurons[i].isFiring()) {
	        	String str = i+","+iteration+"\n";
	        	try {
	        		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				clusFired[n]++;
	        } 
	    }

		decay();
	    STDP();

		for(int i=0; i<numberOfNeurons; i++){
			double inp = noise;
			int f = 0;
			if(neurons[i].isFiring()){
				f = 1;
			}
			if(i>numberOfInhibitory){
				wf[i] = sciSTP(i,f);
			}else{
				wf[i] = 1;
			}
			//all spikes to i			
			neurons[i].addToI(inp*noiseRand.nextGaussian());
	        for(int j=0; j<numberOfNeurons; j++){
	            if((i != j) && neurons[i].isFiring())
	            	neurons[j].addToI(weights[i][j]*wf[i]);//wf[i]
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
	    
	    if(drawGraph){
		    if((iteration%2000) == 0){
		    	//repaint synapses
		    	netGraph.update(weights);
		    }		    
		    netGraph.updateNeurons(neurons);
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
	
	
	//STP according to Science paper "Synaptic Theory of Working Memory-support"
		/**
		 * returns weight factor
		 * @param j
		 * @param fired 1 if this neuron is firing, 0 otherwise
		 * @return
		 */
		double sciSTP(int j, int fired){//nat????
			double wf = 0;
			
			double s = fired;
			double u = sci_u[j];
			double x = sci_x[j];
			
			double tau_d = 200;//ms
			double tau_f = 600;//ms
			double U = 0.2;//mV
			
			double dx = (1-x)/tau_d - u*x*s;
			double du = (U - u)/tau_f + U*(1-u)*s;
			
			double nu = u+du;
			double nx = x+dx;
			
			wf = nu*nx;
			sci_u[j] = nu;
			sci_x[j] = nx;
			return wf;
		}
		
	
	public void closeStreams(){
		mlog.say("closing streams");
		try {
			spikesWriter.flush();
			spikesWriter.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return name;
	}

	public String getDataFolder() {
		return folderName;
	}

	public int[] getFiringClusters() {		
		return clusFired.clone();
	}

	public int getIteration() {
		return iteration;
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
	
	public void setClusters(int inputStart, int inputSize, int outputStart, int outputSize, int outputBStart, int outputBSize){
		netGraph.setClusters(inputStart, inputSize, outputStart, outputSize, outputBStart, outputBSize);		
	}
}
