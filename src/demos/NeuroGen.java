package demos;

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
import java.util.Random;

import communication.MyLog;
import models.IzhNeuron;
import startup.Constants;
import visualization.Display;
import visualization.GraphicalComponent;

/**
 * Growing net, growing body
 * @author lana
 *
 */
public class NeuroGen {
	/** log */
	MyLog mlog = new MyLog("NeuroGenNet", true);
	/**graphics displaying object*/
	Display display;
	
	/** simulation time (age of the network)*/
	int iteration = 0;
	/** initial resolution (x=y)*/
	int[] res = new int[2];
	/** current position*/
	double[] position = new double[2];
	/** total number of neurons in the network */
	int numberOfNeurons;
	/** manimum number of neurons*/
	int MAX_NEURONS = 100;
	/** manimum number of neurons*/
	int maxWeight = 15;
	//weights
	double[][] weights = new double[MAX_NEURONS][MAX_NEURONS];
	/** stdp window */
	int tau = Constants.tau;
	/**stdp tau*/
	int[] STDPcount;
	/**random normal generator*/
	Random noiseRand = new Random();
	double maxNoise = 5;//5;
	//?
	int alarm = -1;
	//game class
	Hand hand = new Hand();
	
	//all neurons
	IzhNeuron[] neurons = new IzhNeuron[MAX_NEURONS];
	//lists: motor neurons
	/** motor neurons: minus x*/
	ArrayList<Integer> motor_mx = new ArrayList<Integer>();
	/** motor neurons: plus x*/
	ArrayList<Integer> motor_px = new ArrayList<Integer>();
	/** motor neurons: minus y*/
	ArrayList<Integer> motor_my = new ArrayList<Integer>();
	/** motor neurons: plus y*/
	ArrayList<Integer> motor_py = new ArrayList<Integer>();
	// proprioception neurons
	ArrayList<Integer> pro_x = new ArrayList<Integer>();
	ArrayList<Integer> pro_y = new ArrayList<Integer>();
	
	//delay between 2 iterations
	int it_delay = 2;
	//noise range
	double noise = 5;//5;
	double score = 0;
//	double maxNoise = 5;
	double maxSW = maxWeight;
	
	/**data directory*/
	String folderName;
	/** records weight matrix*/
	FileWriter weightsWriter;
	//csv files
	/** spikes time series */
	FileWriter spikesWriter;	
	

	public NeuroGen(){
		init();
		run();
	}
	
	void init(){
		STDPcount = new int[MAX_NEURONS];
		
		numberOfNeurons = 2; //??
		position[0] = 0;
		position[1] = 0;
		res[0] = 2;
		res[1] = 2;
		
		hand.setup();
		hand.setRes(res);
		//minimum numbers
		//for (int i = 0; i < 2; i++){//waaait wait what is this loop?
		int n = 0;
			motor_px.add(n);//x
			n++;
			motor_py.add(n);//y
			n++;
			pro_x.add(n);//resolution is 2
			n++;
			pro_x.add(n);//x1
			n++;
			pro_y.add(n); // one for each cell
			n++;
			pro_y.add(n);
			n++;
			motor_mx.add(n);
			n++;
			motor_my.add(n);
			n++;
		//}
		numberOfNeurons = n;
		mlog.say(n+ " neurons");
	
		neurons = new IzhNeuron[MAX_NEURONS];
		initNeurons();
		
		 //get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	
	    folderName = Constants.DataPath + "/" + strDate + "/";
		initDataFiles();
		writeWeights();	
		
		display = new Display();
		display.setName("Development");
		display.addComponent(hand);
	}
	
	void initNeurons(){
	 //   tme = 0;
	    
	    //no inh neurons
	    for(int i=0; i<MAX_NEURONS; i++){
	    	neurons[i] = new IzhNeuron();
			neurons[i].setNeuronType(Constants.NeurExcitatory);
	    }
	    
	  //initialization of weights to 0
  		for(int i=0; i<MAX_NEURONS; i++){
  	        for(int j=0; j<MAX_NEURONS; j++){
  				weights[i][j] = 0;
  			}
  		}
	  		
		for(int i=0; i<MAX_NEURONS; i++){
	        for(int j=0; j<MAX_NEURONS; j++){
				//chem[i][j] = 0;
				weights[i][j] = 0.1;
				if(i==j){
					weights[i][j] = 0;
				} else if(i<numberOfNeurons && j<numberOfNeurons){
					weights[i][j] = maxWeight;// (double)maxWeight/numberOfNeurons;
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
	
	
	void update(){
			//motor neurons update
			int mx = 0;
			for (int i = 0; i < motor_mx.size(); i++){
				if(neurons[motor_mx.get(i)].isFiring()){
					mx++;
				}
			}
			int px = 0;
			for (int i = 0; i < motor_px.size(); i++){
				if(neurons[motor_px.get(i)].isFiring()){
					px++;
				}
			}			
			int py = 0;
			for (int i = 0; i < motor_py.size(); i++){
				if(neurons[motor_py.get(i)].isFiring()){
					py++;
				}
			}	
			
			int my = 0;
			for (int i = 0; i < motor_my.size(); i++){
				if(neurons[motor_my.get(i)].isFiring()){
					my++;
				}
			}
			//mlog.say("mx "+mx+" py "+ py);
			
			//motion factor
			double f = 0.4;//0.2
			double move = f*(px-mx);
			position[0] += move;
			move = f*(py-my);
			position[1] += move;
			for (int i = 0; i < 2; i++){
				if(position[i]<0) position[i]=0;
				if (position[i]>(res[i]-1)) position[i] = res[i]-1;
			}
			//mlog.say("pox "+position[0]+" poy "+ position[1]);

			hand.updatePos((int)position[0], (int)position[1]);
			stimulation();

			STDP();
			
			//update neurons
			for(int i=0; i<numberOfNeurons; i++){
				neurons[i].addToI(noise*noiseRand.nextGaussian());	
				///mlog.say(""+neurons[i].getV());
		        for(int j=0; j<numberOfNeurons; j++){
		            if((i != j) && neurons[i].isFiring()){
						//if fired recently, weight small
						/*int t = count2-chem[i][j];
						double w;
						if(t>10){
							w = weight[i][j];
						} else {
							w = weight[i][j]*(t)/15;
						}*/
						neurons[j].addToI(weights[i][j]);//w
						//chem[i][j] = count2;					 
					}
		        }  
		        
		      
		    }
			
		    for(int i=0; i<numberOfNeurons; i++){
		        neurons[i].update();
		        neurons[i].checkFiring();
		        neurons[i].setI(0);
		    }
		    
			iteration++;
		    if((iteration%10000) == 0){
				writeWeights();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    	mlog.say("step "+iteration);
		    }
		    
		    try {
				Thread.sleep(it_delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
	
	void stimulation(){
		alarm--;//??

		//??	
		double st = 0.8;
		//stim in proprioception neurons
		int stim = 10;
		neurons[pro_x.get((int)position[0])].addToI(stim); //pas solide
		neurons[pro_y.get((int)position[1])].addToI(stim); //pas solide

		//score decay
		if(score>0){
			score-=0.0025/((res[0]-1)*(res[0]-1));
		}
		//hand??
		boolean success = hand.update();//TODO
		if(success){
			mlog.say("Success");
			score+=1;
			int[] temp = hand.getPos();
			position[0] = temp[0];
			position[1] = temp[1];
			//reduce noise (?)
			noise = maxNoise/2;
			if(noise<2) noise = 2;
			alarm = 500;
		}

		if(score>=5){
			score=0;
			maxNoise = maxNoise/1.5;
			if(maxNoise<2) maxNoise =2;
			//double resolution
			int r = res[0]*2;
			res[0] = r;
			res[1] = r;
			hand.setRes(res);
			//add sensory neurons
			
			pro_x = renewVector(pro_x);
			pro_y = renewVector(pro_y);
		}

		if(alarm==0){
			//cout << "noise back\n";
			noise = maxNoise;
		}

	}
	
	/**
	 * add neurons in sandwiched way for some reason
	 * @param source source vector
	 * @return augmented vector
	 */
	private ArrayList<Integer> renewVector(ArrayList<Integer> source){
		int size = source.size();
		ArrayList<Integer> tempVec = new ArrayList<Integer>();
		for (int j = 0; j < size ; j++){
			int n = source.get(j);
			if(numberOfNeurons<MAX_NEURONS){
				//init and copy weights
				for (int i = 0; i < numberOfNeurons; i++){
					//out
					weights[numberOfNeurons][i] = weights[n][i]+0.1;
					//in
					weights[n][numberOfNeurons] = weights[n][j]+0.1;
				}
				weights[numberOfNeurons][numberOfNeurons]=0;
				//add neuron and copy
				tempVec.add(n);
				tempVec.add(numberOfNeurons);
				numberOfNeurons++;
			}
		}
		return tempVec;
	}
	
	
	void STDP(){
		
		//mlog.say("a");
	    
		//reinitialize tau for the neurons that have fired
	    for(int i=0; i<numberOfNeurons; i++){
	        if(STDPcount[i]>0) STDPcount[i]--;
	        if(neurons[i].isFiring()){
	        	STDPcount[i] = tau;
	        	//mlog.say(i+" fired ");
	        	String str = i+","+iteration+"\n";
	        	try {
	        		spikesWriter.append(str);	        	
					spikesWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }

		for(int i=0; i<numberOfNeurons; i++){

	        if(neurons[i].isFiring()){
				for(int j=0; j<numberOfNeurons; j++){
				
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
	
	
	/** record weights in csv file*/
	void writeWeights(){
		openStream();
		
		try {
			for(int i=0; i<MAX_NEURONS; i++){
		        for(int j=0; j<MAX_NEURONS; j++){
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
	
	
	public void  run(){
		NetworkRunnable netThread = new NetworkRunnable();
		new Thread(netThread).run();
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
				update();	
				
				try {
					//mlog.say("hey");
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class Hand implements GraphicalComponent{
		//score
		//mouth position
		int mouth[] = {50,100};
		//field size {width,height}
		int field[]={200,200};

		//resolution
		int res[] = {2,2};
		int ux,uy;
		//mouth
		int[] m = new int[2];

		int points;
		//hand coordinates
		int[] c = new int[2];
		//drawing position x
		int drawX;
		//drawing position y
		int drawY;


		void setup(){
			points = 0;
			//hand coordinates
			c[0] = 0;
			c[1] = 0;
			//drawing position y
			drawX = 500;
			//drawing position x
			drawY = 500;

			newPosition();
			setRes(res);
		}

		void newPosition(){
			c[0]= (int) (Constants.uniformDouble()*res[0]+0.5);
			c[1] = (int) (Constants.uniformDouble()*res[0]+0.5);
		}

		//set resolution
		void setRes(int r[]){
			//mouth position
			res[0] = r[0];
			res[1] = r[0];
			ux = field[0]/res[0];
			uy = field[1]/res[1];
			m[0] = mouth[0]/ux;
			m[1] = mouth[1]/uy;
			newPosition();
		}

		
		boolean update(){
			//is hand in mouth ?
			boolean success = false;

			//hand
			//int h[] = {c[0]/ux,c[1]/uy,};

			if((c[0]==m[0]) && (c[1]==m[1])){
				success= true;
				newPosition();
			}

			return success;
		}


		void updatePos(int x, int y){
			c[0] = x;
			c[1] = y;
		}
		

		int[] getPos(){
			return c;
		}


		@Override
		public void draw(Graphics g, int gridStep) {
			
			Graphics2D g2d = (Graphics2D) g;

			//cleanup
			g2d.clearRect(0,400,1000,1000);


			//black: draw grid
			g2d.setColor(Color.black);

			//draw mouth
			int x = drawX + m[0]*ux;
			int y = drawY + field[1] - m[1]*uy;
			g2d.fill(new Rectangle2D.Double(x,y,ux,uy));

			//hand
			x = drawX + c[0]*ux;
			y = drawY + field[1] - c[1]*uy;
			g2d.setColor(Color.blue);
			g2d.fill(new Rectangle2D.Double(x,y,ux,uy));
			//what?
//			color = 255;
//			ofSetColor(0, 0, color);
//			ofRect(x,y,ux,uy);

			//score
			/*color = 255;
			ofSetColor(color, 0, 0);
			ofDrawBitmapString("Bounty",drPosx-200,drPosy);
			ofDrawBitmapString(ofToString(shootScore,3),drPosx-100,drPosy);*/
			
		}
	}
	
	
	
}
