
public class BasicRestrainingBolt implements RestrainingBolt {
	
	
	private Environment env;
	private Agent agent;
	
	public BasicRestrainingBolt(Environment env, Agent agent) {
		this.env = env;
		this.agent = agent;
	}
	
	
	@Override
	public int getNumberOfStates() {
		return 1;
	}

	@Override
	public int getCurrentState() {
		return 0;
	}

	@Override
	public double applyAction(int action) {
		return agent.x <= 3 ? 0.0 : -5.0;
	}
	
	@Override
	public void reset() {
		
	}
}
