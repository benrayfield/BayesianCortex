/** Ben F Rayfield offers BayesianCortex under GNU GPL 2+ open source license(s) */
package bayesiancortex;
import java.util.*;

/** A group of Nodes which changes in size as less useful Nodes are removed
and new Nodes are added to experiment with new network shapes in realtime.
<br><br>
TODO Should Cortex ever remove a child Node if that child is not useful but
1 of its parent Nodes is useful? Does that make the child Node useful by association? 
*/
public class Cortex implements Runnable{
	
	private List<Node> nodes = new ArrayList<Node>();
	
	public void run(){
		for(Node n : nodes){
			n.run();
		}
	}

}