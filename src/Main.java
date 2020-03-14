import java.util.Random;

public class Main {
	
	static Environment env;
	static QLearner learner;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(8,  6, 35879879654161L);
		
		
		learner = new QLearner(4, 0.9, 0.90, 0.3, env.getWidth() - 2, env.getHeight() - 2, 1 << env.getNumberOfTomatoes());
		
		int actionCount = 100;
		
		learn(100000, actionCount);
		//System.out.println(learner);
		
		//Thread.sleep(5000);
		
		env.reset();
		Agent agent = new Agent(env, 2, 2);
		for(int i = 0; i < actionCount; i++) {
			int[] observations = {agent.x - 1, agent.y - 1, env.getTomatoBitmap()};
			int action = learner.getBestAction(observations);
			agent.move(Move.values()[action]);
			dryout();
			
			System.out.println(env);
			
			Thread.sleep(500);
		}
	}
	
	private static final Random random = new Random();
	private static void dryout() {
		for(int x = 1; x < env.getWidth() - 1; x++) {
			for(int y = 1; y < env.getHeight() - 1; y++) {
				if(env.getTile(x, y) == Tile.WATERED_TOMATO) {
					if(random.nextDouble() < 0.1) {
						env.setTile(x, y, Tile.UNWATERED_TOMATO);
					}
				}
			}
		}
	}
	
	private static void learn(int episodes, int steps) {
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0) System.out.print("Episode: " + i);
			
			env.reset();
			Agent agent = new Agent(env, 2, 2);
			
			QLearner.History hist = learner.new History();
			for(int step = 0; step < steps; step++) {
				int[] observations = {agent.x - 1, agent.y - 1, env.getTomatoBitmap()};
				int action = learner.getNextAction(observations);
				
				boolean wateredPlant = agent.move(Move.values()[action]);
				dryout();
				
				double reward = wateredPlant? 10.0 : -0.5;
				
				
				hist.visit(action, reward, observations);
			}
			
			hist.apply(new int[]{agent.x - 1, agent.y - 1, env.getTomatoBitmap()});
			
			//System.out.println(hist);
			
			if(i%1000 == 0) System.out.println(" reward: " + hist.getTotalReward());
		}
	}
}
