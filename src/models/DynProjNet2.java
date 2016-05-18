package models;

import Jama.Matrix;
import communication.MyLog;
import startup.Constants;

public class DynProjNet2 {
	/** log */
	MyLog mlog = new MyLog("projection network", true);
	
	//build a network with increasingly small layers
	int n1 = 3;
	int n2 = 2;
	//resolution (neuron sensibility)
	int res = 10;
	//input layer is 3d with resolution as a 4thd (value inside array = weights to summurizing neurons)
	double[][][][] layer1 = new double[res][res][res][2];
	//output layer is 2d with resolution as a +d (value inside array = predicted next output)
	int[][][] layer2 = new int[res][res][2];
	//neurons in the 3d layer talk to 2 summarizing neurons. .
	//update consists in making common weights closer or farther from each other.
	//upper layer: predictions are always updated to most recent one.
	
	
	double input[] = {0,0,0};
	//Matrix input = new Matrix(new double[][]{{0,0,0}});
	//weights
	int d1 = n1*res; int d2 = n2*res;
	double[][] dw = new double[d1][d2];
	//Matrix w12 = new Matrix();	
	//this is the prediction space (a plane here): each 2d point points to one other 2d point
	//0: coordinate on pj neuron 1; 1: coordinate on pj neuron 2;  value: to which neuron (res value) it points
	//double[][] prediction = new double[res][res];
	
	//Matrix output = new Matrix(new double[][]{{0,0}});//horizontal
	double[] output = {0,0};//horizontal
	
	public DynProjNet2(){
		//give it random weights (>0?) between 0 and 1 (why?)		
		//double[][] we12 = new double[n1][n2];//n2 are lines
		for(int i=0; i<d1; i++){
			for(int j=0; j<d2; j++){
				//int t = 1;
				//if(Constants.uniformDouble()>0.5) t = -1;
				dw[i][j] = Constants.uniformDouble();
			}
		}	
		
		for(int i=0; i<res; i++){
			for(int j=0; j<res; j++){
				for(int k=0; k<res; k++){
					layer1[i][j][k][0] = Constants.uniformDouble();
					layer1[i][j][k][1] = Constants.uniformDouble();
				}
			}
		}	
		
		//w12 = new Matrix(we12);
		//w12.print(3, 3);
		
		//first no prediction at all
		for(int i=0; i<res; i++){
			for(int j=0; j<res; j++){
				//prediction[i][j][0] = i;
				//prediction[i][j][1] = j;
				layer2[i][j][0] = i;
				layer2[i][j][1] = j;
			}
		}	
	}
	
	public void update(){
		//input = nextInput(input);
		double[] newInput = nextInputArray(input);
		mlog.say("new input "+ newInput[0]+" " + newInput[1]+" "+newInput[2]);
		double[] newOutput = calculateOutput(newInput);
		mlog.say("new output "+ newOutput[0]+" " + newOutput[1]);
		//Matrix newOutput = input.times(w12); //output between 0 and 1

		//prediction map: each 2d point points to one other 2d point
		int x = (int) (output[0]*res);
		int y =(int) (output[1]*res);
		int px = layer2[x][y][0];
		int py = layer2[x][y][1];
		
		int nx =(int) (newOutput[0]*res);
		int ny = (int) (newOutput[1]*res);
		//int cpx = layer2[nx][ny][0];
		//int cpy = layer2[nx][ny][1];
//		mlog.say("weights ");
//		w12.print(3, 3);
//		mlog.say("input ");
//		input.print(3, 3);
		mlog.say("prediction "+ px+" "+py);
		mlog.say("correct prediction "+ nx +" "+ ny);
		double ex = nx-px; double ey = ny-py;
		mlog.say("error "+ex+ " " + ey);
		
		//if no prediction (prediction is self), initialize value
		if(px == x || py == y){
			layer2[x][y][0] = nx; 
			layer2[x][y][1] = ny; 
		}else{//if prediction
			//if prediction false, update weights
			if(px != nx || py != ny){
				//the update must make current projected point different from past one
				//?find which position predicted current one (there could be several and we have to chose closest one)
				double d_pred = 1000;//distance from correct prediction
				double d_current = 1000;//distance from current prediction
				int ii=-1,jj=-1;
				for(int i=0; i<res; i++){
					for(int j=0; j<res; j++){
						//calculate distance from correct prediction
						double temp = Math.pow(layer2[i][j][0]-nx, 2) + Math.pow(layer2[i][j][1]-ny, 2);
						//distance from current prediction
						double temp2 = Math.pow(layer2[i][j][0]-px, 2) + Math.pow(layer2[i][j][1]-py, 2);
						if(temp+temp2<d_pred+d_current){ //best point
							ii = i;
							jj = j;
							d_pred = temp;
							d_current = temp2;
						}
					}
				}				
				//if no prediction to correct pred, change weights so current point and correct point become closer
				//find common weights and make them closer(but there are only common weights...)
				if(ii<0 || jj<0){
					//weight to current point = weight from current input
					int nn1 = (int) (newInput[0]+0.5);
					int nn2 = (int) (newInput[1]+0.5);
					int nn3 = (int) (newInput[2]+0.5);				
					//get weights from this neuron
					double w1 = layer1[nn1][nn2][nn3][0];
					double w2 = layer1[nn1][nn2][nn3][1];
					
					//weights from l1 to predicted output = ?
					//so just make w1 and w2 closer to previous input
					int nn11 = (int) (input[0]+0.5);
					int nn21 = (int) (input[1]+0.5);
					int nn31 = (int) (input[2]+0.5);				
					//get weights from this neuron
					double ow1 = layer1[nn11][nn21][nn31][0];
					double ow2 = layer1[nn11][nn21][nn31][1];
					
					w1 += (ow1-w1)/2;
					w2 += (ow2-w2)/2;
					layer1[nn1][nn2][nn3][0] = w1;
					layer1[nn1][nn2][nn3][1] = w2;
						
				}else{
					//try to match existing prediction
					//weight to current point = weight from current input
					int nn1 = (int) (input[0]+0.5);
					int nn2 = (int) (input[1]+0.5);
					int nn3 = (int) (input[2]+0.5);					
					//get weights from this neuron
					double w1 = layer1[nn1][nn2][nn3][0];
					double w2 = layer1[nn1][nn2][nn3][1];
					
					w1 += ((ii*1.0/res)-w1)/2;
					w2 += ((jj*1.0/res)-w2)/2;
					layer1[nn1][nn2][nn3][0] = w1;
					layer1[nn1][nn2][nn3][1] = w2;
				}
				
				//update prediction anyway
				layer2[x][y][0] = nx; 
				layer2[x][y][1] = ny; 
			}
		}

		output = newOutput.clone();
		input = newInput.clone();
	}
	
	public double[] calculateOutput(double[] input){
		//just multiply and add all weights
		int nn1 = (int) (input[0]*res);
		int nn2 = (int) (input[1]*res);
		int nn3 = (int) (input[2]*res);
		
		//get weights from this neuron
		double w1 = layer1[nn1][nn2][nn3][0];
		double w2 = layer1[nn1][nn2][nn3][1];

		return new double[] {w1,w2};
	}

	
	/**
	 * produce a series of inputs that are not random (here ouputs are going back and forth on a line)
	 * @param a {x,y,z and direction of motion (1 or -1)}
	 * @return next {x,y,z
	 */
//	public Matrix nextInput(Matrix a){
//		//x = at +xa; t = 1, a = 0.1
//		//y = bt +ya; b = 0.05
//		//z = ct +za; z = 0.12
//		//limits: 0 and sqrt(x^2+y^2+z^2) = 1
//		Matrix step = new Matrix(new double[][]{{0.1,0.05,0.12}});
////		double x = a.get(0,0) + d*0.1;
////		double y = a.get(0,1) + d*0.05;
////		double z = a.get(0,2) + d*0.12;
//		Matrix b = a.plus(step.times(d));
//
//		//if a coordinate is negative
//		for(int i=0; i<3; i++){
//			if (b.get(0, 1)<=0){
//				d = 1;
//				b = new Matrix(new double[][]{{0,0,0}});
//				break;
//			}
//		}
//
//		//if the distance is >1
//		if(b.norm1()>=1){//what is norm1
//			d = -1;
//			b = new Matrix(new double[][]{{1,1,1}});
//		}
//		
//		return b;
//	}
	
	public double[] nextInputArray(double[] a){
		//x = at +xa; t = 1, a = 0.1
		//y = bt +ya; b = 0.05
		//z = ct +za; z = 0.12
		//limits: 0 and sqrt(x^2+y^2+z^2) = 1
		//Matrix step = new Matrix(new double[][]{{0.1,0.05,0.12}});
//		Matrix ma = new Matrix(new double[][]{{a[0],a[1],a[2]}});
		double x = a[0] + 0.1;
		double y = a[1] + 0.05;
		double z = a[2] + 0.12;
		
		//Matrix b = a.plus(step);

		//if a coordinate is negative
//		for(int i=0; i<3; i++){
//			if (b.get(0, 1)<=0){
//				d = 1;
//				b = new Matrix(new double[][]{{0,0,0}});
//				break;
//			}
//		}

		double[] b = {x,y,z};
		//if the distance is >1 start again from 0
		double d = Math.sqrt(x*x + y*y+z*z);//dont need to square...
		if(d>=1){//what is norm1
			double r = Constants.uniformDouble()*0.2;
			b[0] = 0.1*r;
			b[1] = 0.05*r;//0.2*Constants.uniformDouble();
			b[2] = 0.12*r;//0.2*Constants.uniformDouble();
		}
		
		return b;
	}

}
