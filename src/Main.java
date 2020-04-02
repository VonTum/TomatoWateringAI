import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.lines.SeriesLines;

public class Main {
	
	static Environment env;
	static Agent agent;
	static ArrayList<RestrainingBolt> bolts = new ArrayList<>();
	static ArrayList<RewardShaper> rewardShapers = new ArrayList<>();
	static final int actionCount = 100;
	static final int learningEpochs = 20000;
	static final int downsampleFactor = 50;
	static final boolean randomizeStart = false;
	
	static final boolean print = false;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(10, 6, 4, 2, 35879879654161L, 0.5, 0.01);
		agent = new Agent(env, 2, 2);
		System.out.println(env);
		
		// learningRate, discount, exploration
		QLearner learner = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		RewardHistories rewardHistory = learn(learner, learningEpochs, actionCount);
		
		rewardShapers.add(new TomatoProximityShaper(env, agent, (dist) -> {return 5.0 / dist;}));
		//bolts.add(new BasicRestrainingBolt(env, agent));
		bolts.add(new AdvancedRestrainingBolt(env, agent));

		//rewardShapers.add(new RandomRewardShaper());
		
		QLearner learner2 = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		RewardHistories rewardHistory2 = learn(learner2, learningEpochs, actionCount);
		
		rewardShapers.remove(0);
		rewardShapers.add(new TomatoProximityShaper(env, agent, (dist) -> {return 10.0*Math.pow(2, -dist);}));

		QLearner learner3 = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		RewardHistories rewardHistory3 = learn(learner3, learningEpochs, actionCount);
		
		double[] indices = new double[(learningEpochs + downsampleFactor - 1) / downsampleFactor];
		for(int i = 0; i < indices.length; i++) {indices[i] = i * downsampleFactor;}
		
		XYChart chart = new XYChartBuilder()
				.title("Base reward")
				.xAxisTitle("episodes")
				.yAxisTitle("average reward")
				.theme(ChartTheme.Matlab).build();
		
		chart.addSeries("Base reward", indices, downsample(rewardHistory.baseRewards, downsampleFactor));
		
		XYSeries series2 = chart.addSeries("Shaped with 5.0/dist", indices, downsample(rewardHistory2.baseRewards, downsampleFactor));
		series2.setLineStyle(SeriesLines.DASH_DASH);
		series2.setLineColor(Color.ORANGE);
		series2.setFillColor(Color.ORANGE);
		series2.setMarkerColor(Color.ORANGE);
		
		XYSeries series3 = chart.addSeries("Shaped with 10.0 * 2^-dist", indices, downsample(rewardHistory3.baseRewards, downsampleFactor));
		series3.setLineStyle(SeriesLines.DOT_DOT);
		series3.setLineColor(Color.GREEN.brighter());
		series3.setFillColor(Color.GREEN.brighter());
		series3.setMarkerColor(Color.GREEN.brighter());
		
		new SwingWrapper<XYChart>(chart).displayChart();
		
		//BubbleChart bubbleChart = new BubbleChart(200, 200, ChartTheme.Matlab);
		
		play(learner);
	}
	
	/*private static double computeAverageRewardOfLearner(QLearner learner, int epochs, int actionCount) {
		
	}*/
	
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
			System.out.println(env);
			env.dryout();
			if (print) System.out.println(env);
			
			Thread.sleep(500);
		}
		//total reward?
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

	private static RewardBreakdown computeAndApplyAction(QLearner learner, double[] currentPotentials) {
		int[] observations = getCurrentObservations();
		int action = learner.getNextAction(observations);
		
		boolean wateredPlant = agent.move(Move.values()[action]);

		double baseReward = (wateredPlant? 10.0 : -0.5);
		
		double[] boltRewards = new double[bolts.size()];
		for(int i = 0; i < boltRewards.length; i++) {
			boltRewards[i] = bolts.get(i).applyAction(action);
		}
		double[] shapingRewards = new double[rewardShapers.size()];
		double[] nextPotentials = getPotentialsForState();
		for(int i = 0; i < shapingRewards.length; i++) {
			shapingRewards[i] = learner.discount * nextPotentials[i] - currentPotentials[i];
			currentPotentials[i] = nextPotentials[i];
		}
		
		env.dryout();
		
		return new RewardBreakdown(observations, action, baseReward, boltRewards, shapingRewards);
	}
	
	static private class HistoryWithRewardBreakdown{
		public double totalBaseReward;
		public double[] totalBoltRewards;
		public double[] totalShapingRewards;
		public QLearner.History hist;
		
		public HistoryWithRewardBreakdown(double totalBaseReward, double[] totalBoltRewards, double[] totalShapingRewards, QLearner.History hist) {
			super();
			this.totalBaseReward = totalBaseReward;
			this.totalBoltRewards = totalBoltRewards;
			this.totalShapingRewards = totalShapingRewards;
			this.hist = hist;
		}
	}
	
	private static HistoryWithRewardBreakdown performSteps(QLearner learner, int steps) {
		double totalBaseReward = 0.0;
		double[] totalBoltRewards = new double[bolts.size()];
		double[] totalShapingRewards = new double[rewardShapers.size()];
		
		QLearner.History hist = learner.new History();
		double[] currentPotentials = getPotentialsForState();
		for(int step = 0; step < steps; step++) {
			
			RewardBreakdown breakdown = computeAndApplyAction(learner, currentPotentials);
			
			hist.visit(breakdown.action, breakdown.getTotalReward(), breakdown.observations);
			
			totalBaseReward += breakdown.baseReward;
			addInto(totalBoltRewards, breakdown.boltRewards);
			addInto(totalShapingRewards, breakdown.shapingRewards);
			
			env.dryout();
		}
		
		hist.apply(getCurrentObservations());
		return new HistoryWithRewardBreakdown(totalBaseReward, totalBoltRewards, totalShapingRewards, hist);
	}
	
	private static RewardHistories learn(QLearner learner, int episodes, int steps) {
		RewardHistories fullHistory = new RewardHistories(episodes, bolts.size(), rewardShapers.size());
		
		
		
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0 && print) System.out.print("Episode: " + i);
			
			reset();
			
			HistoryWithRewardBreakdown hist = performSteps(learner, steps);
			
			//System.out.println(hist);
			
			if(i%1000 == 0 && print) System.out.println(" reward: " + hist.hist.getTotalReward());
			
			fullHistory.addEntry(hist.totalBaseReward, hist.totalBoltRewards, hist.totalShapingRewards);
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
		if(randomizeStart) {
			Random random = new Random();
			env.reset(random.nextLong());
			agent.x = 1 + random.nextInt(env.getWidth() - 2);
			agent.y = 1 + random.nextInt(env.getHeight() - 2);
		}else {
			env.reset();
			agent.x = 2;
			agent.y = 2;
		}
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
		
		public double[] getTotalUnshapedReward() {
			double[] totalBoltReward = getTotalBoltReward();
			double[] result = new double[getLength()];
			
			for(int i = 0; i < result.length; i++) {
				result[i] = totalBoltReward[i] + baseRewards[i];
			}
			return result;
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
	
	static void addInto(double[] first, double[] second) {
		for(int i = 0; i < first.length; i++) {
			first[i] += second[i];
		}
	}
	
	static double sum(double[] arr) {
		double total = 0.0;
		for(int i = 0; i < arr.length; i++) {
			total += arr[i];
		}
		return total;
	}
	
	public static class RewardBreakdown {
		public int[] observations;
		public int action;
		public double baseReward;
		public double[] boltRewards;
		public double[] shapingRewards;
		
		
		public RewardBreakdown(int[] observations, int action, double baseReward, double[] boltRewards, double[] shapingRewards) {
			this.observations = observations;
			this.action = action;
			this.baseReward = baseReward;
			this.boltRewards = boltRewards;
			this.shapingRewards = shapingRewards;
		}
		
		public double getTotalNonShapingReward() {
			return baseReward + sum(boltRewards);
		}
		
		public double getTotalReward() {
			return baseReward + sum(boltRewards) + sum(shapingRewards);
		}
		
		public void add(RewardBreakdown other) {
			this.baseReward += other.baseReward;
			addInto(this.boltRewards, other.boltRewards);
			addInto(this.shapingRewards, other.shapingRewards);
		}
	}
}