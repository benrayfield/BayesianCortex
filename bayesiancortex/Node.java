/** Ben F Rayfield offers BayesianCortex under GNU GPL 2+ open source license(s) */
package bayesiancortex;

import java.util.Arrays;
import java.util.Random;

/** Running a Node changes the state of that Node and nothing else.
Nodes connected to it will only change when they are run,
or when any Node is connected to any other Node they have reverse pointers.
Because of that property, all interactions are local,
therefore it forms scale-free networks. We could run a global brain.
<br><br>
This class is not thread-safe, so synchronize per Node when
adding Nodes to a Node at the same time as running any of those Nodes.
<br><br>
TODO After this software is intelligent and fun and working well,
rewrite it in a block of C code inside a C++ program accessed through a Java API
for high level access to reading and writing it as a high dimensional space.
<br><br>
TODO Testing speed when make most things private. Change it back if its slower
to have to call a few extra functions, including accessing things which
are not yet accessed externally through an API which will be created later.
<br><br>
TODO Should Node, instead of Cortex, create new Nodes depending on the Nodes
this Node can see recursively and how they relate to eachother?
I was planning to do it randomly in Cortex and see if each Node works out. 
*/
public class Node implements Runnable{

	private double chance = .5;
	public double getChance(){ return chance; }
	public void setChance(double c){
		if(c < 0 || c > 1) throw new IllegalArgumentException(
			"set chance="+c+" not in range 0 to 1");
		chance = c;
	}
	
	private double attention = .5;
	public double getAttention(){ return attention; }
	public void setAttention(double a){
		if(a <= 0) throw new IllegalArgumentException(
			"set attention="+a+" must be positive");
		attention = a;
	}
	
	/** Standard-deviation of the values averaged into the "chance" var.
	TODO? This is 0 if axonSize is less than 2.
	*/
	private double chanceStdDev = .01;
	//private static final double chanceStdDev_min = .001;
	public double getChanceStdDev(){ return chanceStdDev; }
	
	/** 3 Child bayesian nodes which the 2^3=8 weights
	are about combinations of being true or false.
	*/
	private Node zZ, yY, xX;
	
	/** 8 bayesian weights for the powerset of 3 child nodes.
	They sum to 1 except for roundoff-error
	and must often be normalized to stay that way.
	*/
	private double zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX;
	
	/** This linked list is all log_base_2 levels
	between short-term and long-term memory.
	<br><br>
	Each halfSpeed Node runs every other time its parent runs,
	so its like a time-based (instead of positional) binary number
	where each digit is a bayesian node.
	*/
	private Node halfSpeed;
	
	/** Returns 1 if this Node is the last in the halfSpeed linked list */
	private int halfSpeedDepth(){
		Node n = halfSpeed;
		int i = 1;
		while(n != null){
			n = n.halfSpeed;
			i++;
		}
		return i;
	}

	/** Used with the halfSpeed Node, which itself may have a halfSpeed Node,
	and ends with null at the most-significant-digit end.
	<br><br>
	My intuition about this is its similar to Fast Fourier Transform (FFT)
	except done in a more digital way, using booleans instead of complex numbers
	which rotate around a complex unit circle.
	Each level in the halfSpeed linked list is like a digit in a binary number
	or 1 of log_base_2 number of levels in FFT. I'll think about it more later.
	For now I'm not trying to make use of waves in that way. I'm using only
	the simpler waves that flow between attention and bayesian weights.
	That FFT analogy (or maybe use it directly in some form?) is about
	waves in the continuous path between short-term and long-term memory
	which is now a discrete path with log_base_2 number of levels.
	*/
	private boolean recurseAlternator;
	
	/** Range 0 to axonSize-1 are parent Nodes if we think of xX, yY, and zZ as child Nodes.
	<br><br>
	Another way to think of it is xX, yY, and zZ are synapses
	and these are Nodes the axon branches to.
	<br><br>
	It may appear that the design of only allowing 3 synapses/childs is short sighted
	and maybe later we would want to have larger bayesian nodes than size 3,
	but this is Turing Complete because it can calculate NAND and statistical
	variations of it as the powerset of 3 synapse/child Nodes,
	and it can spread activation with unlimited branching factor like a neural net
	through this axon[] of parent Nodes.
	There is much debate between AI researchers about how much of intelligence
	happens inside neurons, instead of mostly being between them.
	My answer to that is to not model neurons or axons directly
	and to instead define this Node class as something that can model
	parts of an axon, parts of a neuron, parts of a synapse,
	and generally any kind of high dimensional, fractal, or fields of patterns.
	This can model the timing and echos and directions of flow in an axon
	in how many Nodes could form that axon. I don't mean to directly
	build this into shapes found in brains, because its more general than that,
	but it would be a good test case.
	*/
	private Node axon[];
	private int axonSize;
	
	private double attention_decayToward_aveAccuracyOfChance = .1;
	//private float attention_decayToward_aveChanceOfThisNode = .5;
	
	//private double bayesianWeights_decayToward_predictionsOfChilds = .1;
	//private float bayesianWeights_decayToward_observationsOfChilds = .4;
	
	//private double bayesianWeights_decayToward_linearObservationsOfChilds = .1;
	
	private double bayesianWeights_decay = .1;
	
	private double accuracyMeasuredLastRun;
	public double getAccuracyMeasuredLastRun(){ return accuracyMeasuredLastRun; }
	
	private final String nameForTesting;
	
	/** The 3 bayesian childs are null, and all Node start with an empty axon Node array,
	so run() won't do anything until at least 1 of those changes.
	*/
	public Node(String name){
		this(name, .5, new Node[]{null, null, null}, newWeights(3,null));
	}
	
	/** Weights are in little-endian order relative to childs. */
	public Node(String nameForTesting, double attention, Node childs[], double weights[]){
		this.nameForTesting = nameForTesting;
		if(childs.length != 3) throw new RuntimeException(childs.length+" childs");
		if(weights.length != 8) throw new RuntimeException(childs.length+" weights");
		this.chance = attention;
		xX = childs[0];
		yY = childs[1];
		zZ = childs[2];
		zyx = weights[0];
		zyX = weights[1];
		zYx = weights[2];
		zYX = weights[3];
		Zyx = weights[4];
		ZyX = weights[5];
		ZYx = weights[6];
		ZYX = weights[7];
		//testWeights();
	}
	
	
	/** Updates only variables in this Node, not its 3 childs (x, y, z)
	or its axon nodes (variable size).
	<br><br>
	Updates the "attention" var based on these 2 things:
	<br><br>
	(1) Average (TODO median?) chance of this Node in each axon Node, if axon isn't empty.
	<br><br>
	(2) Accuracy of the 8 bayesian weights (zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX)
	in predicting the "attention" var of the 3 bayesian childs.
	TODO Should "accuracy" be the same as "bayesian chance" or should that
	be a different var? It depends if we want to use "attention" recursively.
	<br><br>
	Updates the 8 bayesian weights (zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX),
	and recursively into the halfSpeed Node if it exists,
	a little toward the 3 bayesian childs.
	<br><br>
	*/
	public void run(){
		if(axonSize > 0){
			//testWeights();
			//(1) Average (TODO median?) chance of this Node in each axon Node, if axon isn't empty.
			double sum = 0;
			double observations[] = new double[axonSize];
			double observation_attention[] = new double[axonSize];
			double accuracySum = 0;
			double totalAttentionSummedWithChance = 0;
			for(int i=0; i<axonSize; i++){
				Node n = axon[i];
				//n.testWeights();
				double myChanceInN;
				if(n.xX == this){
					//myChanceInN = n.predictChanceWithoutObservingAny(0);
					myChanceInN = n.observeOthersThenPredictChanceOf(0);
				}else if(n.yY == this){
					//myChanceInN = n.predictChanceWithoutObservingAny(1);
					myChanceInN = n.observeOthersThenPredictChanceOf(1);
				}else if(n.zZ == this){
					//myChanceInN = n.predictChanceWithoutObservingAny(2);
					myChanceInN = n.observeOthersThenPredictChanceOf(2);
				}else{
					throw new RuntimeException("Pair of links is broken. Found "+n
						+" in my axon Nodes but I'm not 1 of its bayesian childs. I am: "+this);
				}
				//TODO use current average instead of attention which is a decaying average
				
				double stdDevScale = 1.5; //Display this many standard deviations
				double normedObservation = .5 + .5*(myChanceInN-chance)/(chanceStdDev*stdDevScale);
				normedObservation = Math.max(0, Math.min(normedObservation, 1));
				
				//sum += normedObservation;
				
				//Each nonleaf Node gambles, 3 times per run,
				//attention amount of chance_range_0_to_1.
				//Attention can be any positive number and is conserved overall between Nodes.
				
				sum += normedObservation*n.attention;
				//sum += myChanceInN*n.attention;
				totalAttentionSummedWithChance += n.attention;
				observations[i] = normedObservation;
				//observations[i] = myChanceInN;
				observation_attention[i] = n.attention;
				
				
				//n.testWeights();
				accuracySum += n.accuracyMeasuredLastRun*n.attention;
			}
			//double decay = attention_decayToward_aveChanceOfThisNode;
			
			if(axonSize > 1){
				//double aveObservation = sum/axonSize;
				double aveObservation = sum/totalAttentionSummedWithChance;
				double sumOfSquares = 0;
				for(int i=0; i<axonSize; i++){
					double diff = observations[i] - aveObservation;
					sumOfSquares += observation_attention[i]*diff*diff;
				}
				double stdDev = Math.sqrt(sumOfSquares/totalAttentionSummedWithChance);
				//stdDev = Math.max(chanceStdDev_min, stdDev);
				//chanceStdDev = stdDev;
				double decay = attention_decayToward_aveAccuracyOfChance;
				chanceStdDev = chanceStdDev*(1-decay) + decay*stdDev;
			}
			
			
			//testWeights();
			//double aveChanceOfThisNode = sum/axonSize;
			//double aveChanceOfThisNode = sum/totalAttentionSummedWithChance;
			//chance = chance*(1-decay) + decay*aveChanceOfThisNode;
			//testWeights();
			if(chance < 0 || chance > 1){
				throw new RuntimeException("attention out of range: "+chance);
			}
			//testWeights();
			accuracyMeasuredLastRun = accuracySum/totalAttentionSummedWithChance;
		}
		
		if(xX != null){
			//(2) Accuracy of the 8 bayesian weights (zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX)
			//in predicting the "attention" var of the 3 bayesian childs.
			//TODO There should be 2 kinds of accuracy vars: 1 for predictChanceWithoutObservingAny and 1 for observeOthersThenPredictChanceOf
			double observedChanceX = xX.chance;
			//double predictedChanceX = predictChanceWithoutObservingAny(0);
			double predictedChanceX = observeOthersThenPredictChanceOf(0);
			double observedChanceY = yY.chance;
			//double predictedChanceY = predictChanceWithoutObservingAny(1);
			double predictedChanceY = observeOthersThenPredictChanceOf(1);
			double observedChanceZ = zZ.chance;
			//double predictedChanceZ = predictChanceWithoutObservingAny(2);
			double predictedChanceZ = observeOthersThenPredictChanceOf(2);
			double totalDiff = Math.abs(observedChanceX-predictedChanceX)
				+Math.abs(observedChanceY-predictedChanceY)
				+Math.abs(observedChanceZ-predictedChanceZ);
			double diffFraction = totalDiff/3; //range 0 (least accurate) to 1
			//TODO find a less arbitrary way to define accuracyMeasuredLastRun
			accuracyMeasuredLastRun = Math.min(.1/(diffFraction+.1), 1);
		//}//else{
			//TODO spread accuracy like "spreading activation", simple blur, instead of this "else".
			//for(axon...)
		//}
		
		
		//if(xX != null){
			//Updates the 8 bayesian weights (zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX),
			//and indirectly (later) recursively into the halfSpeed Node if it exists,
			//a little toward the 3 bayesian childs.
			
			/*double observeX = xX.chance;
			double observeY = yY.chance;
			double observeZ = zZ.chance;
			*/
			
			/*double observeX = predictedChanceX; //based on observing other 2
			double observeY = predictedChanceY;
			double observeZ = predictedChanceZ;
			*/
			
			//TODO use
			//bayesianWeights_decayToward_predictionsOfChilds
			//bayesianWeights_decayToward_linearObservationsOfChilds
			
			/*double observeX = (xX.chance+predictedChanceX)/2; //based on observing other 2
			double observeY = (yY.chance+predictedChanceY)/2;
			double observeZ = (zZ.chance+predictedChanceZ)/2;
			*/
			
			double d = .02;
			
			double observeX = xX.chance*d+(1-d)*predictedChanceX; //based on observing other 2
			double observeY = yY.chance*d+(1-d)*predictedChanceY;
			double observeZ = zZ.chance*d+(1-d)*predictedChanceZ;
			
			
			double targetzyx = (1-observeZ)*(1-observeY)*(1-observeX);
			double targetzyX = (1-observeZ)*(1-observeY)*observeX;
			double targetzYx = (1-observeZ)*observeY*(1-observeX);
			double targetzYX = (1-observeZ)*observeY*observeX;
			double targetZyx = observeZ*(1-observeY)*(1-observeX);
			double targetZyX = observeZ*(1-observeY)*observeX;
			double targetZYx = observeZ*observeY*(1-observeX);
			double targetZYX = observeZ*observeY*observeX;
			double decay = bayesianWeights_decay;
			zyx = zyx*(1-decay) + decay*targetzyx;
			zyX = zyX*(1-decay) + decay*targetzyX;
			zYx = zYx*(1-decay) + decay*targetzYx;
			zYX = zYX*(1-decay) + decay*targetzYX;
			Zyx = Zyx*(1-decay) + decay*targetZyx;
			ZyX = ZyX*(1-decay) + decay*targetZyX;
			ZYx = ZYx*(1-decay) + decay*targetZYx;
			ZYX = ZYX*(1-decay) + decay*targetZYX;
		}
		
		double attDecay = attention_decayToward_aveAccuracyOfChance;
		//double targetAtt = Math.log(accuracyMeasuredLastRun);
		double targetAtt = .5+.1*accuracyMeasuredLastRun;
		attention = attention*(1-attDecay) + attDecay*targetAtt;
		
		runFullMemoryLinkedList();
		normWeights();
		
		//testWeights();
	}
	
	/** Recursively runs the end of the halfSpeed linked list first,
	1 pair of Node in that list at a time,
	the runs pairs earlier in the list. Each cycle, 2 nodes average their
	8 bayesian weights together to get 8 weights in 2 duplicates each.
	Next run(), the earliest Node in the linked list changes those weights.
	That is the input and output to the rest of the network.
	<br><br>
	TODO use recurseAlternator instead of running the whole linked list.
	*/
	private void runFullMemoryLinkedList(){
		if(halfSpeed == null) return;
		halfSpeed.runFullMemoryLinkedList();
		zyx = (zyx+halfSpeed.zyx)/2;
		zyX = (zyX+halfSpeed.zyX)/2;
		zYx = (zYx+halfSpeed.zYx)/2;
		zYX = (zYX+halfSpeed.zYX)/2;
		Zyx = (Zyx+halfSpeed.Zyx)/2;
		ZyX = (ZyX+halfSpeed.ZyX)/2;
		ZYx = (ZYx+halfSpeed.ZYx)/2;
		ZYX = (ZYX+halfSpeed.ZYX)/2;
		attention = (attention+halfSpeed.attention)/2;
		normWeights();
		halfSpeed.zyx = zyx;
		halfSpeed.zyX = zyX;
		halfSpeed.zYx = zYx;
		halfSpeed.zYX = zYX;
		halfSpeed.Zyx = Zyx;
		halfSpeed.ZyX = ZyX;
		halfSpeed.ZYx = ZYx;
		halfSpeed.ZYX = ZYX; //normed
		halfSpeed.attention = attention;
		//testWeights();
		//halfSpeed.testWeights();
	}
	
	private void testWeights(){
		double sum = zyx+zyX+zYx+zYX+Zyx+ZyX+ZYx+ZYX;
		if(sum < .999999 || sum > 1.000001){
			throw new RuntimeException("Weights sum to "+sum+" in "+this);
		}
	}
	
	/** Causes zyx+zyX+zYx+zYX+Zyx+ZyX+ZYx+ZYX to be as close to 1 as possible, except for roundoff-error.
	This is called at the end of run() but must also be called after weights are changed externally,
	unless the norming is also done externally.
	*/
	private void normWeights(){
		double sum = zyx+zyX+zYx+zYX+Zyx+ZyX+ZYx+ZYX;
		zyx /= sum;
		zyX /= sum;
		zYx /= sum;
		zYX /= sum;
		Zyx /= sum;
		ZyX /= sum;
		ZYx /= sum;
		ZYX /= sum;
		//testWeights();
	}
	
	/** bayesianChild is 0 for X, 1 for Y, or 2 for Z.
	Returns between 0.0 (false) and 1.0 (true).
	TODO range limits a little above 0.0 and a little below 1.0.
	*/
	private double predictChanceWithoutObservingAny(int bayesianChild){
		switch(bayesianChild){
		case 0: return ZYX+zYX+ZyX+zyX;
		case 1: return ZYX+zYX+ZYx+zYx;
		case 2: return ZYX+ZyX+ZYx+Zyx;
		}
		throw new RuntimeException("bayesianChild="+bayesianChild);
	}
	
	/** Uses the Node.attention (which affects my 8 bayesian weights gradually)
	of each bayesian child except the one specified, and the 8 bayesian weights,
	to predict the Node.attention of the bayesian child specified.
	<br><br>
	Bayes' Rule:
	chance(X, given Y) * chance(Y) = chance(Y, given X) * chance(X)
	*/
	private double observeOthersThenPredictChanceOf(int bayesianChild){
		double observedChanceX = xX.chance;
		double observedChanceY = yY.chance;
		double observedChanceZ = zZ.chance;
		double tempzyx=zyx, tempzyX=zyX, tempzYx=zYx, tempzYX=zYX,
			tempZyx=Zyx, tempZyX=ZyX, tempZYx=ZYx, tempZYX=ZYX;
		double predictA, predictB = 0;
		//Set X first, Y second, then predict Z. Then reset and set Y first,
		//then set X, then predict Z. Do this for all 3 vars to predict in switch block.
		//For now only doing it 1 way, since its approximately equal but a little distorted.
		//TODO Use the bayesian calculus in my notes from those papers to do this exactly
		//except for roundoff error. I still have to check the math on those papers.
		switch(bayesianChild){
		case 0: //predict X
			setPredictedChanceWithoutObservingAny(1, observedChanceY);
			setPredictedChanceWithoutObservingAny(2, observedChanceZ);
			predictA = predictChanceWithoutObservingAny(0);
			break;
		case 1: //predict Y
			setPredictedChanceWithoutObservingAny(0, observedChanceX);
			setPredictedChanceWithoutObservingAny(2, observedChanceZ);
			predictA = predictChanceWithoutObservingAny(1);
		break;
		case 2: //predict Z
			setPredictedChanceWithoutObservingAny(0, observedChanceX);
			setPredictedChanceWithoutObservingAny(1, observedChanceY);
			predictA = predictChanceWithoutObservingAny(2);
		break;
		default:
			throw new RuntimeException("bayesianChild="+bayesianChild);
		}
		//Reset chances to before this function was called
		zyx = tempzyx;
		zyX = tempzyX;
		zYx = tempzYx;
		zYX = tempzYX;
		Zyx = tempZyx;
		ZyX = tempZyX;
		ZYx = tempZYx;
		ZYX = tempZYX;
		//Reverse order of set chances and predict again
		//for more accuracy with 2 data points.
		switch(bayesianChild){
		case 0: //predict X
			setPredictedChanceWithoutObservingAny(2, observedChanceZ);
			setPredictedChanceWithoutObservingAny(1, observedChanceY);
			predictB = predictChanceWithoutObservingAny(0);
			break;
		case 1: //predict Y
			setPredictedChanceWithoutObservingAny(2, observedChanceZ);
			setPredictedChanceWithoutObservingAny(0, observedChanceX);
			predictB = predictChanceWithoutObservingAny(1);
		break;
		case 2: //predict Z
			setPredictedChanceWithoutObservingAny(1, observedChanceY);
			setPredictedChanceWithoutObservingAny(0, observedChanceX);
			predictB = predictChanceWithoutObservingAny(2);
		}
		//Restore state to what it was before this function was called.
		zyx = tempzyx;
		zyX = tempzyX;
		zYx = tempzYx;
		zYX = tempzYX;
		Zyx = tempZyx;
		ZyX = tempZyX;
		ZYx = tempZYx;
		ZYX = tempZYX;
		return (predictA+predictB)/2;
	}
	
	/** bayesianChild is 0 for X, 1 for Y, or 2 for Z.
	<br><br>
	Returns between 0.0 (false) and 1.0 (true).
	<br><br>
	TODO range limits a little above 0.0 and a little below 1.0.
	<br><br>
	TODO call normWeights() after calling this some small number of times,
	depending on how much roundoff-error is ok to build up exponentially.
	*/
	private void setPredictedChanceWithoutObservingAny(int bayesianChild, double chance){
		double oldChance = predictChanceWithoutObservingAny(bayesianChild);
		//TODO error if not "TODO range limits a little above 0.0 and a little below 1.0."
		double multTrue = chance / oldChance;
		double multFalse = (1 - chance)/(1 - oldChance);
		switch(bayesianChild){
		case 0:
			zyx *= multFalse;
			zyX *= multTrue;
			zYx *= multFalse;
			zYX *= multTrue;
			Zyx *= multFalse;
			ZyX *= multTrue;
			ZYx *= multFalse;
			ZYX *= multTrue;
		break;
		case 1:
			zyx *= multFalse;
			zyX *= multFalse;
			zYx *= multTrue;
			zYX *= multTrue;
			Zyx *= multFalse;
			ZyX *= multFalse;
			ZYx *= multTrue;
			ZYX *= multTrue;
		break;	
		case 2:
			zyx *= multFalse;
			zyX *= multFalse;
			zYx *= multFalse;
			zYX *= multFalse;
			Zyx *= multTrue;
			ZyX *= multTrue;
			ZYx *= multTrue;
			ZYX *= multTrue;
		break;
		default: throw new RuntimeException("bayesianChild="+bayesianChild);
		}
		//TODO norm weights how often?
	}
	
	
	/** Does not check for duplicates. Enlarges axon array if needed. */
	public void addToAxon(Node n){
		if(axon == null){
			axon = new Node[4];
		}else if(axonSize == axon.length){
			Node axon2[] = new Node[axon.length*2];
			System.arraycopy(axon, 0, axon2, 0, axon.length);
			axon = axon2;
		}
		axon[axonSize++] = n;
	}
	
	
	/** Adds 1 more Node to the halfSpeed linked list */
	public void growMemoryBinaryList(){
		if(halfSpeed != null){
			halfSpeed.growMemoryBinaryList();
		}else{
			Node childs[] = new Node[]{xX, yY, zZ};
			double weights[] = new double[]{zyx, zyX, zYx, zYX, Zyx, ZyX, ZYx, ZYX};
			halfSpeed = new Node(nameForTesting+"+", chance, childs, weights);
		}
	}
	
	
	
	
	
	
	private static final double DONT_BE_TOO_CERTAIN = 1./256;
	
	
	
	/** If randomOrNull is null, fills evenly, else randomly that sums to 1 */
	public static double[] newWeights(int childs, Random randomOrNull){
		double w[] = new double[1 << childs];
		if(randomOrNull != null){
			double sum = 0;
			for(int i=0; i<w.length; i++){
				sum += w[i] = randomOrNull.nextDouble();
			}
			if(sum == 0) return newWeights(childs, randomOrNull);
			for(int i=0; i<w.length; i++) w[i] /= sum;
			return w;
		}else{
			Arrays.fill(w, 1./w.length);
		}
		return w;
	}
	
	
	/** Test this class */
	private static void main(String args[]) throws Exception{
		Node x = new Node("x"), y = new Node("y"), z = new Node("z");
		double weights[] = new double[]{ //sums to 1, except for roundoff error
			     //zyx since its little-endian alignment to bits in each index.
			.05, //000
			.10, //001
			.02, //010
			.19, //011
			.17, //100
			.23, //101
			.20, //110
			.04  //111
		};
		System.out.println("weights="+floArrayToString(weights));
		double sumWeights = 0;
		for(double d : weights) sumWeights += d;
		testNear(sumWeights, 1, "sumWeights");
		Node b = new Node("b", .5, new Node[]{x, y, z}, weights);
		//Do dot(Vecnet) and dot(int) in random-appearing order to catch possible bugs
		//which may depend on the order such functions are called.
		double chanceX = .10+.19+.23+.04;
		double chanceY = .02+.19+.20+.04;
		double chanceZ = .17+.23+.20+.04;
		System.out.println("chanceX="+chanceX+" chanceY="+chanceY+" chanceZ="+chanceZ);
		testNear(b.predictChanceWithoutObservingAny(0), chanceX, "chance of x by index");
		testNear(b.predictChanceWithoutObservingAny(1), chanceY, "chance of y by index");
		testNear(b.predictChanceWithoutObservingAny(2), chanceZ, "chance of z by index");
		//Next use setDot functions and test dot functions again expecting different numbers.
		double setChanceZ2 = chanceZ/2;
		System.out.println("setChanceZ2="+setChanceZ2+" chanceZ="+chanceZ+"" +
			"\r\nsetChanceZ2/chanceZ="+setChanceZ2/chanceZ+" (1-setChanceZ2)/(1-chanceZ)="+(1-setChanceZ2)/(1-chanceZ));
		//TODO this isn't the calculation I wanted??? It leaves x and y the same chance. This is what setDot maybe should do, but its not what I was thinking in doing all vars continuously in a bayesian network, like to align to the current observation then maximize the "accuracy of prediction of mouse movements half a second ahead of time" var that bayes is also measuring statistics of.
		double weights2Correct[] = new double[]{ //sums to 1, except for roundoff error
			                                 //zyx since its little-endian alignment to bits in each index.
			.05*(1-setChanceZ2)/(1-chanceZ), //000
			.10*(1-setChanceZ2)/(1-chanceZ), //001
			.02*(1-setChanceZ2)/(1-chanceZ), //010
			.19*(1-setChanceZ2)/(1-chanceZ), //011
			.17*setChanceZ2/chanceZ,         //100
			.23*setChanceZ2/chanceZ,         //101
			.20*setChanceZ2/chanceZ,         //110
			.04*setChanceZ2/chanceZ          //111
		};
		System.out.println("weights2Correct="+floArrayToString(weights2Correct));
		double sumWeights2 = 0;
		for(double d : weights2Correct) sumWeights2 += d;
		testNear(sumWeights2, 1, "sumWeights2");
		//Node b2 = (Node) b.setDot(z, setChanceZ2);
		
		/*
		b.setDot(z, setChanceZ2);
		double chanceX2 = weights2Correct[1]+weights2Correct[3]+weights2Correct[5]+weights2Correct[7];
		double chanceY2 = weights2Correct[2]+weights2Correct[3]+weights2Correct[6]+weights2Correct[7];
		double chanceZ2 = weights2Correct[4]+weights2Correct[5]+weights2Correct[6]+weights2Correct[7];
		System.out.println("chanceX2="+chanceX2+" chanceY2="+chanceY2+" chanceZ2="+chanceZ2);
		testNear(setChanceZ2, chanceZ2, "setChanceZ2 chanceZ2");
		testNear(chanceX2, b.dot(x), "chanceX2");
		testNear(chanceY2, b.dot(y), "chanceY2");
		testNear(chanceZ2, b.dot(z), "chanceZ2");
		
		//TODO Also test when some dot are 0 or 1, which would result in divide-by-zero errors
		//if not handled correctly.
		
		//TODO use -1 as bayesianfalse and 1 as bayesiantrue, instead of 0 to 1,
		//but do the same calculation as if it was 0 to 1, just scaling and moving the range.
		*/
		throw new RuntimeException("Passed all tests. TODO uncomment and translate tests above, originally from Human AI Net 0.6.3 bayesianvector.vecnets.wavefunctions.BayesianPowerset");
	}
	
	/** for limiting roundoff error in tests */
	private static final double epsilon = .000000001;
	
	private static void testNear(double x, double y, String description) throws Exception{
		double diff = Math.abs(x-y);
		if(diff > epsilon) throw new Exception("testNear failed. x="+x+" y="+y
			+" diff="+diff+" epsilon="+epsilon+" description=["+description+"]");
	}
	
	private static String floArrayToString(double flos[]){
		StringBuilder sb = new StringBuilder("[");
		for(double d : flos){
			if(sb.length() > 1) sb.append(", ");
			sb.append(d);
		}
		return sb.append(']').toString();
	}
	
	private String weightsToString(){
		return new StringBuilder("[")
			.append(zyx).append(", ")
			.append(zyX).append(", ")
			.append(zYx).append(", ")
			.append(zYX).append(", ")
			.append(Zyx).append(", ")
			.append(ZyX).append(", ")
			.append(ZYx).append(", ")
			.append(ZYX).append("]").toString();
	}
	
	public String toString(){
		return "[Node_"+nameForTesting+" weights="+weightsToString()+" attention="+chance+" attentionStdDev="+chanceStdDev+" halfSpeedDepth="+halfSpeedDepth()+"]";
	}
}
