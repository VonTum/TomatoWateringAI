import java.util.Random;

public class Main {
	
	static Environment env;
	static Agent agent;
	static RestrainingBolt bolt;
	static QLearner learner;
	static final int actionCount = 100;
	static final int learningEpochs = 100000;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(6,  6, 35879879654161L);
		agent = new Agent(env, 2, 2);
		bolt = new BasicRestrainingBolt(env, agent);
		
		learner = new QLearner(4, 0.9, 0.90, 0.3, env.getWidth() - 2, env.getHeight() - 2, 1 << env.getNumberOfTomatoes(), bolt.getNumberOfStates());

		
		learn(learningEpochs, actionCount);
		//System.out.println(learner);
		
		//Thread.sleep(5000);
		
		env.reset();
		agent.x = 2;
		agent.y = 2;
		bolt.reset();
		for(int i = 0; i < actionCount; i++) {
			int action = learner.getBestAction(getCurrentObservations());
			agent.move(Move.values()[action]);
			bolt.applyAction(action);
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
	
	static int[] getCurrentObservations() {
		return new int[]{agent.x - 1, agent.y - 1, env.getTomatoBitmap(), bolt.getCurrentState()};
	}
	
	private static void learn(int episodes, int steps) {
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0) System.out.print("Episode: " + i);
			
			env.reset();
			agent.x = 2;
			agent.y = 2;
			bolt.reset();
			
			QLearner.History hist = learner.new History();
			for(int step = 0; step < steps; step++) {
				int[] observations = getCurrentObservations();
				int action = learner.getNextAction(observations);
				
				boolean wateredPlant = agent.move(Move.values()[action]);
				double boltReward = bolt.applyAction(action);
				dryout();
				
				double reward = boltReward + (wateredPlant? 10.0 : -0.5);
				
				
				hist.visit(action, reward, observations);
			}
			
			hist.apply(getCurrentObservations());
			
			//System.out.println(hist);
			
			if(i%1000 == 0) System.out.println(" reward: " + hist.getTotalReward());
		}
	}
}
