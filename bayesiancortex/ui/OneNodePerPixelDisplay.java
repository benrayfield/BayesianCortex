/** Ben F Rayfield offers BayesianCortex under GNU GPL 2+ open source license(s) */
package bayesiancortex.ui;
import bayesiancortex.Node;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.MemoryImageSource;
import java.security.SecureRandom;
import java.util.*;

public class OneNodePerPixelDisplay extends JPanel /*implements MouseMotionListener*/{
	
	//final int color[];
	
	final int intArrayPixels[];
	
	final MemoryImageSource memoryImageSource;
	
	Image imageWrapsIntArrayPixels;
	
	List<Node> nodes;
	
	Random rand = new Random();
	
	public final int virtualWidth, virtualHeight, pixelMagnifyX, pixelMagnifyY;
	
	/** width is in virtual pixels, before pixelMagnify* are applied.
	If pixelMagnifyX is 3 and pixelMagnifyY is 2, each virtual pixel will be 3x2 pixels.
	*/
	public OneNodePerPixelDisplay(int pixelMagnifyX, int pixelMagnifyY, int virtualWidth, List<Node> nodes){
		this.pixelMagnifyX = pixelMagnifyX;
		this.pixelMagnifyY = pixelMagnifyY;
		if(nodes.size() % virtualWidth != 0) throw new RuntimeException(
			"nodes.size()="+nodes.size()+" which is not divisible by width "+virtualWidth);
		this.nodes = nodes;
		this.virtualWidth = virtualWidth;
		virtualHeight = (int)Math.ceil(nodes.size()/virtualWidth);
		if(virtualHeight < 1) throw new RuntimeException("nodes.size() is too small "+nodes.size());
		intArrayPixels = new int[virtualWidth*pixelMagnifyX * virtualHeight*pixelMagnifyY];
		memoryImageSource = new MemoryImageSource(virtualWidth*pixelMagnifyX, virtualHeight*pixelMagnifyY,
			intArrayPixels, 0, virtualWidth*pixelMagnifyX);
		imageWrapsIntArrayPixels = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
	}
	
	//long countPaints;
	
	double totalBrightLastTime;

	public void paint(Graphics g){
		//int color = (countPaints&1)==1 ? 0xffffff : 0;
		//countPaints++;
		//System.out.println("countpaints="+countPaints);
		synchronized(Node.class){
			for(int i=0; i<intArrayPixels.length; i++){
				//TODO optimize by not repeating these calculations for
				//the many subpixels of each virtual pixel.
				int xPixel = i%(virtualWidth*pixelMagnifyX);
				int yPixel = i/(virtualWidth*pixelMagnifyX);
				int virtualXPixel = xPixel / pixelMagnifyX;
				int virtualYPixel = yPixel / pixelMagnifyY;
				Node n = nodes.get(virtualYPixel*virtualWidth + virtualXPixel);
				float red = Math.min((float)n.getChanceStdDev()*10, 1f);
				float green = (float)n.getChance();
				float blue = (float)n.getAccuracyMeasuredLastRun();
				intArrayPixels[i] = new Color(red, green, blue).getRGB();
			}
		}
		memoryImageSource.newPixels();
		imageWrapsIntArrayPixels = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
		//g.drawImage(imageWrapsIntArrayPixels, 0, 0, this);
		g.drawImage(imageWrapsIntArrayPixels, 0, 0, null);
	}

	/*public void mouseDragged(MouseEvent e){
		mouseMoved(e);
	}
	
	int mouseX = 5, mouseY = 5;

	public void mouseMoved(MouseEvent e) {
		int mouseXOld = mouseX, mouseYOld = mouseY;
		mouseX = e.getX();
		mouseY = e.getY();
		int minX = Math.min(mouseXOld, mouseX), maxX = Math.max(mouseXOld, mouseX);
		for(int x=minX; x<=maxX; x++){
			float xFractionExclusiveAtEnd = x/(maxX+1f-minX);
			int y = (int)(mouseYOld + xFractionExclusiveAtEnd*(mouseY-mouseYOld));
			Node n = nodes.get(x*virtualWidth + y);
			//n.attention = 0;
			n.setChance(0);
		}
	}*/

}