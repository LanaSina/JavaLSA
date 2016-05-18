package demos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.FileWriter;
import java.io.IOException;

import models.IzhNetwork;
import startup.Constants;
import visualization.Display;
import visualization.GraphicalComponent;
import communication.MyLog;

public class AlienHunt implements GraphicalComponent {
	/** log*/
	MyLog mlog = new MyLog("hunt", true);
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
	Display display;// = new Display();
	/** draw self or not*/
	boolean drawHunt = false;
	/** draw spiking graph or not*/
	boolean drawNet = false;
	/**runnable*/
	NetworkRunnable netThread;
	
	/**delay between 2 stimulations (sim ms)*/
	int stimDelay = 500;
	//int stimDelay2 = 500;
	/**is the network being stimulated or not (is there an alien)*/
	boolean stim = false;
	boolean stim2 = false;
	/** stimulation value (mV)*/
	double stimValue = 1;
	/** duration of proprioception (ms)*/
	//int propCepDelay = 100;
	int currentPropCep = 0;
	/** duration of the experiment*/
	int experimentTime = 600000;
	//scores
	int hitScore = 0;
	int missScore = 0;
	int hitScore2 = 0;
	int missScore2 = 0;
	//last shoot
	boolean missed = false;
	//
	int respTime = 0;
	
	//graphical parameters
	/** did the missile hit the alien? */
	boolean hit = false;
	/**draw missile for how much time?*/
	int missileDelay=0;
	/** stimulation time for random input stop experiment*/
	int randomStop = 0;
	
	public AlienHunt(){	
		init();
		run();
	}
	
	public AlienHunt(String name){
		this.name = name;
		init();
		run();
	}

	private void init(){
		
		randomStop = generateStimDelay()+8000;//between 9 000 and 10 000
		
		if((name.compareTo("")==0)){
			net = new IzhNetwork(drawNet);
		} else {
			mlog.setName("hunt"+name);
			net = new IzhNetwork(name, drawNet);
		}		
		if(drawNet){
			net.setClusters(20, 10, 30, 10, 40, 10);
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
			//mlog.say("stream opened "+Constants.ReacTimeFileName);
			str = "input,iteration\n";
			inputWriter.append(str);
			inputWriter.flush();
        	
        	hitWriter = new FileWriter(folderName+"\\"+Constants.AlienHitFileName);
			//mlog.say("stream opened "+Constants.ReacTimeFileName);
			str = "hitTime\n";
			hitWriter.append(str);
			hitWriter.flush();		
        	
        	missWriter = new FileWriter(folderName+"\\"+Constants.AlienMissFileName);
			//mlog.say("stream opened "+Constants.ReacTimeFileName);
			str = "missTime\n";
			missWriter.append(str);
			missWriter.flush();		
			
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
		
		//add self to graphic panel
		if(drawHunt){
			display = new Display();
			display.addComponent(this);
		}
	}
	
	private void  run(){
		netThread = new NetworkRunnable();
		new Thread(netThread).run();
	}
	
	void broadStimulation(){
		stimDelay--;
		respTime++;

		int iteration = net.getIteration();
		if(stimDelay<0)
			stim = true;

		/**conditions*/
		boolean c1=false;
		int clusFired[] = net.getFiringClusters();
		
		if((clusFired[3]>3) && (clusFired[4]<4)){
			c1 = true;
//			missileDelay = 10;
		}
		
		String strR = clusFired[3]+","+iteration+"\n";
		String strD = clusFired[4]+","+iteration+"\n";
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
				stim = false;
				stimDelay = (int) (Constants.uniformDouble(1000,2000) +0.5);
				
//				hitScore++;
//				try {
//					hitWriter.append(iteration+"\n");
//					hitWriter.flush();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
////				stim = false;
//				missed = false;
//				
				String str = -respTime+","+iteration+"\n";
				respTime = 0;
	        	try {
	        		reacTimeWriter.append(str);	        	
					reacTimeWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
//				
//				stimDelay = generateStimDelay();//(int) (Constants.uniformDouble(1000,2000) +0.5);
			} else{
//				missScore++;
//				missed = true;
//				try {
//					missWriter.append(iteration+"\n");
//					missWriter.flush();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}        	
		}
		
//		if(c2){
//			if(stim2){
//				hitScore2++;
//				mlog.say("hit 2 "+ hitScore2);
//				stim2 = false;
//				stimDelay2 = (int) (Constants.uniformDouble(1000,2000) +0.5);
//			} else{
//				missScore2++;
//				mlog.say("miss 2 "+ missScore2);
//				stim2 = false;
//			}
//		}

		if(stimDelay < -10000){//-randomStop
			respTime = 0;
			String str = -10000+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stimDelay = (int) (Constants.uniformDouble(1000,2000) +0.5);//generateStimDelay();//(int) (Constants.uniformDouble(1000,2000) +0.5);
			//randomStop = generateStimDelay()+8000;
			stim = false;
			mlog.say("reached limit \n");
		}
		
//		if(stimDelay2 < -10000){
//			
//			stimDelay2 = (int) (Constants.uniformDouble(1000,2000) +0.5);
//			stim2 = false;
//			mlog.say("reached limit2 \n");
//		}

		//alien
		String str = "0";
		if(stim && (stimDelay<0)){
			net.stimulate(20,10,stimValue);
			str = ""+stimValue;
		}
		
//		if(stim2 && (stimDelay2<0)){
//			net.stimulate(50,10,stimValue);
//		}
//		
		//proprioception
//		if(currentPropCep>0){
//			//mlog.say("prop");
//			net.stimulate(30,10,stimValue*10);
//		}
		
		//write input
		try {
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

	public void draw(Graphics g, int gridStep) {
		
		int length =120;
		int botHeight = 10;
		int alienHeight = 30;
		//drawing position y
		int drPosy = 500;
		//drawing position x
		int drPosx = 500;
		//ship width
		int width = 40;
		int pos = 0;
		//draw missile for how much time?
		//int missileDelay=0;
		
		Graphics2D g2d = (Graphics2D) g;

		//cleanup
		//g2d.setColor(Color.white);
		g2d.clearRect(0,400,1000,1000);

		//black: draw spaceship
		g2d.setColor(Color.black);
		//body
		int x = drPosx + pos*width;
		g2d.fill(new Rectangle2D.Double(x,drPosy+length,width,botHeight));
		
		//canon
		x = x+(width/2)-5;
		int y = drPosy+length-botHeight;
		g2d.fill(new Rectangle2D.Double(x,y,5,10));

		//draw missile
		missileDelay--;
		//default color
		Color color = Color.blue;	
		//drawing
		if(missed){
			color = Color.red;
		}
		if(missileDelay>0){
			g2d.setColor(color);
			y-=10;
			/*int[] xpoints = {x-5,x+10,x+7};
			int[] ypoints = {y,  y,  y-10};		
			//triangle
			g2d.drawPolygon(xpoints, ypoints, 3);
			g2d.fill(new Polygon.Double(xpoints, ypoints, 3));*/
			g2d.fill(new Rectangle2D.Double(x,y-20,5,10));
		}

		//draw alien green		
		if(stim){
			color = Color.ORANGE;
			g2d.setColor(color);
			//body
			x = drPosx;
			g2d.fill(new RoundRectangle2D.Double(x,drPosy,width,alienHeight,5, 5));//arc width and height
			//4 legs
			y = drPosy+alienHeight-5;
			int legWidth = 3;
			int mx = 0;
			for(int i=0;i<4;i++){
				mx = x + i*(width)/4;
				g2d.fill(new Rectangle2D.Double(mx,y,legWidth,15));
			}
			g2d.fill(new Rectangle2D.Double(x+width-legWidth,y,legWidth,15));
		}

		//score
		color = Color.red;
		g2d.setColor(color);
		String hitString = "Hit "+hitScore;
		String missString = "Miss "+missScore;
		g2d.drawString(hitString,drPosx+150,drPosy);
		//g2d.drawString(missString,drPosx+150,drPosy+40);
	}

}
