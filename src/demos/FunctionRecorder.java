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

public class FunctionRecorder {

	/** to apply Euler forward method on equations and record it in csv*/
	public static void main(String[] args) {
		MyLog mlog = new MyLog("functionRecorder", true);
		String folderName;
		FileWriter writer = null;
		
		//one neuron
		IzhNeuron neuron = new IzhNeuron();
		//AIAFNeuron neuron = new AIAFNeuron();
		neuron.setNeuronType(Constants.NeurExcitatory);
		//spike trace
		int s = 0;
		//u
		double u = 0;
		double x = 1;
		
		//get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	
	    folderName = Constants.DataPath + "/" + strDate + "/";
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
		//create csv file
		String fname = "testIzh.csv";
		try {			
			writer = new FileWriter(folderName+"/"+fname);
			mlog.say("stream opened "+fname);
        	String str = "iteration,factor,u,x,membrane\n";
        	writer.append(str);
        	writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}			
		
		//time step = 1ms
		//duration
		int duration = 1000;//ms
		//stimulation level
		double stim = 8; //30; //mV
		//number of close spikes
		int time = 0;
		for(int i=0; i<duration; i++){
			time++;
			//update neuron
			//neuron.checkFiring();
			//update trace
			if(neuron.isFiring()) {
				s = 1;
			}else{
				s = 0;
			}
			
			double[] res = STP(s,u,x);
		    double wf = res[0];
		    u = res[1];
		    x = res[2];
		    //record
		    String str = time +"," + wf+","+u+","+x + ","+ neuron.getV() +"\n";
        	try {
				writer.append(str);
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	    
			
			//update communications
			//neuron.addToI(noise*noiseRand.nextGaussian());
			//if(time<200){//%25 ==0  && sp>0){
				neuron.addToI(stim);
						
				
			/*}
			if(time > 400){//one last spike
				neuron.addToIexc(stim);
			}*/
			
			//update neuron states
		    neuron.update();
		    neuron.checkFiring();
			neuron.setI(0);	
		    //neuron.setI(0);	   
		}

	}
	
	//STP in nature paper Diverse plasticity rules orchestrated...
	/**
	 * Only Exc neurons
	 * @param s synaptic trace
	 * @param u previous u variable
	 * @param x previous x
	 * @return {wf,new u, new x}
	 */
	static double[] natSTP(int s, double u , double x){
		double wf = 0;
		double tau_d = 200;//ms
		double tau_f = 600;//ms
		double U = 0.2;//mV
		
		double dx = (1-x)/tau_d - u*x*s;
		double du = (U - u)/tau_f + U*(1-u)*s;
		
		double nu = u+du;
		double nx = x+dx;
		
		wf = nu*nx;
		double[] res = {wf,nu,nx};
		return res;
	}
	
	
	//STP in Synaptic Theory of Working Memory-support
	//returns 
	/**
	 * @param s synaptic trace
	 * @param u previous u variable
	 * @param x previous x
	 * @return {wf,new u, new x}
	 */
	static double[] STP(int s, double u , double x){
		double wf = 0;
		double bigU = 0.2;
		double tauF = 1500;//ms
		double tauD = 200;//ms
		double du = 0;
		
		//update u
		du = (bigU-u)/tauF + bigU*(1-u)*s;		  
		double nu = u+du;
		
		//update x
		double dx = (1-x)/tauD - u*x*s;
		double nx = x+dx;
		
		wf = nu*nx;//no delay
		
		double[] res = {wf,nu,nx};
		return res;
	}

}
