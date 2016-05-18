/**
 * 
 */
package demos;

import java.io.FileWriter;

import startup.Constants;

/**
 * @author lana
 *	Experiment with 2 or 3 neurons to demonstrate LSA principle.
 */
public class MinimalNetwork {

	/**data directory*/
	String folderName;

	/**stores weight of first synapse*/
	FileWriter wsyn1;//= new FileWriter(fileName);
	/**stores weight of second synapse*/
	FileWriter wsyn2;
	
	/*std::ofstream ofs;//first synapse
	std::ofstream ofs1;//second synapse
	std::ofstream ofs2;//spikes?
	std::ofstream stream_st;
	std::ofstream stream_dummy_rt;
	std::ofstream stream_stimTime;*/

	/**delay between 2 simulations (sim ms)*/
	int st_delay = 30;
	/**delay between 2 real time cycles (real ms)*/
	int it_delay = 300;	
	/**noise range*/
	double noise = 10;
	/** weight range*/
	double weights_range = 5;
	double decay_rate = Constants.DecayRate;
	/** stimulation value (mV)*/
	double stim_value = 2;
}
