package models;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import communication.MyLog;
import startup.Constants;
import visualization.Display;
import visualization.GraphicalComponent;
import visualization.NetworkGraph;

/**
 * Plasticity from the Nature paper "Diverse synaptic plasticity mechanisms
 * orchestrated to form and retrieve memories in spiking neural networks"
 * 
 * Has 4: STP, STDP, heterosynaptic plasticity and transmitter-induced plasticity.
 * @author lana
 *
 */
public class NatMemoryNetwork implements GraphicalComponent {

	/** log */
	MyLog mlog = new MyLog("MemNet", true);
	/** visualization*/
	NetworkGraph netGraph;
	/** Window name*/
				String w_name = "very STRONG stim, size input = 30, hsp, full connections";
	/** draw graphics or not*/
	boolean drawGraph = false;
	/** synchronization lock*/
	//boolean ready;
	
	/** total number of neurons in the network */
	int numberOfNeurons = 1100;
	/** number of inhibitory neurons in the network*/
	int numberOfInhibitory = 220;
	/** simulation time (age of the network)*/
	int iteration = 0;
	/** array of neurons representing the network */
	AIAFNeuron[] neurons;
	/** synaptic weights*/
	double[][] weights;
	/**reference synaptic weights*/
	double[][] ref_weights;
	/**STP factors*/
	double[][] w_factors;
	//STP according to Science
	double[] sci_u;
	double[] sci_x;
	
	/**iSTDP z*/
	//double[] inh_z; same as z_minus
	/** global modulation factor for iSTDP*/
	double istdp_h = 0;
	
	/** weights decay*/
	double decay_rate = Constants.DecayRate;
	/** stdp window */
	int tau = Constants.tau;
	/**stdp tau count*/
	int[] STDPcount;
	/** pattern checking */
	int nclus;
	int[] clusFired;
	/**random normal generator*/
	Random noiseRand = new Random();
	
	/** weight range*/
	double weightsRange = 0.1;
	/** maximum weight*/
	double maxWeight = 50;
	/**noise range (gaussian std deviation)*/
	double noise = 0.08;//3
	
	//for nature ref
	/** some kind of synaptic trace??*/
	double[] z_minus;
	/** firing status = 0 or 1*/
	double[] fired;
	
	//stimulate or not
	boolean exp_stim = true;
	
	/**data directory*/
	String folderName;
	/** records weight matrix*/
	FileWriter weightsWriter;
	//csv files
	/** spikes time series */
	FileWriter spikesWriter;	
	/**name of data file if running several experiments simultaneously*/
	String name="";
	
	/** draw raster in a new self-refreshing window*/
	boolean draw_raster = false;
	/**graphics displaying object*/
	Display display;
	//size
	int w = 1000;
	int h = 300;
	
	/**
	 * Builds a network with default parameters
	 * @param draw set true if graphics are necessary (slow)
	 */
	public NatMemoryNetwork(boolean draw){
		drawGraph = draw;
		init();		
	}
	
	/**
	 * 
	 * @param name
	 * @param draw set true if graphics are necessary (slow)
	 */
	public NatMemoryNetwork(String name, boolean draw){
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
	public NatMemoryNetwork(int nn, int ni, double wr, double maxw, double nr, String name, boolean draw){
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
		neurons = new AIAFNeuron[numberOfNeurons];
		weights = new double[numberOfNeurons][numberOfNeurons];
		ref_weights = new double[numberOfNeurons][numberOfNeurons];
		z_minus = new double[numberOfNeurons];
		fired = new double[numberOfNeurons];
		w_factors = new double[numberOfNeurons][numberOfNeurons];
		sci_x = new double[numberOfNeurons];
		sci_u = new double[numberOfNeurons];
	    
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
		
		//add self to graphic panel
		if(draw_raster){
			display = new Display(w,h, false);
			display.setName("Raster - constant stim");
			display.addComponent(this);
		}
	}
	
	/**
	 * Initializes the network and the neurons
	 */
	void initNetwork(){

	    for(int i=0; i<numberOfNeurons; i++){
	    	neurons[i] = new AIAFNeuron();
	         if(i<numberOfInhibitory){
				 neurons[i].setNeuronType(Constants.NeurInhibitory);
			 } else{ 
				neurons[i].setNeuronType(Constants.NeurExcitatory);
			 }
	    }
	    
		//initialization of weights to 0
		for(int i=0; i<numberOfNeurons; i++){
			z_minus[i] = 0;//TODO ??
			//inh_z[i] = 0; //??
			fired[i] = 0;
			sci_x[i] = 1;
			sci_u[i] = 0;
	        for(int j=0; j<numberOfNeurons; j++){
				weights[i][j] = 0;
				//Initialization value really is random positive (normal dist mean = sd = 0.3)
				ref_weights[i][j] = 0;
				w_factors[i][j] = 1;
			}
		}
			
		//weights
	    for(int i=0; i<numberOfNeurons; i++){
	    	int num = 0;
	    	//cfNe connections from EACH coding pop
	    	int ne = numberOfNeurons - numberOfInhibitory;
	    	int n = (int) (ne*0.02 +0.5);
	    	while(n>0){
	    		int j = (int) (800*Constants.uniformDouble()+300);
	    		if(i!=j){
	    			//to coding prop
	    			if(i>=300 && i<1100){
	    				weights[j][i] = 0.45;
	    			} else{
	    				//to non coding prop
	    				weights[j][i] = 0.1;
	    			}
	    			//to inhibitory
	    			if(i<numberOfInhibitory){
	    				weights[j][i] = 0.135;
	    			}
	    			n--;
	    			num++;
	    		}
	    	}
	    	//c(1-f)Ne from non coding 0.2*(1-0.1) = 0.18
	    	n = (int) (ne*0.18 +0.5);	    
	    	while(n>0){
	    		int j = (int) (80*Constants.uniformDouble()+220);
	    		if(i!=j){
	    			weights[j][i] = 0.1;
	    			//potentiation proba
	    			double proba = 0.1;
	    			//to inhibitory
	    			if(i<numberOfInhibitory){
	    				weights[j][i] = 0.135;
	    			}else if(Constants.uniformDouble()<=proba){//to excitatory, potentiated
	    				weights[j][i] = 0.45;
	    			}	    			
	    			n--;
	    			num++;
	    		}
	    	}
	    	//cNi from inhibitory
	    	n = (int) (numberOfInhibitory*0.2 +0.5);	    
	    	while(n>0){
	    		int j = (int) (numberOfInhibitory*Constants.uniformDouble());
	    		if(i!=j){
	    			//to inh
	    			if(i<numberOfInhibitory){
	    				weights[j][i] = 0.2; 
	    			}else{
	    				//to ex
	    				weights[j][i] = 0.25; 
	    			}	    			   			
	    			n--;
	    			num++;
	    		}
	    	}
	    	
	    	//mlog.say("num " + num + " vs "+ 0.2*numberOfNeurons);    	
	    }
//	    	int num =0;
//			for(int j=0; j<numberOfNeurons; j++){				
//				if(i!=j){
//					//cNi from inhib
//					double proba = 0.2;
//					if(i<numberOfInhibitory){
//						if(Constants.uniformDouble()<=proba){
//							weights[i][j] = -0.135;
//							num++;
//						}
//					}else{		
//						//cfNe connections from EACH coding pop
//						proba = 0.02;
//						if(i>=300 && i<1100){
//							if(Constants.uniformDouble()<=proba){
//								num++;
//								weights[i][j] = 0.1;//to normal
//								if(i>=300 && i<1100){
//									weights[i][j] = 0.45;//to coding pop
//								}
//							}
//						} else{
//							//c(1-f)Ne from non coding 0.2*(1-0.1)
//							proba = 0.18;
//							if(Constants.uniformDouble()<=proba){
//								weights[i][j] = 0.1;
//								num++;
//							}
//						}
//					}
//					
//					
//					
//					//connections from coding population
////					if(i>=300 && i<1100){	
////						if(Constants.uniformDouble()<=0.02){						
////							if(j>=300 && j<1100){//connections to coding pop
////								weights[i][j] = 0.45;//Constants.uniformDouble(0.3,0.5);//0.3,.5 for 400*1%//100: 5 full, 4 non maintained //Constants.uniformDouble(5,8);
////								//ref_weights[i][j] = maxWeight;
////							}else{ 
////								//to inhibitory
////								if(i<numberOfInhibitory){
////									weights[i][j] = 0.135;
////								}
////								//to non coding
////								weights[i][j] = 0.1;
////							}
////						}	
////					}else{						
////						//from non coding excitatory
////						if(i>=numberOfInhibitory){//to everyone
////							if(Constants.uniformDouble()<=0.18){
////								weights[i][j] = 0.1;
////								if(Constants.uniformDouble()<=0.02){
////									weights[i][j] = 0.45;//non coding to non coding
////								}
////							}
////						}else{// from inhibitory
////							//to inhibitory
////							if(i<numberOfInhibitory){
////								weights[i][j] = -0.2;
////							}else{//to excitatory
////								if(Constants.uniformDouble()<=0.2){
////									weights[i][j] = -0.25;
////								}
////							}
////						}
////					}
//					
////					if((Constants.uniformDouble()<=0.1) && (i!=j)){
////						if(i<numberOfInhibitory){
////							weights[i][j] = -0.135;//weightsRange*Constants.uniformDouble();
////						} else{
////							weights[i][j] = 0.1;//weightsRange*Constants.uniformDouble();
////							//Initialization value really is random positive (normal dist mean = sd = 0.3)
////							//do this outside of this loop ?
////							ref_weights[i][j] = weightsRange*Constants.uniformDouble();
////							//we should have a look at these dyn
////						}
////	//assembly// 
////					}	
//				}
////				if(i>=300 && i<1000 && j>=300 && j<1100){	
////					if(Constants.uniformDouble()<=0.02){//0.6 to 1  for 100 neurons. ie >600 con 
////						//* w = 10 => energy in 1 neuron ~ 6000mV/ms. same con, more sparse = delay activity?
////						weights[i][j] = 0.45;//Constants.uniformDouble(0.3,0.5);//0.3,.5 for 400*1%//100: 5 full, 4 non maintained //Constants.uniformDouble(5,8);
////						ref_weights[i][j] = maxWeight;
////					}	
////				}
//			}
//			mlog.say("num " + num + " vs "+ 0.2*numberOfNeurons);
//	    }
	    
	    iteration = 0;
	    
	    //create graph visualizer
	    if(drawGraph){
	    	netGraph = new NetworkGraph(numberOfNeurons, weights);
	    	netGraph.setName(w_name);
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
	
	//stimulate or not
	boolean do_stim = true;
	//stimulation stop delay
	int stim_stop_delay = 100;
	//
	int hit = 0;
	//
	int stim_delay = 0;
	
	int p1 = 350;//1
	int p2 = 1;//1
	private void stimulate(){
		//input neurons
		int i_start = 300;
		//size of input zone
		int i_size = 800;
		//stimulation strength
		double stim = 0.1;//100;//mV
		
		if(iteration>=1000 && p1>0){
			//if(iteration%10==0){
				for(int i = i_start; i<(i_start+i_size);i++){
				//if(Constants.uniformDouble()<=0.8)
					neurons[i].addToIexc(stim);
				}
			//}		
			p1--;
		}
		
//		int f = 35;//66;//35;//Hz
//		p1--;
//		p2--;
//		
//		if(iteration%(1000/f)==0){
//			p1 = 1;
//		}
//		
//		if(p1>0){
//			for(int i = i_start; i<(i_start+i_size);i++){
//				neurons[i].addToI(stim);
//			}
//		}
//		
//		if(iteration%(1000/f)==5){
//			p2 = 1;
//		}
//		
//		if(p2>0){// && iteration <1000000){
//			for(int i = 60; i<70;i++){
//				neurons[i].addToI(stim);
//			}
//		}
	}
	
	/** update all neurons and their interactions*/
	public synchronized void updateNeurons(){		

		for(int i =0 ; i<nclus; i++){
			clusFired[i] = 0;
		}

		int n = 0;
		
	    for(int i=0; i<numberOfNeurons; i++){
	        //neurons[i].checkFiring();
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

	    //plasticity update for all neurons
//	  //reinitialize tau for the neurons that have fired
//	    double e_sum = 0;
//	    for(int i=0; i<numberOfNeurons; i++){
//        	fired[i] = 0;//TODO this in above loop
//	        if(STDPcount[i]>0) STDPcount[i]--;
//	        if(neurons[i].isFiring()){
//	        	STDPcount[i] = tau;	
//	        	//update firing status
//	        	fired[i] = 1;
//	        	if(i>numberOfInhibitory){
//	        		//sum excitatory spikes
//	        		e_sum+= 1;
//	        	}
//	        }
//	    }
//	    //update drawing
//	    if(draw_raster){
//	    	updateShapes();
//	    }
//	    
//	    //update H(t) (weeeird because Si is treated as firing rate)
//	    double tau_h = 10;//10s
//	    double gamma = 4;//Hz (does not look like enough... should be f(net_size)?
//	    double dh = -(istdp_h/tau_h) + e_sum;
//	    istdp_h += dh;
//	    double istdp_g = istdp_h - gamma;
//	    
//	    //do Ix connections
//	    for(int i=0; i<numberOfInhibitory; i++){
//	    	//IE (iSTDP)
//	    	for(int j=numberOfInhibitory; j<numberOfNeurons; j++){
//				//no self connections
//				if(i != j){
//					double w = weights[i][j];
//					double dw = iSTDP(i,j,istdp_g);
//					w += dw; TODO this is wji
//					weights[i][j] = w;//TODO clean up this mess of variables
//					
//					if (-weights[i][j] > maxWeight) weights[i][j] = -maxWeight;
//	                else if(weights[i][j] >= 0) weights[i][j] = 0;
//				}
//	    	}
//	    }
//	    
//	    //do Ex connections    
	    for(int i=numberOfInhibitory; i<numberOfNeurons; i++){
//	    	//update zminus
//			//d(z^minus_i)/dt = -z^minus/tau^minus + S_i 
//			double zm = z_minus[i];
//			//weird
//			double dzm = -(zm / tau) + fired[i];//tau minus = tau plus = 20ms
//			zm = zm + dzm;
//			z_minus[i] = zm;
//		
//			//EI connections (STP only)
//			for(int j=0; j<numberOfInhibitory; j++){
//				//no self connections
//				if(i != j){
//					//scale weight according to stp (temporary scaling)
//					w_factors[i][j] = STP(i,j);
//				}
//			}
//			
//			//EE connections (4 types of plasticity)
			for(int j=numberOfInhibitory; j<numberOfNeurons; j++){
//				//no self connections
//				if(i != j){
//					double w = weights[i][j];
//					double rw = ref_weights[i][j];
//					//well potential fixed point (custom)
//					double wp = 9; //maxWeight;
//					//potential strength parameter
//					double p = 20;//20 //p low means ??
//					//update reference weights every 1.2s
//					if(iteration%1200 == 0){
//						double drw = w - rw - p*rw*((wp/2)-rw)*(wp-rw);//TODO name the constants
//						double tau_cons = 20*60*1000;//20mn
//						drw = (1/tau_cons)* drw*1200;//(integration time!!!); //tau^cons = 20 mn
//						rw = rw+ drw; 
//						//if(rw==Double.NaN) mlog.say("***nan");
//						ref_weights[i][j] = rw;
//						//if(i==25 && j==65) mlog.say("rw "+rw+" w "+w);
//					}	
//					
//					//scale weight according to stp
//					w_factors[i][j] = STP(i,j); not j
					double wf = natSTP(i);//not j
					w_factors[i][j] = wf;
					
//					//update weights
//					double dij = 0;
//					double dji = 0;
//					//j to i and i to j
////					double[] dw = STDP(i,j, dij, dji);
////					dij = dw[0];
////					dji = dw[1];
////dij += HSP(w, rw, zm, fired[i]);
//					//
//					dij += TIP(fired[j]);
//					weights[i][j] = weights[i][j] + dij;
//					weights[j][i] = weights[j][i] + dji;
//					
//					//if(weights[i][j]==Double.NaN) mlog.say("***nan");
//					
//					if (weights[i][j] > maxWeight) weights[i][j] = maxWeight;
//	                else if(weights[i][j] <= 0) weights[i][j] = 0;
//	                
//	                if (weights[j][i] > maxWeight) weights[j][i] = maxWeight;
//	                else if(weights[j][i] <= 0) weights[j][i] = 0;
				}
			}
//	    }
//	   

	    //update communications
		for(int i=0; i<numberOfNeurons; i++){
			neurons[i].addToIexc(noise);//+noiseRand.nextGaussian());//noise*noiseRand.nextGaussian());
			
	        for(int j=0; j<numberOfNeurons; j++){
	            if((i != j) && neurons[i].isFiring()){
	            	if(i<numberOfInhibitory){
	            		neurons[j].addToIinh(w_factors[i][j]*weights[i][j]);
	            	}else{
	            		neurons[j].addToIexc(w_factors[i][j]*weights[i][j]);
	            	}
	            }
	            	
	        }        
	    }
		
		//update neuron states
	    for(int i=0; i<numberOfNeurons; i++){
	        neurons[i].update();
	       // neurons[i].setI(0);
	    }
	    
	    //stimulate
	    if(exp_stim){
	    	stimulate();
	    }
	    
	    //record data
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
	    
	    //draw if necessary
	    if(drawGraph){
		    if((iteration%2000) == 0){
		    	//repaint synapses
		    	netGraph.update(weights);
		    }		    
		    //netGraph.updateNeurons(neurons);
	    }
	}
	
	//STP according to Science paper "Synaptic Theory of Working Memory-support"
	/**
	 * returns weight factor
	 * @param j
	 * @return
	 */
	double natSTP(int j){
		double wf = 0;
		
		double s = fired[j];
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
	
	//nature used a triplet rule. keep old rule for now
	/**
	 * Doublet Spike Timing Dependent Plasticity
	 * @param i presynaptic neuron index
	 * @param j postsynaptic neuron index
	 * @param r_w 
	 * @param return weight change array [dij, dji]
	 */
	private double[] STDP(int i, int j, double dij, double dji){
		//TODO dij and ji local variables!! very dangerous

        if(neurons[i].isFiring()){
			double d = (0.1*Math.pow(0.95, (tau-STDPcount[j])));
			//check that neuron linked to i fired less than tau_ms before (but is not firing right now)
			//if j fired before i, then weight j->i ++
			if((weights[j][i] != 0.0) && (STDPcount[j]>0) && (STDPcount[j] != tau)){  //(STDPcount[j]>0) && 		
				dji += d;
            }
            
			//now weight from i to j, should be lowered if i fired before j
			if((weights[i][j] != 0.0) && (STDPcount[j]>0)  && (STDPcount[j] != tau)){
                dij -= d;
            }   
            
//            if(weights[j][i] == 0.0){
//            	//TODO implement connection probability as Zenke
//            	if(Constants.uniformDouble()>0.1){
//            		dji += d;
//            	}
//            }        
        }
        
        double[] r_w = {dij, dji};
        return r_w;
	}
	
	
	//STDP on IE connections according to Zenke nature paper
	//dji
	double iSTDP(int i, int j, double g){
		double dw = 0;
		double n = 2.0/100000; //learning rate
//TODO there was an error above. corrected not tested 		
//TODO tauSTDP never updated anywhere!!!
		dw = (z_minus[i]+1)*fired[j] + z_minus[j]*fired[i];
		dw = dw*g*n;
		
		return dw;
	}
	

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
//	public void stimulate(int begin, int size, double stimValue) {
//		for(int i=begin; i<begin+size; i++){			
//			neurons[i].addToI(stimValue);
//		}
//	}
	
	private List<Rectangle2D> lstShapes = new ArrayList<Rectangle2D>(25);//dirrrrty
	private void updateShapes(){
		int dot_size =1;
		
		int x = iteration%w;//
		x = x/50;
		
		if(x==0){
			lstShapes.clear();
		}
		for(int i=0;i<numberOfNeurons;i++){
			if(fired[i]==1){
			//g2d.fill(new Rectangle2D.Double(x,i*dot_size,dot_size,dot_size));
				lstShapes.add(new Rectangle2D.Double(x,i*dot_size,dot_size,dot_size));
			}
		}
	}
	
	public void draw(Graphics g, int gridStep) {
	
		
		Graphics2D g2d = (Graphics2D) g;

		//cleanup
		//g2d.setColor(Color.white);
		
		
		//black: draw raster
		g2d.setColor(Color.black);
		for (Rectangle2D rect : lstShapes) {
			g2d.fill(rect);
		}
		//x = x/10;
		//x = x*dot_size;
		//y (top = inhibitory)
//		for(int i=0;i<numberOfNeurons;i++){
//			//if(fired[i]==1){
//				//g2d.fill(new Rectangle2D.Double(x,i*dot_size,dot_size,dot_size));
//				display.addRectangle(new Rectangle2D.Double(x,i*dot_size,dot_size,dot_size));
//			//}
//		}
		
//		if(iteration%50==0){//TODO dirty dirty delay in Display class refresh
////			display.repaint();
//			g2d.fill(new Rectangle2D.Double(30,30,30,30));
////			for (Rectangle2D rect : lstShapes) {
////				g2d.fill(rect);
////	        }
//		}
	}

	
	public void setClusters(int inputStart, int inputSize, int outputStart, int outputSize, int outputBStart, int outputBSize){
		netGraph.setClusters(inputStart, inputSize, outputStart, outputSize, outputBStart, outputBSize);		
	}
}
