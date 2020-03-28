import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

public class Main {
	
	static Environment env;
	static Agent agent;
	static ArrayList<RestrainingBolt> bolts = new ArrayList<>();
	static ArrayList<RewardShaper> rewardShapers = new ArrayList<>();
	static final int actionCount = 100;
	static final int learningEpochs = 100000;
	static final int downsampleFactor = 200;
	static final boolean doDryout = true;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(6,  6, 35879879654161L);
		agent = new Agent(env, 2, 2);
		
		
		// learningRate, discount, exploration
		QLearner learner = produceLearnerFor(0.3, 0.9, 0.3, bolts);
		RewardHistories rewardHistory = learn(learner, learningEpochs, actionCount);
		
		rewardShapers.add(new TomatoProximityShaper(env, agent));
		//bolts.add(new BasicRestrainingBolt(env, agent));
		
		//rewardShapers.add(new RandomRewardShaper());
		
		QLearner learner2 = produceLearnerFor(0.3, 0.9, 0.3, bolts);
		RewardHistories rewardHistory2 = learn(learner2, learningEpochs, actionCount);
		
		XYChart chart = produceChartFor(new double[][] {rewardHistory.baseRewards, rewardHistory2.baseRewards}, new String[] {"No Shaping", "Shaping"});
		JFrame chartFrame = new SwingWrapper<XYChart>(chart).displayChart();
		
		//avgRewards.plot;
		
		//play(learner)
	}
	
	private static QLearner produceLearnerFor(double learningRate, double discount, double exploration, List<RestrainingBolt> bolts) {
		int[] observationSizes = new int[bolts.size() + 3];
		observationSizes[0] = env.getWidth() - 2;
		observationSizes[1] = env.getHeight() - 2;
		observationSizes[2] = 1 << env.getNumberOfTomatoes();
		
		for(int i = 0; i < bolts.size(); i++) {
			observationSizes[i+3] = bolts.get(i).getNumberOfStates();
		}
		
		return new QLearner(4, learningRate, discount, exploration, observationSizes);
	}
	
	public static XYChart produceChartFor(double[][] histories, String[] labels) {
		double[] h0 = downsample(histories[0], downsampleFactor);
		double[] indices = new double[h0.length];
		for(int i = 0; i < indices.length; i++) {
			indices[i] = i * downsampleFactor;
		}
		XYChart chart = QuickChart.getChart("testChart", "episodes", "average reward", labels[0], indices, h0);
		for(int i = 1; i < histories.length; i++) {
			chart.addSeries(labels[i], indices, downsample(histories[i], downsampleFactor));
		}
		return chart;
	}
	
	private static double[] downsample(double[] data, int factor) {
		double[] result;
		
		int l = data.length;
		
		if(l % factor == 0) {
			result = new double[l / factor];
		} else {
			result = new double[l / factor + 1];
		}
		
		for(int i = 0; i < l / factor; i++) {
			double total = 0.0;
			
			for(int j = 0; j < factor; j++) {
				int index = i * factor + j;
				total += data[index];
			}
			
			result[i] = total / factor;
		}
		
		int leftOver = l % factor;
		
		if(leftOver != 0) {
			double total = 0.0;
			
			int rl = result.length;
			
			for(int j = 0; j < leftOver; j++) {
				int index = rl * factor + j;
				total += data[index];
			}
			
			result[rl] = total / leftOver;
		}
		return result;
	}
	
	private static double[][] downsample(double[][] datas, int downsampleFactor){
		double[][] results = new double[datas.length][];
		
		for(int i = 0; i < datas.length; i++) {
			results[i] = downsample(datas[i], downsampleFactor);
		}
		
		return results;
	}
	
	private static void play(QLearner learner) throws InterruptedException {
		reset();
		for(int i = 0; i < actionCount; i++) {
			int action = learner.getBestAction(getCurrentObservations());
			agent.move(Move.values()[action]);
			for(RestrainingBolt bolt : bolts) {
				bolt.applyAction(action);
			}
			if(doDryout) dryout();
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
		int[] observations = new int[bolts.size() + 3];
		observations[0] = agent.x - 1;
		observations[1] = agent.y - 1;
		observations[2] = env.getTomatoBitmap();
		for(int i = 0; i < bolts.size(); i++) {
			observations[i+3] = bolts.get(i).getCurrentState();
		}
		
		return observations;
	}

	private static RewardHistories learn(QLearner learner, int episodes, int steps) {
		RewardHistories fullHistory = new RewardHistories(episodes, bolts.size(), rewardShapers.size());
		
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0) System.out.print("Episode: " + i);
			
			reset();
			
			
			double totalBaseReward = 0.0;
			double[] totalBoltRewards = new double[bolts.size()];
			double[] totalShapingRewards = new double[rewardShapers.size()];
			
			QLearner.History hist = learner.new History();
			double[] currentPotentials = getPotentialsForState();
			for(int step = 0; step < steps; step++) {
				int[] observations = getCurrentObservations();
				int action = learner.getNextAction(observations);
				
				boolean wateredPlant = agent.move(Move.values()[action]);

				double baseReward = (wateredPlant? 10.0 : -0.5);
				totalBaseReward += baseReward;
				
				double reward = baseReward;
				
				for(int bi = 0; bi < bolts.size(); bi++) {
					double boltReward = bolts.get(bi).applyAction(action);
					totalBoltRewards[bi] += boltReward;
					reward += boltReward;
				}
				
				double[] nextPotentials = getPotentialsForState();
				
				for(int si = 0; si < rewardShapers.size(); si++) {
					double shapingReward = learner.discount * nextPotentials[si] - currentPotentials[si];
					totalShapingRewards[si] = shapingReward;
					reward += shapingReward;
				}
				currentPotentials = nextPotentials;
				
				hist.visit(action, reward, observations);
				
				if(doDryout) dryout();
			}
			
			hist.apply(getCurrentObservations());
			
			//System.out.println(hist);
			
			if(i%1000 == 0) System.out.println(" reward: " + hist.getTotalReward());
			
			fullHistory.addEntry(totalBaseReward, totalBoltRewards, totalShapingRewards);
			
		}
		return fullHistory;
	}
	
	private static double[] getPotentialsForState() {
		double[] potentials = new double[rewardShapers.size()];
		for(int i = 0; i < rewardShapers.size(); i++) { 
			potentials[i] = rewardShapers.get(i).getPotential();
		}
		return potentials;
	}
	
	private static void reset() {
		env.reset();
		agent.x = 2;
		agent.y = 2;
		for(RestrainingBolt bolt : bolts) {
			bolt.reset();
		}
	}
	
	private static class RewardHistories {
		public double[] baseRewards;
		public double[][] boltRewards;
		public double[][] shapingRewards;
		int curIndex = 0;
		
		public RewardHistories(int numberOfLearningEpochs, int numberOfBolts, int numberOfShapings) {
			this.baseRewards = new double[numberOfLearningEpochs];
			this.boltRewards = new double[numberOfBolts][numberOfLearningEpochs];
			this.shapingRewards = new double[numberOfShapings][numberOfLearningEpochs];
		}
		
		private RewardHistories(double[] baseRewards, double[][] boltRewards, double[][] shapingRewards) {
			this.baseRewards = baseRewards;
			this.boltRewards = boltRewards;
			this.shapingRewards = shapingRewards;
		}
		
		public void addEntry(double baseReward, double[] boltRewards, double[] shapingRewards) {
			this.baseRewards[curIndex] = baseReward;
			for(int i = 0; i < boltRewards.length; i++) {
				this.boltRewards[i][curIndex] = boltRewards[i];				
			}
			for(int i = 0; i < shapingRewards.length; i++) {
				this.shapingRewards[i][curIndex] = shapingRewards[i];				
			}
			curIndex++;
		}
		
		public int getLength() {
			return baseRewards.length;
		}
		
		public double[] getTotalBoltReward() {
			double[] result = new double[getLength()];
			
			for(int i = 0; i < result.length; i++) {
				double total = 0.0;
				for(int j = 0; j < boltRewards.length; j++) {
					total += boltRewards[j][i];
				}
				result[i] = total;
			}
			return result;
		}
		
		public double[] getTotalShapingReward() {
			double[] result = new double[getLength()];
			
			for(int i = 0; i < result.length; i++) {
				double total = 0.0;
				for(int j = 0; j < shapingRewards.length; j++) {
					total += shapingRewards[j][i];
				}
				result[i] = total;
			}
			return result;
		}
		
		// averages
		public RewardHistories downSample(int factor) {
			return new RewardHistories(
					downsample(this.baseRewards, factor), 
					downsample(this.boltRewards, factor), 
					downsample(this.shapingRewards, factor)
			);
		}
	}
}