package demos;

import java.io.FileWriter;
import java.io.IOException;

import models.IzhNetwork;
import startup.Constants;
import visualization.Display;
import communication.MyLog;


/**
 * LIke Alien Hunt but output B causes stimulation in Input B.
 * @author lana
 *
 */
public class ExplicitPruning {

	/** log*/
	MyLog mlog = new MyLog("pruning", true);
	IzhNetwork net;
	String name = "";
	/** reaction time */
	FileWriter reacTimeWriter;	
	/** hits writer*/
	FileWriter hitWriter;
	/** misses writer*/
	FileWriter missWriter;
	/** dummy cluster*/
	FileWriter dummyWriter;
	/** real cluster*/
	FileWriter realWriter;
	/** time and value of input*/
	FileWriter inputWriter;
	/**graphics displaying object*/
	Display display;
	/** draw spiking graph or not*/
	boolean drawNet =false;// true;
	/**runnable*/
	NetworkRunnable netThread;
	
	/**delay between 2 stimulations (sim ms)*/
	int pauseDelay;
	int stimDelay2;
	/**is the network being stimulated or not*/
	boolean stim = false; //input1
	boolean stim2 = false; //input2
	/** stimulation value (mV)*/
	double stimValue = 1;//1
	/** duration of the experiment*/
	int experimentTime = 500000;
	//scores
	int hitScore = 0;
	int missScore = 0;
	int hitScore2 = 0;
	int missScore2 = 0;
	//last shoot
	boolean missed = false;
	
	
	int input1 = 2;//10
	//int input2 = 2;//whole net actually
	int output1 = 3;
	int output2 = 4;
			
	public ExplicitPruning(){	
		init();
		run();
	}
	
	public ExplicitPruning(String name){
		this.name = name;
		init();
		run();
	}

	private void init(){
		
		pauseDelay = generateStimDelay();
		stimDelay2 = (int) (Constants.uniformDouble(2000,3000) +0.5);
				
		if((name.compareTo("")==0)){
			net = new IzhNetwork(drawNet);
		} else {
			mlog.setName("hunt"+name);
			net = new IzhNetwork(name, drawNet);
		}		

		String folderName = net.getDataFolder();
		//now create csv files
		try {					
			reacTimeWriter = new FileWriter(folderName+"\\"+Constants.ReacTimeFileName);
			mlog.say("stream opened "+Constants.ReacTimeFileName);
			String str = "reactionTime,iteration\n";
        	reacTimeWriter.append(str);
        	reacTimeWriter.flush();		
        	
        	inputWriter = new FileWriter(folderName+"\\"+Constants.InputValueFileName);
			str = "input,iteration\n";
			inputWriter.append(str);
			inputWriter.flush();
			
			hitWriter = new FileWriter(folderName+"\\"+Constants.AlienHitFileName);
			str = "hitTime\n";
			hitWriter.append(str);
			hitWriter.flush();		
        	
        	missWriter = new FileWriter(folderName+"\\"+Constants.AlienMissFileName);
			str = "missTime\n";
			missWriter.append(str);
			missWriter.flush();		
			
			
			//writes when dummy fired and how many neurons
			dummyWriter = new FileWriter(folderName+"\\"+Constants.DummyClusterFileName);
			str = "neurons,iteration\n";
        	dummyWriter.append(str);
        	dummyWriter.flush();	
        	
			realWriter = new FileWriter(folderName+"\\"+Constants.RealClusterFileName);
			str = "neurons,iteration\n";
        	realWriter.append(str);
        	realWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void  run(){
		netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	void broadStimulation(){
		pauseDelay--;
		stimDelay2--;
		
		int iteration = net.getIteration();
		if(pauseDelay<0)
			stim = true;

		/**conditions*/
		boolean c1=false,c2=false;
		int clusFired[] = net.getFiringClusters();
		
		if((clusFired[output1]>3) & (clusFired[output2]<4)){//
			c1 = true;
		}
		
		//dummy
		if((clusFired[output2]>4)){//&(clusFired[output1]<4)
			c2 = true;
		}
		
		String strR = clusFired[output1]+","+iteration+"\n";
		String strD = clusFired[output2]+","+iteration+"\n";
    	try {
    		realWriter.append(strR);	        	
			realWriter.flush();
    		dummyWriter.append(strD);	        	
			dummyWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(c1){
			if(stim){
				hitScore++;
				try {
					hitWriter.append(iteration+"\n");
					hitWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				stim = false;
				missed = false;
				
				String str = pauseDelay+","+iteration+"\n";
	        	try {
	        		reacTimeWriter.append(str);	        	
					reacTimeWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				pauseDelay = generateStimDelay();//(int) (Constants.uniformDouble(1000,2000) +0.5);
			} else{
				missScore++;
				missed = true;
				try {
					missWriter.append(iteration+"\n");
					missWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}        	
		}
		
		if(c2){
			stim2 = true;
			stimDelay2 = 5;//0;//(int) (Constants.uniformDouble(100,200) +0.5);
			mlog.say("dummy firing" +strD);
		}

		if(pauseDelay < -10000){
			String str = pauseDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			pauseDelay = generateStimDelay();//(int) (Constants.uniformDouble(1000,2000) +0.5);
			stim = false;
			mlog.say("reached limit \n");
		}
		
		if(stimDelay2 < 0){
			stim2 = false;
		}

		//input1
		if(stim){//&& (pauseDelay<0)
			net.stimulate(input1*10,10,stimValue);
		}
		
//		//input2
//		if(stim2){
//			net.stimulate(20,20,stimValue);//avoid 40-50
//			net.stimulate(50,50,stimValue);
//		}
		
		//write input
		try {
			String str;
			if(!stim) str = "0";
			else str = ""+stimValue;
			inputWriter.append(str+","+iteration+"\n");
			inputWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int generateStimDelay(){
		int delay = (int) (Constants.uniformDouble(1000,2000) +0.5);
		return delay;
	}
	
	
	void closeStreams(){
		mlog.say("closing streams");
		
		//first write final values
		String folderName = net.getDataFolder();
		try {
			FileWriter finalValuesWriter = new FileWriter(folderName+"\\"+Constants.FinalValuesFileName);
			mlog.say("stream opened "+Constants.FinalValuesFileName);
			String str = "totalHits,totalMisses\n";
			finalValuesWriter.append(str);
			finalValuesWriter.flush();		
			str = hitScore+","+missScore+"\n";
			finalValuesWriter.append(str);
			finalValuesWriter.flush();	
			finalValuesWriter.close();
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	
		try {
			reacTimeWriter.flush();
			reacTimeWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		net.closeStreams();
	}
	
	
	private class NetworkRunnable implements Runnable{
		boolean run = true;
		MyLog mlog = new MyLog("networkRunnable",true);
		
		public void kill(){
			run = false;
		}
		
		//@Override
		public void run() {			
			int iter = 0;
			
			while(run){
				
				net.updateNeurons();
				broadStimulation();
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


	public void globalKill() {
		mlog.say("kill");
		netThread.kill();
	}
}
