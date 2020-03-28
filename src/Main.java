import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
	
	static Environment env;
	static Agent agent;
	static RestrainingBolt bolt;
	static QLearner learner;
	static final int actionCount = 100;
	static final int learningEpochs = 100000;
	static final boolean print = false;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(6,  6, 35879879654161L);
		agent = new Agent(env, 2, 2);
		bolt = new BasicRestrainingBolt(env, agent);
		
		learner = new QLearner(4, 0.9, 0.90, 0.3, env.getWidth() - 2, env.getHeight() - 2, 1 << env.getNumberOfTomatoes(), bolt.getNumberOfStates());

		
		RewardHistories rewardHistory = learn(learningEpochs, actionCount);
		
		
		
		
		//avgRewards.plot;
		
		//Thread.sleep(5000);
		
		reset();
		for(int i = 0; i < actionCount; i++) {
			int action = learner.getBestAction(getCurrentObservations());
			agent.move(Move.values()[action]);
			bolt.applyAction(action);

			dryout();
			if (print) System.out.println(env);
			
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

	private static RewardHistories learn(int episodes, int steps) {
		List<Double> rewards = new ArrayList<>();
		List<Double> avgRewards = new ArrayList<>();
		
		RewardHistories fullHistory = new RewardHistories(episodes);
		
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0 && print) System.out.print("Episode: " + i);
			
			double totalBaseReward = 0.0;
			double totalBoltReward = 0.0;
			double totalShapingReward = 0.0;
			
			reset();
			
			QLearner.History hist = learner.new History();
			double currentPotential = getPotentialForState();
			for(int step = 0; step < steps; step++) {
				int[] observations = getCurrentObservations();
				int action = learner.getNextAction(observations);
				
				boolean wateredPlant = agent.move(Move.values()[action]);

				double baseReward = (wateredPlant? 10.0 : -0.5);
				
				double boltReward = bolt.applyAction(action);
				
				double nextPotential = getPotentialForState();
				double shapingReward = learner.discount * nextPotential - currentPotential;
				currentPotential = nextPotential;
				
				
				totalBaseReward += baseReward;
				totalBoltReward += boltReward;
				totalShapingReward += shapingReward;
				
				double reward = baseReward + boltReward + shapingReward;
				
				
				hist.visit(action, reward, observations);
				
				dryout();
			}
			
			hist.apply(getCurrentObservations());
			
			//System.out.println(hist);
			
			if(i%1000 == 0 && print) System.out.println(" reward: " + hist.getTotalReward());
			
			fullHistory.addEntry(totalBaseReward, totalBoltReward, totalShapingReward);
			
			//Compute average reward over 1000 episodes
			rewards.add(hist.getTotalReward());
			if ((i + 1) % 1000 == 0){
				double s = 0;
				for (double reward : rewards){
					s+=reward;
				}
				double avg = s / rewards.size();
				avgRewards.add(avg);
				rewards.clear();
				
			}
			
		}
		return fullHistory;
	}
	
	private static void reset() {
		env.reset();
		agent.x = 2;
		agent.y = 2;
		bolt.reset();
	}
	
	private static double getPotentialForState() {
		int minDistToUnwateredTomato = 1000000000;
		for(int x = 1; x < env.getWidth() - 1; x++) {
			for(int y = 1; y < env.getHeight() - 1; y++) {
				int dist = Math.abs(x - agent.x) + Math.abs(y - agent.y);
				if(env.getTile(x, y) == Tile.UNWATERED_TOMATO && dist < minDistToUnwateredTomato) {
					minDistToUnwateredTomato = dist;
				}
			}
		}
		return 5.0/minDistToUnwateredTomato;
	}
	
	private static class RewardHistories {
		public double[] baseRewards;
		public double[] boltRewards;
		public double[] shapingRewards;
		int curIndex = 0;
		
		public RewardHistories(int numberOfLearningEpochs) {
			baseRewards = new double[numberOfLearningEpochs];
			boltRewards = new double[numberOfLearningEpochs];
			shapingRewards = new double[numberOfLearningEpochs];
		}
		
		public void addEntry(double baseReward, double boltReward, double shapingReward) {
			baseRewards[curIndex] = baseReward;
			boltRewards[curIndex] = boltReward;
			shapingRewards[curIndex] = shapingReward;
			curIndex++;
		}
		
		public int getLength() {
			return baseRewards.length;
		}
		
		// averages
		public RewardHistories downSample(int factor) {
			int l = getLength();
			
			RewardHistories result;
			
			if(l % factor == 0) {
				result = new RewardHistories(l / factor);
			} else {
				result = new RewardHistories(l / factor + 1);
			}
			
			for(int i = 0; i < l / factor; i++) {
				double totalBase = 0.0;
				double totalBolt = 0.0;
				double totalShaping = 0.0;
				
				for(int j = 0; j < factor; j++) {
					int index = i * factor + j;
					totalBase += baseRewards[index];
					totalBolt += boltRewards[index];
					totalShaping += shapingRewards[index];
				}
				
				result.baseRewards[i] = totalBase;
				result.boltRewards[i] = totalBolt;
				result.shapingRewards[i] = totalShaping;
			}
			
			if(l % factor != 0) {
				double totalBase = 0.0;
				double totalBolt = 0.0;
				double totalShaping = 0.0;
				
				int rl = result.getLength();
				
				for(int j = 0; j < l % factor; j++) {
					int index = rl * factor + j;
					totalBase += baseRewards[index];
					totalBolt += boltRewards[index];
					totalShaping += shapingRewards[index];
				}
					
				result.baseRewards[rl] = totalBase;
				result.boltRewards[rl] = totalBolt;
				result.shapingRewards[rl] = totalShaping;
			}
			return result;
		}
	}
}