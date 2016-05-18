package models;

import communication.MyLog;
import startup.Constants;

/**
 * Adaptive Leaky Integrate and Fire Neuron according to Nature paper "Diverse plasticity mechanisms orchestrated to..."
 * conductance based
 * @author lana
 *
 */
public class AIAFNeuron {
	MyLog mlog = new MyLog("AIAF", false);
	//default excitatory neuron
	int type = Constants.NeurExcitatory;
	
	double tauM = 20;//ms
	double I_exc = 0;
	double I_inh = 0;
	
	//AIAF
	double u_rest = -60;//resting potential in mV
	double u_exc = 0; // exc reversal potential (mV)
	double u_inh = -80;//inh reversal potential mV
	double a = 0.2;//alpha default Exc
	double tau_ampa = 5;//decay time cst ms
	double tau_gaba = 10;//decay time cst ms
	double tau_nmda = 100;//decay time cst ms
	double tau_a = 100;// adaptation time cst ms
	double delta_a = 0.1;//adaptation strength
	double tau_b = 20; // slow adaptation time constant (s)
	//not used double delta_b
	double tau_thr = 2;//thershold time cst ms
	double v_rest = -50;// thereshold resting value mV
	double g_b = 0; //useless most of the time
	
	//time-dependant, initial values unknown
	double g_ampa = 0;//?
	double g_a = 0;//probably
	double g_nmda = 0;

	
	//init value known
	double v = 50;//mV
	double u = u_rest;
		double g_gaba = 0;
	
	boolean isSpiking = false;
	
	public void setNeuronType(int type){
		this.type = type;
        switch(type){
            case Constants.NeurInhibitory:{
            	a = 0.3;
                break;
            }              
        }		
	}

	//dt = 1 ms
	public boolean update(){
		
		//update excitation
		double dgampa = -g_ampa/tau_ampa + I_exc;
		double dgnmda = -g_nmda + g_ampa;
		dgnmda = dgnmda/tau_nmda;
		//
		double g_exc = a*g_ampa + (1-a)*g_nmda;				
		//update inhibition
		double dggaba = -g_gaba/tau_gaba + I_inh;		
		//update adaptation if is E
		double dga = 0;
		if(type == Constants.NeurExcitatory){
			int s = 0;
			if(isSpiking) s = 1;
			dga = -g_a/tau_a + delta_a*s;//S_j(t)
		}		
			
		//update threshold
		double dv = -v+v_rest;
		dv = dv/tau_thr;		
		//update membrane conductance
		double du = (u_rest - u) + g_exc*(u_exc-u) + (u_inh - u)*(g_gaba+g_a+g_b);
		du = du/tauM;
		
		//update everything
		g_nmda = g_nmda + dgnmda;
		g_ampa = g_ampa + dgampa;
		g_gaba = g_gaba+dggaba;
		g_a = g_a + dga;
		v = v + dv;	
		u = u + du;
		
		isSpiking = false;	
		if(u>v){
			mlog.say("spike u "+u+" v "+ v);
			isSpiking = true;
			v = 50;//mV
			u = u_rest;			
		}
		
		I_exc = 0;
		I_inh = 0;
		
		return isSpiking;
	}
	
	
	public void addToIexc(double i){
		this.I_exc = this.I_exc + i;
	}
	
	public void addToIinh(double i){
		this.I_inh = this.I_inh + i;
	}
	
	
	public boolean isFiring(){
		return isSpiking;
	}
	
	
	public double getU(){
		return u;
	}
}
