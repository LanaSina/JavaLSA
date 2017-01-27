/**
 * 
 */
package startup;

import demos.AlienHunt;
import demos.OneNeuronDynamics;
import demos.WallAvoidance;

/**
 * This class is the main class.
 * Test svn relocate. 2015/10/14 17:47
 * @author lana
 */
public class Starter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/* demo for chain of 2 or 3 neurons*/
		//Chain demo = new Chain();
		
		/* demo for alien destroyer game*/
		//AlienHunt hunt = new AlienHunt();

		/* parameter search*/
		//(explore connectance and variance of weights)	
		/*double variance = 2;
		for(int connections = 30; connections<51; connections=connections+1){//150
			String folder = "parameterSearch/detailed/var"+variance+"connections"+connections;
			for(int i=0;i<10;i++){
				ExperimentThread t = new ExperimentThread(i);
				t.setFolder(folder);	
				t.setWVar(variance);
				t.setConnections(connections);
				new Thread(t).start();
			}
		}*/
		
		
		/* wall avoidance and other demos are run in this thread*/	
		//separate folder can be set to save data for entire batches of experiments
//		String folder = "testFolder";
		int i = 0;
		//uncomment this loop to run batches of experiments instead of just one experiment
//		//for(int i=0;i<20;i++){
		
			ExperimentThread t = new ExperimentThread(i);
//			t.setFolder(folder);			
			new Thread(t).start();
//		//}
		
		//Demo for recording the membrane potential of one neuron
		/*OneNeuronDynamics n = new OneNeuronDynamics();
		n.run();*/
			
	}

	public static class ShutdownThread implements Runnable {

		AlienHunt hunt;
			public ShutdownThread(AlienHunt h){
			hunt = h;
		}
		
		public void run() {
			hunt.globalKill();
		}
		
	}
	
	/**there is already a thread in simpleExperiment class but this is an alternative*/
	public static class ExperimentThread implements Runnable {

		int id;
		String folder = "";
		double wvar = 0;
		int connections = 0;
		
		ExperimentThread(int id){
			this.id = id;
		}
		
		public void setFolder(String f){
			folder = f;
		}
		
		public void setConnections(int c){
			connections = c;
		}
		
		public void setWVar(double v){
			wvar = v;
		}
		
	    public void run() {
	    	//uncomment any of the following demos
	    	//new AlienHunt("AlienHunt"+id);
	    	//new ExplicitPruning2(""+id);
	    	//new SimpleMemory("");
	    	//IzhMemoryNet n = new IzhMemoryNet(false);
	    	//NeuralAssembly n = new NeuralAssembly();
	    	//new BigMemory("");
	    	//new Chain(""+id);
	    	
	    	new WallAvoidance("");//+id
	    	//use this line if runing experiments by batches
	    	//new WallAvoidance(""+id);
	    	
	    	/*SimpleExperiment2 s = new SimpleExperiment2(""+id);
	    	s.setSupFolder(folder);
	    	s.setWVar(wvar);
	    	s.setConnections(connections);
	    	s.makeNet();
	    	s.launch();*/
	    }
	}
}
