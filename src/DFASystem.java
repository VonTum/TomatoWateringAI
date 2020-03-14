
public class DFASystem {
	private int[][] nextStateForAction;
	
	public DFASystem(int numberOfStates, int numberOfActions) {
		nextStateForAction = new int[numberOfActions][numberOfStates];
	}
	
	void setNextState(int state, int action, int nextState) {
		nextStateForAction[action][state] = nextState;
	}
	
	int getNextState(int state, int action) {
		return nextStateForAction[action][state];
	}
	
	int getNumberOfState() {
		return nextStateForAction[0].length;
	}
	
	int getNumberOfActions() {
		return nextStateForAction.length;
	}
}
