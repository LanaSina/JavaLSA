package models;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import communication.MyLog;
import startup.Constants;

/**
 * According to paper "Spike-Timing theory of Working Memory"
 * @author lana
 */
public class IzhMemoryNet {
	
	/** log */
	MyLog mlog = new MyLog("IzhWM", true);
	
	/** total number of neurons in the network */
	int numberOfNeurons = 500;//100
	/** number of inhibitory neurons in the network*/
	int numberOfInhibitory = 100;//20
	/** simulation time (age of the network)*/
	int iteration = 0;
	/** array of neurons representing the network */
	IzhNeuron[] neurons = new IzhNeuron[numberOfNeurons];
	/** synaptic weights*/
	double[][] weights = new double[numberOfNeurons][numberOfNeurons];;
//nature	
	/**reference synaptic weights*/
	double[][] ref_weights = new double[numberOfNeurons][numberOfNeurons];
	/** some kind of synaptic trace??*/
	double[] z_minus = new double[numberOfNeurons];
	int e_sum = 0;
	
	/** stdp window */
	int tau = Constants.tau;
	/**stdp tau*///stdp per synapse
	int[][] synSTDPcount = new int[numberOfNeurons][numberOfNeurons];//20ms
	//stdp per neuron
	int[] neurSTDPcount = new int[numberOfNeurons];
	/**random normal generator*/
	Random noiseRand = new Random();	
	/** weight range*/
	double weightsRange = 5;//2//10
	/** maximum weight*/
	double maxWeight = 10;//1
	/**noise range (gaussian std deviation)*/
	double noise = 3;//3
	
	int nConnections = 100;//100
	
	//STP according to Science
	double[] sci_u = new double[numberOfNeurons];
	double[] sci_x= new double[numberOfNeurons];
	double[] wf = new double[numberOfNeurons];
	
	//axonal delays
	int[][] delays = new int[numberOfNeurons][numberOfNeurons];
	//
	int maxDelay = 44;//ms
	//buffer of spikes 
	ArrayList<Spike> spikes = new ArrayList<Spike>();
	//short term stdp by Izhikevich
	double[][] sd = new double[numberOfNeurons][numberOfNeurons];
	/**stdp tau*/
	int[][] sdCount = new int[numberOfNeurons][numberOfNeurons];//5 seconds?? how is that "short term"
	int sdTau = 5000;
	//sum of all stimulations
	double[] stMat = new double[numberOfNeurons];
	
	
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
	/**name of data file if running several experiments simultaneously*/
	//String name="";
	
	/**
	 * Builds a network with default parameters
	 * @param draw set true if graphics are necessary (slow)
	 */
	public IzhMemoryNet(boolean draw){
		init();		
		run();
	}
	
	
	private void init(){	    
	    //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);

	    folderName = Constants.DataPath + "/" + strDate + "/";		
	    
	    initNetwork();
		initDataFiles();
		writeWeights();		
	}
	
	
	/**
	 * Initializes the network and the neurons
	 */
	void initNetwork(){
		//init neurons
	    for(int i=0; i<numberOfNeurons; i++){
	    	neurons[i] = new IzhNeuron();    	
	    	wf[i] = 1;
	    	neurSTDPcount[i] = 0;
			//STP factors 
			sci_x[i] = 1;
			sci_u[i] = 0;
			z_minus[i] = 0;//TODO ??
	    	//STDPcount[i] = 0;
	         if(i<numberOfInhibitory){
				neurons[i].setNeuronType(Constants.NeurInhibitory);
			 } else{ 
				neurons[i].setNeuronType(Constants.NeurExcitatory);
			 }
	         
	         for(int j=0; j<numberOfNeurons; j++){
					weights[i][j] = 0;
					ref_weights[i][j] = 0;
					sd[i][j] = 0;
					synSTDPcount[i][j] = 0;
					sdCount[i][j] = 0;
	         }
	    }

		//random weights and delays
		//Random delayRand = new Random();		
	    for(int i=0; i<numberOfNeurons; i++){ // probabilistic connections; 0.1
	    	int n = 0;
	    	while(n<nConnections){
	    		int j2 =  (int) Constants.uniformDouble(0,numberOfNeurons);
				if(i!=j2){
					n++;
					if(i<numberOfInhibitory){
						weights[i][j2] = -weightsRange*Constants.uniformDouble();
						delays[i][j2] = 1;
					} else{
						weights[i][j2] = weightsRange*Constants.uniformDouble();
						ref_weights[i][j2] = weights[i][j2];
						delays[i][j2] = (int) (Constants.uniformDouble(0, 20)+ 0.5);// (delayRand.nextGaussian()*20 + 0.5);
					}					
				}			
			}
	    }
	    
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
		//current spikes
		boolean[] currentSpikes = new boolean[numberOfNeurons];
		
		//spikes travelling
		//stdp buffers also updated here
		boolean[][] delayedSpikes = updateSpikeBuffer();		
		
	    for(int i=0; i<numberOfNeurons; i++){    	
	        if(neurons[i].checkFiring()) {
	        	currentSpikes[i] = true;
	        	neurSTDPcount[i] = tau+1;//will be decayed next loop
	        	//add spike to buffer	
	        	for(int j=0; j<numberOfNeurons; j++){
	        		if(weights[i][j]>0){
		        		Spike s =  new Spike(i,j,delays[i][j]);
		        		spikes.add(s);
	        		}
	        	}        	
	        	//write spike in file
				String str = i+","+iteration+"\n";
		    	try {
		    		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else{
	        	currentSpikes[i] = false;
	        }   
	    }
  
	    for(int i=numberOfInhibitory; i<numberOfNeurons; i++){	    	
    		if(neurSTDPcount[i]>0) neurSTDPcount[i]--;
    		
		    for(int j=numberOfInhibitory; j<numberOfNeurons; j++){
		    	if(i != j){
		    		if(synSTDPcount[i][j]>0) synSTDPcount[i][j]--;
		    		if(sdCount[i][j]>0) sdCount[i][j]--;
		    		
		    		//double dij = 0;
		    		//double dji = 0;				    
					double[] dw = uniSTDP(i,j,currentSpikes[i],delayedSpikes[i][j]);
					//dij += dw[0];
					//dji += dw[1];
											
					weights[i][j] += dw[0];//dij; 
					weights[j][i] += dw[1];//dji 
					
					//weight check
			        if (weights[i][j] > maxWeight) weights[i][j] = maxWeight;
			        else if(weights[i][j] <= 0) weights[i][j] = 0;
			        
			        if (weights[j][i] > maxWeight) weights[j][i] = maxWeight;
			        else if(weights[j][i] <= 0) weights[j][i] = 0;
		    	}
		    }
	    }
	    
	  // noiseScale(e_sum);	    
	    for(int i=0; i<numberOfNeurons; i++){
	    	//noise to i
			neurons[i].addToI(noise*noiseRand.nextGaussian());
			wf[i] = sciSTP(i);
			//all spikes to i
			neurons[i].addToI(wf[i]*stMat[i]);//
//			for(int j=0; j<numberOfNeurons; j++){
//				if(delayedSpikes[j][i]){
//					double s = shortTermSTDP(j,i);
//	            	double weight = weights[j][i]*(1 + s);//Short-term STDP by Izh		            	
//	            	neurons[i].addToI(wf[i]*weight);//
//		        }     	
//		    }
			//update and set
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
			mlog.say("noise " + noise);
	    }	    
	    
	    //stimulation
	    externalStim();
	}
	
	/**
	 * update spikes delays and spikes list
	 * @return which spikes should be transmitted now
	 */
	boolean[][] updateSpikeBuffer(){
		boolean[][] s = new boolean[numberOfNeurons][numberOfNeurons];
		//init
		for(int i=0; i<numberOfNeurons; i++){
			stMat[i] = 0;
			for(int j=0; j<numberOfNeurons; j++){
				s[i][j] = false;//TODO maybe we can delete this var				
			}
		}
		
		//check
		e_sum = 0;//sum of excitatory spikes
		ArrayList<Spike> remove = new ArrayList<Spike>();
		int i = 0;
		while(i<spikes.size()){			
			Spike sp = spikes.get(i);
			sp.delay--;
			if(sp.delay<=0){
				s[sp.i][sp.j] = true;
				double s2 = shortTermSTDP(sp.i,sp.j);
				double weight = weights[sp.i][sp.j]*(1 + s2);
				stMat[sp.j]+=weight;
				
				e_sum++;
				synSTDPcount[sp.i][sp.j] = tau+1; // delay per synapse; will be decayed right after
				sdCount[sp.i][sp.j] = sdTau+1; //
				remove.add(sp);
			}
			i++;
		}
		e_sum = (int) (e_sum/(1.0*nConnections));

		spikes.removeAll(remove);
		return s;
	}
	
	//unitary stdp
	/**
	 * The implementation is a bit different than STDP but result i the same.
	 * Doublet Spike Timing Dependent Plasticity
	 * @param i presynaptic neuron index
	 * @param j postsynaptic neuron index
	 * @param spikeI did post synaptic neuron I fire
	 * @param spikeIJ did synapse IJ fire
	 * @param return weight change array [dij, dji]
	 */
	private double[] uniSTDP(int i, int j, boolean spikeI, boolean spikeIJ){
		double dij = 0,dji = 0;
		double d = (0.1*Math.pow(0.95, (tau-synSTDPcount[j][i])));
		
		if(spikeI){
			//check that neuron linked to i fired less than tau_ms before (but is not firing right now)
			//if jsyn fired before i, then weight j->i ++
			if((weights[j][i] != 0.0) && (synSTDPcount[j][i]>0) && (synSTDPcount[j][i] != tau)){  //(STDPcount[j]>0) && 		
				dji += d;
				sd[j][i] += d;//
				if(sd[j][i]>1) sd[j][i] = 1;
            }
		}
		
		if(spikeIJ){
			//now weight from i to j, should be lowered if isyn fired before j
			if((weights[i][j] != 0.0) && (neurSTDPcount[j]>0)  && (neurSTDPcount[j] != tau)){
                dij -= d;
                if(sd[i][j]>0) sd[i][j] -= d;//-
                if(sd[j][i]<-1) sd[j][i] = -1;
            }         
        }
        
        double[] r_w = {dij, dji};
        return r_w;
	}
	
	
	private double shortTermSTDP(int i, int j){
		double sdij = 0;
		//just exponential decay
		sdij = sd[i][j]*Math.exp(-(sdTau-sdCount[i][j])/sdTau);//5s*delay
        return sdij;
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
		return wf;
	}
	
	//heterosynaptic plasticity: can it destroy "STP+low-activity" induced bursts?  (several plasticity paper)
	//answer = no
	/**
	 * HeteroSynaptic Plasticity
	 * @param w the weight before plasticity
	 * @param ref_w the reference weight w_{ij}
	 * @param z_m z^{minus}_{i}
	 * @param s S_i
	 * @return the weight modification dw/dt 
	 */
	public double HSP(double w, double ref_w, double z_m, double si){
		double dw = 0;
		//beta = HSP strength parameter
		double b = 0.05;
		//integration constant = 1ms?
		double t = 1;
		//probablility of connection EE
		double e = 0.1;
		
		//(z_m*(t-e))^3
		double temp = Math.pow(z_m*(t-e),3);
		dw = -b*(w-ref_w)*temp*si;
		//if(dw==Double.NaN) mlog.say("***nan");
		return dw;
	}
	
	//STDP on IE connections according to Zenke nature paper
	//dji
	double iSTDP(int i, int j, double g, int[] currentSpikes){
		double dw = 0;
		double n = 2.0/50000; //learning rate
		dw = (z_minus[i]+1)*currentSpikes[j] + z_minus[j]*currentSpikes[i];
		dw = dw*g*n;
		
		return dw;
	}
	
	/** global modulation factor for iSTDP*/
	//double istdp_h = 0;
	double target_f = 3;//Hz
	//try global noise modulation (which really shoud be module-dependent and sacalble I guess
	//inspired from nature paper
	/**
	 * @param e_sum sum of current spikes
	 */
	void noiseScale(int e_sum){//TODO global. try to make it local?
		double b2 = target_f - e_sum*1000.0/numberOfNeurons;//
		double t = 1.0/5000;
		noise += b2*t;
	//	mlog.say("noise "+ noise);
	}
	
	//stop the bursting
	/**
	 * transmitter induced plasticity
	 * @param sj S_j
	 * @return the weight modification dw/dt
	 */
	public double TIP(double sj){
		//TIP strength parameter (2.10^-5)
		double d = 2*0.00001;		
		double dw = -d*sj;		
		return dw;
	}
	
	
	private void externalStim(){
		//stimulate 10 times in one second, every 10 seconds
		int r = iteration%10000;
		if(iteration>00000){ //300000
			if(r<=1000){
				polychronyPattern(r%100,1);
			}
		}
	}
	
	void polychronyPattern(int r, int type){
		if(r<80){//30
			if(type==1)
				stimulate(200+r,1,80); //m1
			if(type==2)
				stimulate(99-r,1,50); ///m2
			if(type==3){
				stimulate(20+r,1,50);
				stimulate(99-r,1,50); //bind
			}
			if(type==4)//recall m1
				stimulate(20+r,1,50); 
		}		
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
	
	public void closeStreams(){
		mlog.say("closing streams");
		try {
			spikesWriter.flush();
			spikesWriter.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void  run(){
		NetworkRunnable netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	/** to store spike neuron ID i, j and delay*/
	private class Spike{
		int i,j;
		int delay;
		
		/**
		 * @param i presynaptic neuron id
		 * @param j postsynaptic neuron id
		 * @param d
		 */
		public Spike(int i, int j, int d){
			this.i = i;
			this.j = j;
			delay = d;
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
