
public class AdvancedRestrainingBolt implements RestrainingBolt{

	private Environment env;
	private Agent agent;
	
	//1 if all our plants have ever been watered, at any time, 0 otherwise
	public boolean isAllowedToWaterNeighbor;
	
	public AdvancedRestrainingBolt(Environment env, Agent agent) {
		this.env = env;
		this.agent = agent;
		this.isAllowedToWaterNeighbor = false;
	}	
	
	@Override
	public int getNumberOfStates() {
		return 2;
	}
	
	@Override
	public int getCurrentState() {
		return this.isAllowedToWaterNeighbor ? 1 : 0;
	}
	
	//return -10 if watering neighbour plants when our plants are not all watered
	@Override
	public double applyAction(int action) {
		if(areAllOurTomatoesWatered()) {
			this.isAllowedToWaterNeighbor = true;
		}
		if (!this.isAllowedToWaterNeighbor){
			Position nextPos = env.getPositionWhenMoving(agent.x, agent.y, Move.values()[action]);
			if (this.agent.x > env.ourTomatoesWidth+1 && this.env.getTile(nextPos.x, nextPos.y) == Tile.UNWATERED_TOMATO){
				return -20;
			}
		}
		return 0;
	}
	
	//true if all our tomatoes are watered
	private boolean areAllOurTomatoesWatered(){
		for (int x = 1; x <= env.ourTomatoesWidth+1; x++){
			for (int y = 1; y <= this.env.getHeight() - 1; y++){
				if (this.env.getTile(x, y) == Tile.UNWATERED_TOMATO) return false;
			}		
		}
		return true;
	}
	
	@Override
	public void reset() {
		this.isAllowedToWaterNeighbor = false;
	}
}
