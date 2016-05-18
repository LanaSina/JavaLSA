/**
 * 
 */
package models;

import startup.Constants;

/**
 * A neuron built according to the Izhikevich model.
 * @author lana
 *
 */

public class IzhNeuron {

	/** true if neuron is currently firing*/
	private boolean firing;
	/** type of neuron - see file Constants.java*/
    int type;
    
    /**threshold to emit a spike*/
    private double potentialThreshold;
    /** parameters for the differential equation*/
    private double a, b, c, d;
    /** membrane recovery variable */
    private double u;
    /** membrane potential*/
    double v;
	/** input (mV)*/
    double I;
    
    /**
     * Constructor of a neuron with FS dynamics
     */
    public IzhNeuron(){
    	//initialization
        firing = false;    
        //set parameters as default
        double re = Constants.uniformDouble();
        a = 0.02;
        b = 0.2;
        c = -65 + 15*re*re; //default according to Izh paper
        d = 8 - 6*re*re;
        v = -65;
        u = b*v;
        I = 0;
        potentialThreshold = 20;
    }
    
    
    /**
     *  Set parameters to obtain fixed dynamics: FS neuron, RS neuron etc
     *  See Inzhievich paper to chose parameters values
     *  or use function setNeuronType.
     * @param type inhibitory, excitatory (see Constants file)
     * @param a
     * @param b
     * @param c
     * @param d
     */
   public void setParam(int type, double a, double b, double c, double d){
        this.type = type;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    
    /**
     * Set neuron dynamics
     * @param type see Constants file for neuron types
     */
    public void setNeuronType(int type){
        this.type = type;
        switch(type){
            case Constants.NeurExcitatory:
            {
    			a = 0.02;
    			b = 0.2;
    			c = -65;
    			d = 8.0;
                break;
            }
            case Constants.NeurInhibitory:
            {
    			a = 0.1;
    			b = 0.2;
    			c = -65;
    			d = 2.0;
                break;
            }                
        }
        v = -65;
        u = d;
        I = 0;
    }    
    

    /**
     * Update neuron dynamics
     */
    public void update(){    
	    v = v + 0.5*(0.04*v*v + 5*v +140 - u + I);
	    v = v + 0.5*(0.04*v*v + 5*v +140 - u + I);
	    u = u + a*(b*v-u);
	
		//time++;
	}

    /** 
     * calculate if neuron should be in firing state
     * @return true if neuron is firing
     */
    public boolean checkFiring(){
	    if(v>potentialThreshold) {
	        firing = true;
	        v = c;
	        u = u + d;
			/*?
			f = lastFired-preLastFired;
			preLastFired = lastFired;
			lastFired = time;*/
	    }else{
	        firing = false;
	    }
	    return(firing);
	}


	public boolean isFiring() {
		return firing;
	}


	public void setFiring(boolean firing) {
		this.firing = firing;
	}


	public int getType() {
		return type;
	}

	public double getV() {
		return v;
	}


	public void setV(double v) {
		this.v = v;
	}


	public double getI() {
		return I;
	}


	public void setI(double i) {
		I = i;
	}    
	
	public double addToI(double a){
		I+=a;
		return I;
	}
    
}
