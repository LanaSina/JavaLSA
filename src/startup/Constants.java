/**
 * 
 */
package startup;

import java.util.Random;

/**
 * @author lana
 * Class containing all the constants variable and generic functions.
 */
public class Constants {
	
	//neuron types
	/** excitatory Regular Spiking neuron*/
	public static final int NeurExcitatory = 2;
	/** inhibitory Fast Spiking neuron */
	public static final int NeurInhibitory = 3;
	
	//connection types
	/** Excitatory to Excitatory */
	public static final int C_EE = 1;
	/** Excitatory to Excitatory */
	public static final int C_EI = 2;
	
	//types of neuronal clusters
	public static final int HiddenCluster = 0;
	public static final int InputCluster = 1;
	public static final int OutputCluster = 2;
	public static final int InhibCluster = 3;
	public static final int OutputClusterB = 4;
	/** rate of decay function*/
	public static double DecayRate = 0.9999995;
	/** STDP delay window (ms)*/
	public static int tau = 20;
	/** folder where data file will be recorded*/
	public static String DataPath = "/Users/lana/Development/JAVANN";//
	/** path to sound files*/
	public static String SoundPath = "/sound/piano";
	//filename
	/**data file names*/
	public static String SpikesFileName = "spiking_data.csv";
	public static String ReacTimeFileName = "stimulation_reaction_time.csv";
	public static String AlienHitFileName = "alien_hit_time.csv";
	public static String AlienMissFileName = "alien_miss_time.csv";
	public static String FinalValuesFileName = "final_values.csv";
	public static String DummyClusterFileName = "dummy_firing.csv";
	public static String RealClusterFileName = "real_firing.csv";
	public static String InputValueFileName = "input_value.csv";

	/**
	 * from http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
	 * and http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range
	 * Returns a pseudo-random number between min and max, inclusive.
	 * Uniform distribution.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimum value
	 * @param max Maximum value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static double uniformDouble(double min, double max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    //rand.nextInt(max - min) + 1)

	    return randomNum;
	}
	
	/**
	 * Uniform distribution between 0 and 1
	 * @return random number
	 */
	public static double uniformDouble() {
		double min = 0;
		double max = 1;
	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    //rand.nextInt(max - min) + 1)

	    return randomNum;
	}
}

