package demos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import models.IzhNetwork;
import startup.Constants;
import sun.applet.Main;
import visualization.Display;
import visualization.GraphicalComponent;

import communication.MyLog;

public class Polychrony implements GraphicalComponent {

	/** log*/
	MyLog mlog = new MyLog("polychrony", true);
	IzhNetwork net;
	String name = "";
	/** reaction time */
	FileWriter reacTimeWriter;	
	
	/**graphics displaying object*/
	Display display;// = new Display();
	/** draw self or not*/
	boolean drawHunt = true;
	/** draw spiking graph or not*/
	boolean drawNet = true;
	/**runnable*/
	NetworkRunnable netThread;
	/** total experiment length*/
	int experimentTime = 1000000;
	
	/**delay between 2 stimulations (sim ms)*/
	int stimDelay = 500;
	/**delay between 2 iterations*/
	int itDelay = 0;
	/**is the network being stimulated or not (is there an alien)*/
	boolean stim = false;
	/** stimulation value (mV)*/
	double stimValue = 2;
	/**scores*/
	int hitScore = 0, missScore = 0;
	/** polychronization step*/
	int step = 0;
	/** correct launch code pattern*/
	int launchCode[] = {7,5,6,8};
	
	
	public Polychrony(){	
		init();
		run();		
	}
	
	public Polychrony(String name){
		this.name = name;
		init();
		run();
	}

	private void init(){
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
	
	/**
	 * checks if the network is firing the expected several-step pattern or not.
	 * @param pattern the expected (correct) pattern
	 * @param clusters array of clusters state (firing or not)
	 * @param start id of first output cluster
	 * @param size number of output clusters
	 * @param step current step in the firing pattern
	 * @return step+1 if expected cluster was the only one to fire; 
	 * step if no output cluster fired;
	 * -1 if an incorrect cluster fired.
	 */
	int checkPattern(int pattern[], boolean[] clusters, int start, int size, int step){
		//which cluster should be firing now?
		int which = pattern[step];
		//return value
		int returnStep = step;
		boolean ok = false;
		
		for(int i=start; i<start+size; i++){
			//if this cluster is firing
			if(clusters[i]==true){
				//mlog.say("fired " +i);
				//if it is the expected cluster increase one step
				if(i==which){
					//returnStep=step+1; don't change returnstep in loop
					ok = true;
					//mlog.say("step "+returnStep);
				}else{//if another cluster is firing start from 0
					ok = false;
					returnStep = 0; //changing only var ok would lead to confusion with no one firing
					mlog.say("minus "+i);
					break;
				}
			}
		}	
		
		if(ok) {
			returnStep=step+1;
			mlog.say("step "+returnStep);
		}
		//mlog.say("step "+returnStep);
		return returnStep;
	}
	
	void broadStimulation(){
		stimDelay--;
	
		int iteration = net.getIteration();
		if(stimDelay<0)
			stim = true;

		int clusFired[] = net.getFiringClusters();
		int n = clusFired.length;
		boolean cfired[] = new boolean[n];		
		for(int i=0;i<n;i++){
			if(clusFired[i]>3){
				//mlog.say("fired");
				cfired[i] = true;
			}else {
				cfired[i] = false;
			}
		}
		
		step = checkPattern(launchCode, cfired, 5, 4, step);
		
		if(step==launchCode.length){
			//playSound("piano_D4.wav");
			step = 0;
			stim = false;
			hitScore++;
			String str = stimDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mlog.say("OK, rt "+ stimDelay+" \n");
        	stimDelay = (int) (Constants.uniformDouble(800,1200) +0.5);
		} else if(step==-1){
			step = 0;
		}

		if(stimDelay < -10000){
			String str = stimDelay+","+iteration+"\n";
        	try {
        		reacTimeWriter.append(str);	        	
				reacTimeWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stimDelay = (int) (Constants.uniformDouble(1000,2000) +0.5);
			stim = false;
			mlog.say("reached limit \n");
		}

		//alien
		if(stim && (stimDelay<0)){
			double stv = stimValue/(step*0.5+1);
			net.stimulate(20,10,stv);
		}
	}	
	
	void closeStreams(){
		mlog.say("closing streams");
		
		//first write final values
//		String folderName = net.getDataFolder();
//		try {
//			FileWriter finalValuesWriter = new FileWriter(folderName+"\\"+Constants.FinalValuesFileName);
//			mlog.say("stream opened "+Constants.FinalValuesFileName);
//			String str = "totalHits,totalMisses\n";
//			finalValuesWriter.append(str);
//			finalValuesWriter.flush();		
//			str = hitScore+","+missScore+"\n";
//			finalValuesWriter.append(str);
//			finalValuesWriter.flush();	
//			finalValuesWriter.close();
//			
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
    	
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
	
	//sound (wav) piano_D4
	public static synchronized void playSound(final String url) {
		  new Thread(new Runnable() {
		  // The wrapper thread is unnecessary, unless it blocks on the
		  // Clip finishing; see comments.
		    public void run() {
		      try {
		        Clip clip = AudioSystem.getClip();
		        
		        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		        //InputStream input = classLoader.getResourceAsStream("/image.gif");
		        
		        AudioInputStream inputStream = AudioSystem.getAudioInputStream(
		          Main.class.getResourceAsStream(Constants.SoundPath+"/"+url));// +"\\"+
		        clip.open(inputStream);
		        clip.start(); 
		      } catch (Exception e) {
		        System.err.println(e.getMessage());
		      }
		    }
		  }).start();
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
		int missileDelay=0;
		
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
		Color color = Color.red;	
		//drawing
		if(!stim){
			color = Color.blue;
			/*if(s==2){
				casualities+=money;
			}*/
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
			color = Color.green;
			g2d.setColor(color);
			//body
			x = drPosx;
			g2d.fill(new RoundRectangle2D.Double(x,drPosy,width,alienHeight,5, 5));//arc widht and height
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
		g2d.drawString(hitString,drPosx+150,drPosy);
	}
}
