import java.util.function.Function;

public class AdvancedRewardShaper implements RewardShaper {
	public Environment env;
	public Agent agent;
	public int division;//all tomatoes on the right of this division are our neigbours'
	Function<Integer, Double> potentialOverDistance;
	
	public AdvancedRewardShaper(Environment env, Agent agent, int division, Function<Integer, Double> potentialOverDistance) {
		this.env = env;
		this.agent = agent;
		this.potentialOverDistance = potentialOverDistance;
		this.division = division;
	}
	
	@Override
	public double getPotential() {
		//find closest unwatered neighbour tomato
				
		//not all our tomatoes are watered
		boolean b = true;
		for (int i = 1; i <= division; i++){
			for (int j = 1; j < this.env.getHeight(); j++){
				if (this.env.getTile(i, j) == Tile.UNWATERED_TOMATO) b = false;
			}		
		}
		if (!b){
			//find closest neighbours unwatered tomato
			int minDistToUnwateredTomato = 1000000000;
			for(int x = division + 1; x < env.getWidth() - 1; x++) {
				for(int y = 1; y < env.getHeight() - 1; y++) {
					int dist = Math.abs(x - agent.x) + Math.abs(y - agent.y);
					if(env.getTile(x, y) == Tile.UNWATERED_TOMATO && dist < minDistToUnwateredTomato) {
						minDistToUnwateredTomato = dist;
					}
				}
			}
			
			return potentialOverDistance.apply(minDistToUnwateredTomato);
		}
		return 0;
	}
}