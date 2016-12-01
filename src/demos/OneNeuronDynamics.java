package demos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import communication.MyLog;
import models.IzhNeuron;
import startup.Constants;

public class OneNeuronDynamics {
	
	/** log*/
	MyLog mlog = new MyLog("neneuron", true);
	/** duration of the experiment*/
	int experimentTime = 300;
	NetworkRunnable netThread;
	IzhNeuron neuron;
	/** spikes time series */
	FileWriter membraneWriter;
	int iter = 0;
	
	public void  run(){
		neuron = new IzhNeuron();
		neuron.setNeuronType(Constants.NeurInhibitory);
		
		//get date
		 DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	    String	folderName = Constants.DataPath + "/" + strDate + "/";
		//create directory
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
		
		try {
			membraneWriter = new FileWriter(folderName+"/"+Constants.SpikesFileName);
			mlog.say("stream opened "+Constants.SpikesFileName);
	    	String str = "iteration,V\n";
	    	membraneWriter.append(str);
	    	membraneWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		netThread = new NetworkRunnable();
		new Thread(netThread).run();
		
	}
	
	private void updateNeuron() {
		double in = 4;//.1;
		neuron.addToI(in);
		neuron.update();
        neuron.checkFiring();
		double v = neuron.getV();
		neuron.setI(0);
		
		try {
			
	    	String str = iter+","+v+"\n";
	    	membraneWriter.append(str);
	    	membraneWriter.flush();
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

			
			while(run){
				
				updateNeuron();
				iter++;
				
				if(iter>=experimentTime){
					mlog.say("end of experiment at t="+iter);
					kill();
				}
			}
			
			try {
				membraneWriter.flush();
				membraneWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	

}

