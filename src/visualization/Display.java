package visualization;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Graphic panel
 * The field is an array.
 * @author lana
 *
 */
public class Display extends JFrame {

		private static final long serialVersionUID = 1579747902278268747L;
		//this is awful

		//surface to be drawn on
		Surface s; 
		//window title
		String name = "ALien Hunt";
		boolean selfRepaint =true;
		
		public Display() {
			//default
			int w = 1500;
			int h = 1000;
			
	        initUI(w,h);
	        this.setVisible(true);
	    }
		
		/**
		 * set custom window size
		 * @param w width
		 * @param h height
		 * @param b visible or not
		 */
		public Display(int w, int h, boolean b) {
			selfRepaint = b;
	        initUI(w,h);
	        this.setVisible(true);
	    }
		
		public void setName(String name){
			this.name = name;
			setTitle(name);
		}

//		public void setSelfRepaint(boolean b){
//			selfRepaint = b;
//		}
		
		/**
		 * 
		 * @param w width
		 * @param h height
		 */
	    private void initUI(int w, int h) {
	        setTitle(name);
	        s = new Surface(w,h);
	        add(s);
	        setSize(w,h);
	        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        setLocationRelativeTo(null);
	        
	        //refresh
	        if(selfRepaint){
		        int delay = 50; //milliseconds
	
		        ActionListener taskPerformer = new ActionListener() {
		          public void actionPerformed(ActionEvent evt) {
		            s.repaint();
		          }
		        };
	
		        new Timer(delay, taskPerformer).start();
	        }else{
	        	  int delay = 50; //milliseconds
	        		
			        ActionListener taskPerformer = new ActionListener() {
			          public void actionPerformed(ActionEvent evt) {
			            s.repaint();
			          }
			        };
		
			        new Timer(delay, taskPerformer).start();
	        }
	    }
	    
	    //please don't look this should be cleaner I JUST WANT A RASTER
//	    public void addRectangle(Rectangle2D r){
//	    	lstShapes.add(r);
//	    }
//	    
//	    public void clearShapes(){
//	    	lstShapes.clear();
//	    }
	    /**
	     * Add a object to be drawn on the pannel.
	     * @param c the object implementing the component interface
	     */
	    public void addComponent(GraphicalComponent c){
	    	s.addComponent(c);
	    }
	    
	    //hem ?
	    public JPanel getSurface(){
	    	return s;
	    }
	    
	    /**
	     * Add an object to be controlled by keyboard actions.
	     * @param p the object to be controlled
	     * @param string a unique name
	     */
}
	

	/**
	 * The surface on which we draw the graphics.
	 * @author lana
	 *
	 */
	class Surface extends JPanel{
		//default size
		int w;// = 1000;
		int h;// = 1000;
        //grid step size
        int step = 20;//TODO not needed

		//list of things to draw
		private List<GraphicalComponent> components = new ArrayList<GraphicalComponent>();
		
		/** build surface with custom size*/
		public Surface(int w, int h){
			this.w = w;
			this. h = h;
		}
		
		public Surface(){
			w = 1000;
			h = 1000;
		}
		
	    /**
	     * Adds a object to be drawn on the pannel.
	     * @param c the object implementing the component interface
	     */
	    public void addComponent(GraphicalComponent c){
	    	components.add(c);
	    }
	    
		private static final long serialVersionUID = 6523850037367826272L;

		/**
		 * Sets the background grid.
		 * @param g
		 */
		private void init(Graphics g) {
			this.setOpaque(true);
			this.setBackground(Color.white);
		}

	    @Override
	    public void paintComponent(Graphics g) {

	        super.paintComponent(g);
	        init(g);
	        
	        for(int i=0;i<components.size();i++){
	        	components.get(i).draw(g,step);
	        }
	    }
	    
	    public int getWidth(){
	    	return w;
	    }
	    
	    public int getLength(){
	    	return h;
	    }
	    
	    public int getStep(){
	    	return step;
	    }
		   
	}
	
	
