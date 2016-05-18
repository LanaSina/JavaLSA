package visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.JFrame;

import models.IzhNeuron;

import org.apache.commons.collections15.Transformer;

import startup.Constants;
import communication.MyLog;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;



/**
 * This class manages visualization of the spiking NN.
 * @author lana
 *
 */
public class NetworkGraph {
	MyLog mlog = new MyLog("graphViz", true);
	String name = "Radical Embodied Network";
	
	/**Graph<V, E> where V is the type of the vertices and E is the type of the edges*/
	//DirectedGraph<NeuronVertex, SynapseEdge> g;
	DirectedOrderedSparseMultigraph <NeuronVertex, SynapseEdge> g;
    /** visualizer*/
    BasicVisualizationServer<NeuronVertex,String> vv;
    /** number of neurons*/
    int n;
    /** only synapses above this weight will be displayed*/
    double minWeight = 4;
   static NeuronVertex[] vertices;
    

   /**
    * Creates a new instance of NeuronGraph 
    * @param size number of neurons
    * @param weights weights of the network [from][to]
    */
    public NetworkGraph(int size, double[][] weights) {
    	n = size;
    	vertices = new NeuronVertex[n];
    	g = new DirectedOrderedSparseMultigraph<NeuronVertex, SynapseEdge>();
        
        // Add neurons and edges
        for(int i=0; i<n;i++){
        	NeuronVertex nv = new NeuronVertex(i);
        	vertices[i] = nv;
        	g.addVertex(nv);
        }
        
        for(int i=0; i<n;i++){
        	for(int j=0; j<n; j++){
        		if((i!=j) && (weights[i][j]>=minWeight)){
        			String label = "w_"+i+","+j;
        			SynapseEdge se = new SynapseEdge(label, weights[i][j]);
        			g.addEdge(se, vertices[i],vertices[j]);
        		}        			
        	}        	
        }       
    }
    
    public void setName(String n){
    	this.name = n;
    }
    
    public void setClusters(int inputStart, int inputSize, int outputStart, int outputSize,  int outputBStart, int outputBSize){
    	for(int i=0; i<20;i++){
    		vertices[i].setType(Constants.InhibCluster);
    	}
    	
    	for(int i=inputStart; i<(inputStart+inputSize);i++){
    		vertices[i].setType(Constants.InputCluster);
    	}
    	
    	for(int i=outputStart; i<(outputStart+outputSize);i++){
    		vertices[i].setType(Constants.OutputCluster);
    	}
    	
    	for(int i=outputBStart; i<(outputBStart+outputBSize);i++){
    		vertices[i].setType(Constants.OutputClusterB);
    	}
    }
    
    /**
     * will show if neurons are spiking or not
     * @param neurons array of IzhNeurons
     */
    public void updateNeurons(IzhNeuron[] neurons){
    	for(int i=0; i<n;i++){
    		vertices[i].setSpiking(false);
    		if(neurons[i].isFiring())
    			vertices[i].setSpiking(true);
    	}
    	
    	//repaint out of main thread
    	Runnable code = new Runnable() {
        	public void run() {
        		vv.repaint();
        	}
        };

        (new Thread(code)).start();
    }
    
    /**
     * 
     * @param weights double array of synaptic weights
     */
    public void update(double[][] weights){
        
        for(int i=0; i<n;i++){
        	for(int j=0; j<n; j++){ 
        		double w = weights[i][j];
        		SynapseEdge e = g.findEdge(vertices[i],vertices[j]);
        		if(e!=null){    				        		
	        		if(Math.abs(e.weight-w)>0.5){
	        			g.removeEdge(e);
	        			if(w>=minWeight){
	            			String label = "w_"+i+","+j;
	            			SynapseEdge se = new SynapseEdge(label, w);
	            			g.addEdge(se, vertices[i],vertices[j]);
	            		}   
	        		}    
	        	} else{
	        		if(w>=minWeight){
            			String label = "w_"+i+","+j;
            			SynapseEdge se = new SynapseEdge(label, w);
            			g.addEdge(se, vertices[i],vertices[j]);
            		}   
	        	}
        	}        	
        }
    	
        Runnable code = new Runnable() {
        	public void run() {
        		vv.repaint();  
        	}
        };

        (new Thread(code)).start();	
    }
    
    
    
    public void show() {

        // The Layout<V, E> is parameterized by the vertex and edge types
        Layout<NeuronVertex, String> layout = new CircleLayout(g);
        layout.setSize(new Dimension(800,800)); // sets the initial size of the layout space
        // The BasicVisualizationServer<V,E> is parameterized by the vertex and edge types
        vv = new BasicVisualizationServer<NeuronVertex,String>(layout);
        vv.setPreferredSize(new Dimension(800,800)); //Sets the viewing area size
        vv.getRenderer().setVertexRenderer(new MyRenderer());
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<NeuronVertex, String>() {
            public String transform(NeuronVertex nv) {
                return (nv.toString());
            }
        });
        
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv); 
        frame.pack();
        frame.setVisible(true);       
    }
    
    /**
     * Custom node class to display neurons
     * @author lana
     *
     */
    class NeuronVertex {
    	 int id; 
    	 private boolean isSpiking = false;
    	 private int type = Constants.HiddenCluster;
    	 
    	 public NeuronVertex(int id) {
    		 this.id = id;
    	 }
    	 
    	 public NeuronVertex(int id, int type) {
    		 this.id = id;
    		 this.type = type;
    	 }
    	 
    	 public void setSpiking(boolean b){
    		 isSpiking = b;
    	 }
    	 
    	 public void setType(int t){
    		 type = t;
    	 }
    	 
    	 public String toString() { 
    		 return "N"+id; 
    	 }
    }
    
    /**
     * Custom edge class
     * @author lana 
     */

    class SynapseEdge{
    	double weight = 0;
    	String label;
    	
    	public SynapseEdge(String label, double weight){
    		this.label = label;
    		this.weight = weight;
    	}
    	
    	public String toString() { 
   		 return label; 
   	 	}
    }
    /**
     * Custom vertex visualization.
     * @author lana
     *
     */
    static class MyRenderer implements Vertex<NeuronVertex, String> {
        /*public void paintVertex(RenderContext<String, String> rc, Layout<String, String> layout, String vertex) {
          GraphicsDecorator graphicsContext = rc.getGraphicsContext();
          Point2D center = layout.transform(vertex);
          Shape shape = null;
          Color color = null;
          //if(vertex.equals("Square")) {
            shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
            color = Color.BLUE;
          //}
          graphicsContext.setPaint(color);
          graphicsContext.fill(shape);
        }*/

		public void paintVertex(RenderContext<NeuronVertex, String> rc, Layout<NeuronVertex, String> layout, NeuronVertex vertex) {
			GraphicsDecorator graphicsContext = rc.getGraphicsContext();
	          Point2D center = layout.transform(vertex);
	          Shape shape = null;
	          Color color = null;
	          
	          shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
	          NeuronVertex nv = vertices[vertex.id];
	          if(nv.isSpiking) {	    
	        	  color = Color.RED;	         
	          } else{
	        	  switch(nv.type){
		        	  case Constants.InhibCluster:{
		        		  color = Color.GREEN;
		        		  break;
		        	  }
		        	  case Constants.OutputCluster:{
		        		  color = Color.BLUE;
		        		  break;
		        	  }
		        	  case Constants.OutputClusterB:{
		        		  color = Color.GRAY;
		        		  break;
		        	  }
		        	  case Constants.InputCluster:{
		        		  color = Color.ORANGE;
		        		  break;
		        	  }
		        	  default:{
		        		  color = Color.BLACK;  
		        		  break;
		        	  }
	        	  }
	        	  
	          }
	          graphicsContext.setPaint(color);
	          graphicsContext.fill(shape);
		}
      }
}
