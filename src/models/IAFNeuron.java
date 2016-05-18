package models;

import startup.Constants;

/**
 * (Not leaky?) Integrate and Fire Neuron according to Science paper
 * not Nature paper "Diverse plasticity mechanisms orchestrated to..."
 * current based or conductance based? Current-based (science)
 * @author lana
 *
 */
public class IAFNeuron {
	//default excitatory neuron
	int type = Constants.NeurExcitatory;
	
	//reset potential (mV)
	double v_reset = 16;
	//refractory period (ms)
	int tau_arp = 2;
	int t_ref = 0;
	//membrane voltage v
	double V;
	double I;
	//spike emission threshold (mV)
	double th = 20;
	double tauM = 15;//ms
	
	boolean isSpiking = false;
	
	public void setNeuronType(int type){
		this.type = type;
        switch(type){
            case Constants.NeurInhibitory:{
            	v_reset = 13;
            	tauM = 10;
                break;
            }              
        }		
	}

	//dt = 1 ms
	public boolean update(){
		isSpiking = false;
		
		//refractory period
		if(t_ref>0){
			t_ref--;
		}
		
		if(t_ref<=0){
			//update membrane potential
			double dv = -V+I;
			dv = dv/tauM;
			V = V + dv;
		}
			
		if(V>=th){
			isSpiking = true;
			V = v_reset;
			t_ref = tau_arp;
		}
		
		I = 0;
		
		return isSpiking;
	}
	
	
	public void addToI(double i){
		this.I = this.I + i;
	}
	
	
	public boolean isFiring(){
		return isSpiking;
	}
	
	
	public double getV(){
		return V;
	}
}
