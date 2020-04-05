
public class AdvancedRestrainingBolt implements RestrainingBolt{

	private Environment env;
	private Agent agent;
	
	public AdvancedRestrainingBolt(Environment env, Agent agent) {
		this.env = env;
		this.agent = agent;
		this.setState(0);
	}	
	
	@Override
	public int getNumberOfStates() {
		return 2;
	}
	
	//1 if all our plants are watered, 0 otherwise
	public int state;
	
	public void setState(int i){
		this.state = i;
	}
	
	@Override
	public int getCurrentState() {
		return this.state;
	}

	//return -10 if watering neighbour plants when our plants are not all watered
	@Override
	public double applyAction(int action) {
		if (checkState()){
			return -10;
		}
		else return 0;
	}
	
	//true if all our tommatoes are watered
	private boolean checkOurTomatoesWatered(){
		boolean b = true;
		for (int i = 0; i <= 3; i++){
			for (int j = 0; j <= this.env.getHeight(); j++){
				if (this.env.getTile(i, j) == Tile.UNWATERED_TOMATO) b = false;
			}		
		}
		this.setState(b ? 1 : 0);
		return b;
	}
	
	//true if we try to water a neighbours tomato when not all our tomatoes are watered 
	private boolean checkState() {
		boolean b = false;
		//assumes our tomatoes are on x <= 3
		if (checkOurTomatoesWatered() && this.agent.x > 3 && this.env.getTile(this.agent.x, this.agent.y) == Tile.UNWATERED_TOMATO){
			b = true;					
		}
		return b;
	}

	@Override
	public void reset() {
		this.setState(0);
	}
}
