
public interface RestrainingBolt {
	public int getNumberOfStates();
	public int getCurrentState();
	// returns the extra reward the bolt gives as a result of the action of the agent
	public double applyAction(int action);
	public void reset();
}
