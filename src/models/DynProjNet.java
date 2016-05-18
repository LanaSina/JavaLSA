package models;

import java.util.ArrayList;
import java.util.HashMap;

import Jama.Matrix;
import communication.MyLog;
import startup.Constants;

public class DynProjNet {
	/** log */
	MyLog mlog = new MyLog("projection network", true);
	
	//build a network with increasingly small layers
	int n1 = 3;
	int n2 = 1;//this dimension should start at 1
	//resolution (neuron sensibility)
	int resIn = 2;
	int resOut = 10;
	//double input[] = {0,0,0};
	Matrix input = new Matrix(new double[][]{{0,0,0}});
	//weights
	Matrix w12;// = new Matrix();	
	/**this is the prediction space **/
	HashMap<String, TopNeuron> map = new HashMap<String, TopNeuron>();
	Matrix output = new Matrix(1,n2,0);//(new double[][]{{0,0}});//horizontal
	
	boolean justReset = true;
	int pattern =0;//pattern of output
	
	public DynProjNet(){
		//give it random weights (>0?) between 0 and 1 (why?)		
		double[][] we12 = new double[n1][n2];//n2 are lines
		for(int i=0; i<n1; i++){
			for(int j=0; j<n2; j++){
				//int t = 1;
				//if(Constants.uniformDouble()>0.5) t = -1;
				we12[i][j] = Constants.uniformDouble();
			}
		}	
		w12 = new Matrix(we12);	
	}
	
	public void update(){
		Matrix newInput = nextInput(input);
		Matrix newOutput = newInput.times(w12).times(1.0/n1); //output between 0 and 1

		//newOutput.print(3, 3);
		newInput.print(3, 3);
		boolean newDim = false;
		boolean doTest = false;
	if(justReset == false){
			//prediction map: each 2d point points to one other 2d point
			String previousKey = "";
			String currentKey = "";
			int[] previousVal = new int[n2];
			int[] currentVal = new int[n2];

			for(int i=0;i<n2;i++){	
				previousVal[i] = (int) (output.get(0,i)*resIn); 
				previousKey += previousVal[i]+",";
				currentVal[i] = (int) (newOutput.get(0,i)*resIn);
				currentKey += currentVal[i]+",";
			}
			
			// this output value is new: add it to map (should be done at previous step for logic...)
			if(!map.containsKey(previousKey)){
				//previous output will point to current output
				TopNeuron predictionNeuron = new TopNeuron(n2,currentVal,previousVal);//p
				mlog.say("new");
				map.put(previousKey, predictionNeuron);//this prediction points to the real prediction??
				if(map.containsKey(currentKey)){
					TopNeuron pointedNeuron = map.get(currentKey);
					///if(!t2.isPointedBy(predictionNeuron))
					pointedNeuron.addPointer(predictionNeuron);
				}		
			} else{//if this output value exists
				//get its corresponding neuron
				TopNeuron predictionNeuron = map.get(previousKey);
				int[] prediction = predictionNeuron.getPointed();
				mlog.say("prediction "+prediction[0]);
				
				//error
				double[] error = new double[n2];
				//error too
				double e = 0;
				boolean pointsSelf = true;
				for(int i=0;i<n2;i++){
					if(previousVal[i] != prediction[i]) pointsSelf = false;
					error[i] = currentVal[i] - prediction[i];
					e+= error[i]*error[i];
				}
				e = Math.sqrt(e);
				mlog.say("e "+e);
				
				//correct if error exists
				if(e>1.0 || pointsSelf){
					mlog.say("correcting");
					int dimMin = 0;
					
					//if this prediction exists
					if(map.containsKey(currentKey)){
						TopNeuron t2 = map.get(currentKey);
						//get pointer to this prediction
						TopNeuron t3 = t2.getTopPointer();
						if(t3!=null){//there is a pointer to this prediction
							int dimMax = 0;
							double er = 0;
							//double e2 = 0;
							//get coordinates of pointer
							int[] exVal = t3.getCoordinates();
							//look for biggest coordinate difference (maybe we should look for the smallest?)
							for(int i=0;i<n2;i++){
								error[i] = exVal[i] - prediction[i];
								//e2+= error[i]*error[i];
								if(Math.abs(error[i])>er){
									dimMax = i;
								}
							}
							//mimimize distance
							double temp = (e/Math.sqrt(resIn*resIn*2));
						//	mlog.say(" temp "+temp+ " e "+ e + " div "+ Math.sqrt(resIn*resIn*2));
							for(int i=0;i<n1;i++){
								//reduce this weight
								double w = w12.get(i,dimMax);
								//mlog.say("old w " + w);
								
								
								w = w - w*temp;
								//mlog.say("new w "+w);
								if(w<0.001) w = 0;
								w12.set(i,dimMax, w);
							}
							
						} else {
							//there is no pointer to this prediction
							//make current prediction be possibly dedoubled
							//biggest difference in input may be maximized
							double ec = 0;
							for(int i=0;i<n1;i++){
								double dif = input.get(0, i) - newInput.get(0, i);
								dif = Math.abs(dif);
								if(dif>=ec){
									dimMin = i;
								}
							}
							
							for(int i = 0; i<n2;i++){
								double w = w12.get(dimMin,i);
								mlog.say("dim " + dimMin + " i "+i);
							
								w = w*(1+e);
								//mlog.say("new w "+w);
								if(w == 0 ) w = 0.1;
								if(w>1) w = 1;
								w12.set(dimMin,i, w);
							}				
						
						}
					}
					
					
					//scale up these weights
					if(pointsSelf){ 
						if(resIn>=20){
							newDim = true;
						}
						else{
							resIn = resIn+1;
							mlog.say("*scale up, res "+resIn);
						}//if res>?, add one dimension?
						w12.plusEquals(new Matrix(n1, n2,0.1)); 
						w12.times(1.0/w12.norm1());
						for(int i = 0; i<n1;i++){
							for(int j = 0; j<n2;j++){
								double w = w12.get(i,j);
								if(w>1) w = 1;
								w12.set(i,j, w);
							}						
						}				
					}
					//so something has been corrected. now you can ask to check it
					mlog.say("asking for test on previous data");
					doTest = true;
					reset();
				} else{
					//this prediction does not exist
					//do nothing
				}
	
				//remove whoever we were pointing before
				String oldKey = "";
				for(int i=0;i<n2;i++){	
					oldKey += prediction[i]+",";
				}				
				if(map.containsKey(oldKey)){
					//check if above works please
					TopNeuron pointed = map.get(oldKey);
					pointed.removePointer(predictionNeuron);
				}
				predictionNeuron.setPointed(currentVal);//c
				if(map.containsKey(currentKey)){
					TopNeuron t4 = map.get(currentKey);
					if(!t4.isPointedBy(predictionNeuron))
						t4.addPointer(predictionNeuron);
				}
				
				w12.print(3, 3);
				
			}
		} else{
			//mlog.say("just reset, output ");
		}
	
		if(newDim){
			mlog.say("need another dim "+ n2);
			Matrix temp = new Matrix(n1,n2+1);
			for(int i = 0; i<n1;i++){
				for(int j = 0; j<n2;j++){
					temp.set(i, j, w12.get(i, j));
				}
				temp.set(i, n2, Constants.uniformDouble());
			}
			n2 = n2+1;
			w12 = temp.copy();
			output = new Matrix(1,n2,0);
			mlog.say("n2 "+n2+ " out "+ output.get(0,n2-1));
			output.print(3, 3);
			resIn = 2;
			map.clear();
			reset();
		}else{
			if(!doTest){
				justReset = false;
				output = newOutput.copy();
			}
		}
		if(doTest){
			justReset = true;
			//go back one step for input and output; this step will be skipped(because of reset)
			input = nextInput(input,-1);
		}else{
			input = newInput.copy(); 
		}
	}

	//hey how come we can't predict when going the opposite direction
	public void goBackTo(Matrix inp){

	}
	
	/**
	 * produce a series of inputs that are not random (here ouputs are going back and forth on a line)
	 * @param a {x,y,z and direction of motion (1 or -1)}
	 * @return next {x,y,z
	 */
	public Matrix nextInput(Matrix a){
		return(nextInput(a,1));
//		Matrix step0 = new Matrix(new double[][]{{0.1,0.12,0.08}});
//		Matrix step1 = new Matrix(new double[][]{{0.05,0.1,0.12}});
//		Matrix b ;
//		if(pattern==0){
//			b = a.plus(step0);
//		}else{
//			b = a.minus(step1);
//		}
//
//		//if a coordinate is negative
//		for(int i=0; i<3; i++){
//			if (b.get(0, 1)<=0){
//				b = new Matrix(new double[][]{{0,0,0}});
//				pattern = 0;
//				break;
//			}
//		}
//
//		//if the distance is >1
//		if(b.norm1()>=1){//restart from 0
//			double r = Constants.uniformDouble()*0.2;
//			b = step0.times(r);
//			reset();
//		}
		//start another line
//		if(b.norm1()>=1){
//			pattern = 1;
//			b = new Matrix(new double[][]{{1,1,1,0}});
//			b = a.minus(step1);
//		}
		//go back
//		if(b.norm1()>=1){
//			//pattern = 1;
//			//b = new Matrix(new double[][]{{1,1,1}});
//			//b = a.minus(step0);
//			b = new Matrix(new double[][]{{0,0,0}});
//		}
		
		//Matrix bmem = new Matrix(1,n2)
		//b.set(0, 3, input.get(0, 0));//very random memory
		
		//return b;
	}
	
	//for when we ask past data mostly
	public Matrix nextInput(Matrix a, int step){
		Matrix step0 = new Matrix(new double[][]{{0.1,0.12,0.08}});
		Matrix b ;
		b = a.plus(step0.times(step));
		
		//if a coordinate is negative
		for(int i=0; i<3; i++){
			if (b.get(0, 1)<=0){
				b = new Matrix(new double[][]{{0,0,0}});
				pattern = 0;
				break;
			}
		}

		//if the distance is >1
		if(b.norm1()>=1){//restart from 0
			double r = Constants.uniformDouble()*0.2;
			b = step0.times(r*step);
			reset();
		}
		
		return b;
	}
	
	public void reset(){
		justReset = true;
	}
	
	private class TopNeuron{
		public int[] coordinates; //of this neuron
		int[] pointed;//coordinates of anoter neurons
		//int ID;
		ArrayList<TopNeuron> pointedBy = new ArrayList<TopNeuron>();//pointing to this neuron
		
		/**
		 * 
		 * @param dim number of dimensions in the output layer
		 * @param p coordinates of the pointed neuron
		 * @param c coordinates of current neuron
		 */
		public TopNeuron(int dim,int[] p, int[] c){
			coordinates = c.clone();
			pointed = p.clone();
		}
		
		public boolean isPointedBy(TopNeuron t) {
			boolean r = true;
			int id = pointedBy.lastIndexOf(t);
			if(id<0) r = false;
			return r;
		}

		public void addPointer(TopNeuron t){
			pointedBy.add(t);
		}
		
		public TopNeuron getTopPointer(){
			if(pointedBy.isEmpty()) return null;
			else{
				TopNeuron top = pointedBy.get(0);
				double dist = 1000;
				for(int i=0; i<pointedBy.size();i++){
					double temp = 0;
					TopNeuron t = pointedBy.get(i);
					for(int j=0; j<n2;j++){
						double e = t.coordinates[j] - coordinates[j];
						temp+= e*e;
					}
					if(Math.sqrt(temp)<=dist) top = t;
				}
				return top;
			}
		}
		
		public void removePointer(TopNeuron t){
			int id = pointedBy.lastIndexOf(t);
			if(id>=0) pointedBy.remove(id);
		}
		
		public int[] getCoordinates(){
			return coordinates.clone();
		}
		
		public int[] getPointed(){
			return pointed.clone();
		}
		
		public void setPointed(int[] p){
			pointed = p.clone();
		}
	}

}
