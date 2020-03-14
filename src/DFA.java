
public class DFA {
	
	public DFASystem system;
	public int curState;
	
	public DFA(DFASystem system, int curState) {
		this.system = system;
		this.curState = curState;
	}
	
	public void applyAction(int action) {
		curState = system.getNextState(curState, action);
	}
}
