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

/**
 * A net of Izhikevich neurons.
 * @author lana
 *
 */
public class IzhNetwork {	
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
	double w_var = 0;
	/** maximum weight*/
	double maxWeight = 10;//10
	/**noise range (gaussian std deviation)*/
	double noise = 3;//3;	
	/** firing rates*/
	LinkedList<Integer> rates[];
	int rateWindow = 20;

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
	
	
	/**
	 * Builds a network with default parameters
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhNetwork(boolean draw){
		drawGraph = draw;
		//init("");
	}

	
	/**
	 * @param name name for logging
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhNetwork(String name, boolean draw){
		this.name = name;
		drawGraph = draw;
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
	public IzhNetwork(int nn, int ni, double wr, double maxw, double nr, String name, boolean draw){
		numberOfNeurons = nn;
		numberOfInhibitory = ni;
		weightsRange = wr;
		maxWeight = maxw;
		noise = nr;
		this.name = name;
		drawGraph = draw;
		
		init("");
	}
	
	public void setWVar(double v){
		w_var = v;
	}
	
	public void setConnections(int c){
		connections = c;
	}
	
	public void init(String folder){
		mlog.setName("simpleExp "+name);
		mlog.say("folder aa " + folder);
		
		rates = (LinkedList<Integer>[])new LinkedList<?>[numberOfNeurons];		
		for(int i=0;i<numberOfNeurons;i++){
			rates[i] = new LinkedList<Integer>();
		}
		
		
		STDPcount = new int[numberOfNeurons];
		nclus  = numberOfNeurons/10;
		clusFired = new int[nclus];		
		neurons = new IzhNeuron[numberOfNeurons];
		weights = new double[numberOfNeurons][numberOfNeurons];
	    
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    
	    folderName = Constants.DataPath + "/" ;
	    if((folder.compareTo("")!=0)){
	    	folderName = folderName + folder + "/";
	    }
	    
	    if((name.compareTo("")==0)){
	    	folderName = folderName + strDate + "/";
	    }else{
	    	folderName = folderName + "net" +name + "/";
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
				for(int j=0; j<rateWindow; j++){
				rates[i].add(0);
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
				int j2;// = j;
//				while(i==j2){
					j2=(int) (numberOfNeurons*Constants.uniformDouble()); //00 to 99			
//				}
				if(i!=j2){			
					if(i<numberOfInhibitory){
						weights[i][j2] = -weightsRange+w_var*Constants.uniformDouble(-1,1);
					} else{
						weights[i][j2] = weightsRange+w_var*Constants.uniformDouble(-1,1);
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
		        theDir.mkdirs();
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

	    //spikedNeuronID.clear();
		int n = 0;
		
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].checkFiring();
	       // rates[i].removeFirst();
			//check on clusters
			n = i/10;
	        if(neurons[i].isFiring()) {
	        	//rates[i].add(1);
	        	String str = i+","+iteration+"\n";
	        	try {
	        		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				clusFired[n]++;
	        } else{
	        	//rates[i].add(0);
	        }
	    }

		decay();
	    STDP();

		for(int i=0; i<numberOfNeurons; i++){
			double inp = noise;///(sum*2);
			
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
