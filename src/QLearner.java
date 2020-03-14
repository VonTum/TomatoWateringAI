import java.util.ArrayList;
import java.util.Random;

public class QLearner {
	double[][] qValues;
	
	public double discount;
	public double exploration;
	public double learningRate;
	
	int[] observationSizes;
	
	private Random r = new Random();
	
	public QLearner(int numberOfActions, double learningRate, double discount, double exploration, int... observationSizes) {
		this.discount = discount;
		this.exploration = exploration;
		this.learningRate = learningRate;
		this.observationSizes = observationSizes;
		
		int totalObservationSize = 1;
		for(int size : observationSizes) {
			totalObservationSize *= size;
		}
		qValues = new double[totalObservationSize][numberOfActions];
		
		for(double[] values : qValues) {
			for(int i = 0; i < values.length; i++) {
				values[i] = 0.0;
			}
		}
		
		System.out.println("Created QLearner with " + totalObservationSize + " possible cases");
	}
	
	private double getQFor(int action, int observationID) {
		return qValues[observationID][action];
	}
	
	private void setQFor(int action, double newQ, int observationID) {
		qValues[observationID][action] = newQ;
	}
	
	public double getQFor(int action, int... observations) {
		return getQFor(action, observationsListToID(observations));
	}
	
	public void setQFor(int action, double newQ, int... observations) {
		setQFor(action, newQ, observationsListToID(observations));
	}
	
	public int getNumberOfActions() {
		return qValues[0].length;
	}
	
	public int getNumberOfObservations() {
		return qValues.length;
	}
	
	public int getBestAction(int... observations) {
		int index = observationsListToID(observations);
		
		int bestAction = 0;
		double bestQ = Double.NEGATIVE_INFINITY;
		
		for(int action = 0; action < getNumberOfActions(); action++) {
			double q = getQFor(action, index);
			if(q > bestQ) {
				bestQ = q;
				bestAction = action;
			}
		}
		return bestAction;
	}
	
	public int getNextAction(int... observations) {
		if(r.nextDouble() > exploration) {
			return r.nextInt(getNumberOfActions());
		} else {
			return getBestAction(observations);
		}
	}
	
	private int observationsListToID(int... observations) {
		int total = 0;
		for(int i = 0; i < observationSizes.length; i++) {
			total *= observationSizes[i];
			total += observations[i];
		}
		return total;
	}
	
	private int[] observationIDToList(int observationID) {
		int[] result = new int[observationSizes.length];
		
		int cur = observationID;
		for(int i = observationSizes.length - 1; i >= 0; i--) {
			int multiplier = observationSizes[i];
			result[i] = cur % multiplier;
			cur = cur / multiplier;
		}
		
		return result;
	}
	
	private String observationsToString(int... observations) {
		StringBuilder builder = new StringBuilder();
		
		String prefix = "";
		for(int obs : observations) {
			builder.append(prefix).append(obs);
			prefix = ", ";
		}
		
		return builder.toString();
	}
	
	private String observationIDToString(int observationID) {
		return observationsToString(observationIDToList(observationID));
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		int numberOfObservations = getNumberOfObservations();
		int numberOfActions = getNumberOfActions();
		
		for(int o = 0; o < numberOfObservations; o++) {
			builder.append(observationIDToString(o) + ": ");
			for(int a = 0; a < numberOfActions; a++) {
				double q = getQFor(a, o);
				
				builder.append(String.format("%.4f ", q));
			}
			builder.append('\n');
		}
		return builder.toString();
	}
	
	public class History {
		public ArrayList<HistoryElement> history;
		
		public History() {
			this.history = new ArrayList<HistoryElement>();
		}
		
		public History(int expectedLength) {
			this.history = new ArrayList<HistoryElement>(expectedLength);
		}
		
		public void visit(int action, double reward, int... observations) {
			history.add(new HistoryElement(observationsListToID(observations), action, reward));
		}
		
		public void apply(int... lastObservation) {
			int lastObservationID = observationsListToID(lastObservation);
			for(int i = history.size() - 1; i >= 0; i--) {
				HistoryElement el = history.get(i);
				
				double bestQOfNextState = getQFor(0, lastObservationID);
				
			    for (int j = 1; j < getNumberOfActions(); j++) {
			    	double q = getQFor(j, lastObservationID);
			        if (q > bestQOfNextState) {
			        	bestQOfNextState = q;
			        }
			    }
			    
				double targetQ = el.reward + discount * bestQOfNextState;
				double oldQ = getQFor(el.action, el.observation);
				
				
				double resultingQ = oldQ + learningRate * (targetQ - oldQ);
				
				setQFor(el.action, resultingQ, el.observation);
				lastObservationID = el.observation;
			}
		}
		
		public double getTotalReward() {
			double total = 0.0;
			for(HistoryElement el : history) {
				total += el.reward;
			}
			return total;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for(HistoryElement el : history) {
				builder.append(observationIDToString(el.observation));
				
				builder.append(": " + el.action + " => " + el.reward + "\n");
			}
			
			return builder.toString();
		}
	}
	
	private static class HistoryElement {
		public int observation;
		public int action;
		public double reward;
		
		public HistoryElement(int observation, int action, double reward) {
			this.observation = observation;
			this.action = action;
			this.reward = reward;
		}
	}
}
