import java.util.function.Function;

public class TomatoProximityShaper implements RewardShaper {
	public Environment env;
	public Agent agent;
	Function<Integer, Double> potentialOverDistance;
	
	public TomatoProximityShaper(Environment env, Agent agent, Function<Integer, Double> potentialOverDistance) {
		this.env = env;
		this.agent = agent;
		this.potentialOverDistance = potentialOverDistance;
	}
	
	@Override
	public double getPotential() {
		int minDistToUnwateredTomato = 1000000000;
		for(int x = 1; x < env.getWidth() - 1; x++) {
			for(int y = 1; y < env.getHeight() - 1; y++) {
				int dist = Math.abs(x - agent.x) + Math.abs(y - agent.y);
				if(env.getTile(x, y) == Tile.UNWATERED_TOMATO && dist < minDistToUnwateredTomato) {
					minDistToUnwateredTomato = dist;
				}
			}
		}
		return potentialOverDistance.apply(minDistToUnwateredTomato);
	}
}
