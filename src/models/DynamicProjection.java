package models;

public class DynamicProjection {

	public static void main(String[] args) {
		
		
		
		new Thread(new ExperimentThread()).start();

		
	}
	
	public static class ExperimentThread implements Runnable {
		DynProjNet net = new DynProjNet();
	    public void run() {
	    	while(true){
	    		net.update();
	    		
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }

	}
	
	
}
