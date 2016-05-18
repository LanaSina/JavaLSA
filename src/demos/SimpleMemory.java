package demos;

import communication.MyLog;
import models.NatMemoryNetwork;
//import models.IzhNetwork;
import models.SciMemoryNetwork;

public class SimpleMemory {
	/** log*/
	MyLog mlog = new MyLog("SimpleMem", true);
	
	NetworkRunnable netThread;
	/** duration of the experiment*/
	int experimentTime = 600000;
	
	//SciMemoryNetwork net;
	NatMemoryNetwork net;
	boolean drawNet = false;
	String name="";
	
	public SimpleMemory(String name){
		this.name = name;
		init();
		run();
	}
	
	private void init(){
		if((name.compareTo("")==0)){
			net = new NatMemoryNetwork(drawNet);
		} else {
			mlog.setName("mem"+name);
			net = new NatMemoryNetwork(name, drawNet);
		}	
	}

	private void  run(){
		netThread = new NetworkRunnable();
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
			int iter = 0;
			
			while(run){
				
				net.updateNeurons();
				iter++;
				
//				if(iter>=experimentTime){
//					mlog.say("end of experiment"+net.getName()+" at t="+iter);
//					kill();
//				}
				/*try {
					//mlog.say("hey");
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/				
			}
			
		//	closeStreams();
		}
	}


	public void globalKill() {
		mlog.say("kill");
		netThread.kill();
	}

}
