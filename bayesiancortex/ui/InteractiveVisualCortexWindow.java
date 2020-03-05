/** Ben F Rayfield offers BayesianCortex under GNU GPL 2+ open source license(s) */
package bayesiancortex.ui;
import bayesiancortex.Node;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class InteractiveVisualCortexWindow extends JFrame implements MouseMotionListener{
	
	static Random rand;
	static{
		rand = new SecureRandom();
		rand.setSeed(System.nanoTime()+System.currentTimeMillis()+new Object().hashCode());
	}
	
	static List<Node> pixelNodes = new ArrayList<Node>();
	
	static List<Node> thinkingNodes = new ArrayList<Node>();
	
	final OneNodePerPixelDisplay display;
	
	public static void main(String args[]){
		
		Node x = new Node("xx");
		for(int i=0; i<2500; i++){
			Node m = new Node("pixel"+i);
			pixelNodes.add(m);
		}
		
		int jEnd = 5;
		for(int i=0; i<pixelNodes.size()-3; i++){
			for(int j=0; j<jEnd; j++){
				List<Node> childsList = new ArrayList<Node>();
				while(childsList.size() < 3){
					Node n;
					if(thinkingNodes.size() < 10 || rand.nextBoolean()){
						n = pixelNodes.get(rand.nextInt(pixelNodes.size()));
					}else{
						n = thinkingNodes.get(rand.nextInt(thinkingNodes.size()));
					}
					if(!childsList.contains(n)) childsList.add(n);
				}
				double w[] = Node.newWeights(childsList.size(), rand);
				//double w[] = new double[]{0, .25, 0, .25, 0, .25, .25, 0}; //NAND
				
				double att = rand.nextDouble();
				Node childs[] = childsList.toArray(new Node[0]);
				Node m = new Node("secondLayer_"+i+"_"+j, .5, childs, Node.newWeights(3, rand));
				for(Node c : childs) c.addToAxon(m);
				thinkingNodes.add(m);
			}
		}
		
		InteractiveVisualCortexWindow window = new InteractiveVisualCortexWindow(pixelNodes);
		while(true){
			window.nextState();
			window.repaint();
			try{
				Thread.sleep(30L);
			}catch(InterruptedException e){}
		}
	}
	
	double wave = 0;
	
	int lineStart = 0;
	
	long runs = 0;
	
	public void nextState(){
		synchronized(Node.class){
			runs++;
			
			//TODO test long-term memory:
			//if(runs == 10) for(Node n : pixelNodes){
			//	for(int i=0; i<7; i++) n.growMemoryBinaryList();
			//}
			
			wave += .1;
			lineStart++;
			int lineWidth = 50;
			int start = lineStart*lineWidth, end = lineStart*lineWidth+1200;
			double decay = .03;
			for(int i=start; i<end; i++){
				Node n = pixelNodes.get(i%pixelNodes.size());
				double target = .5 + .5*Math.sin(wave+Math.PI*2*i/end);
				//n.attention = n.attention*(1-decay) + decay*target;
				n.setChance(n.getChance()*(1-decay) + decay*target);
			}
			
			int mousePaintbrushWidth = 20;
			for(int xPixel=mouseX-mousePaintbrushWidth/2; xPixel<mouseX+mousePaintbrushWidth/2; xPixel++){
				for(int yPixel=mouseY-mousePaintbrushWidth/2; yPixel<mouseY+mousePaintbrushWidth/2; yPixel++){
					int virtualXPixel = xPixel/display.pixelMagnifyX;
					int virtualYPixel = yPixel/display.pixelMagnifyY;
					int i = virtualYPixel*display.virtualWidth + virtualXPixel;
					if(i < 0 || i >= pixelNodes.size()) continue;
					Node n = pixelNodes.get(i);
					double paintbrushDecay = .03;
					double targetBrightness = 1;
					//n.attention = n.attention*(1-paintbrushDecay) + paintbrushDecay*targetBrightness;
					//n.setAttention(n.getAttention()*(1-paintbrushDecay) + paintbrushDecay*targetBrightness);
					n.setChance(n.getChance()*(1-paintbrushDecay) + paintbrushDecay*targetBrightness);
				}
			}
			
			for(Node n : pixelNodes){
				n.run();
			}
			for(Node n : thinkingNodes){
				n.run();
			}
			//normalizeAttentions(pixelNodes);
			//normalizeAttentions(thinkingNodes);
			//System.out.println(list.get(10));
		}
	}
	
	public InteractiveVisualCortexWindow(List<Node> nodes){
		super("BayesianCortex");
		setLayout(new BorderLayout());
		add(new JLabel("red=chance std dev, green=chance, blue=accuracy"), BorderLayout.NORTH);
		this.pixelNodes = nodes;
		int magnifyX = 6, magnifyY = 6;
		display = new OneNodePerPixelDisplay(magnifyX, magnifyY, 50, nodes);
		int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
		int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
		add(display, BorderLayout.CENTER);
		display.addMouseMotionListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(new java.awt.Dimension(310, 340));
		setLocation(300, 100);
		setVisible(true);
	}
	
	/*public static void normalizeAttentions(List<Node> nodes){
		if(nodes.isEmpty()) return;
		double sum = 0;
		for(Node n : nodes) sum += n.attention;
		double ave = sum / nodes.size();
		double sumOfSquares = 0;
		for(Node n : nodes){
			double diff = n.attention - ave;
			sumOfSquares += diff*diff;
		}
		double stdDev = Math.sqrt(sumOfSquares/nodes.size());
		if(stdDev == 0) throw new RuntimeException("TODO different way to spread nodes");
		double targetStdDev = .2;
		double targetAve = .5;
		for(Node n : nodes){
			double att = n.attention;
			double nodeStdDev = (att-ave)/stdDev;
			att = targetAve + nodeStdDev*targetStdDev;
			att = Math.max(Node.DONT_BE_TOO_CERTAIN, Math.min(att, 1-Node.DONT_BE_TOO_CERTAIN));
			n.attention = att;
		}
	}*/
	
	int mouseX = 20, mouseY = 20;
	
	public void mouseMoved(MouseEvent e){
		mouseX = e.getX();
		mouseY = e.getY();
	}
	
	public void mouseDragged(MouseEvent e){ mouseMoved(e); }

}
