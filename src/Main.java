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
import org.knowm.xchart.style.markers.SeriesMarkers;

public class Main {
	
	static Environment env;
	static Agent agent;
	static ArrayList<RestrainingBolt> bolts = new ArrayList<>();
	static ArrayList<RewardShaper> rewardShapers = new ArrayList<>();
	static final int actionCount = 100;
	static final int learningEpochs = 100000;
	static final int downsampleFactor = 500;
	static final boolean randomizeStart = false;
	
	static final boolean print = false;
	
	public static void main(String[] args) throws InterruptedException {
		env = new Environment(10, 6, 3, 3, 35879879654161L, 0.3, 0.01);
		agent = new Agent(env, 2, 2);
		System.out.println(env);

		double[] indices = new double[(learningEpochs + downsampleFactor - 1) / downsampleFactor];
		for(int i = 0; i < indices.length; i++) {indices[i] = i * downsampleFactor;}
		
		double[] halfIndices = new double[(learningEpochs + downsampleFactor - 1) / downsampleFactor / 2];
		for(int i = 0; i < halfIndices.length; i++) {halfIndices[i] = i * downsampleFactor;}
		
		
		bolts.add(new AdvancedRestrainingBolt(env, agent));
		{
			QLearner learner = produceLearnerFor(0.3, 0.9, 0.3, bolts);
			{
				HistoryWithRewardBreakdown breakdown = performSteps(learner, 100);
				XYChart moveChart = produceMovementChart(breakdown.movementHistory);
				new SwingWrapper<XYChart>(moveChart).displayChart();
			}
			RewardHistories rewardHistory = learn(learner, learningEpochs, actionCount);
			
			rewardHistory.boltRewards = new double[1][rewardHistory.getLength()];
			
			{
				HistoryWithRewardBreakdown breakdown = performSteps(learner, 100);
				XYChart moveChart = produceMovementChart(breakdown.movementHistory);
				new SwingWrapper<XYChart>(moveChart).displayChart();
			}
		}
		
		// learningRate, discount, exploration
		QLearner learner = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		{
			HistoryWithRewardBreakdown breakdown = performSteps(learner, 100);
			//XYChart moveChart = produceMovementChart(breakdown.movementHistory);
			//new SwingWrapper<XYChart>(moveChart).displayChart();
		}
		RewardHistories rewardHistory = learn(learner, learningEpochs / 2, actionCount);
		
		rewardHistory.boltRewards = new double[1][rewardHistory.getLength()];
		
		{
			HistoryWithRewardBreakdown breakdown = performSteps(learner, 100);
			//XYChart moveChart = produceMovementChart(breakdown.movementHistory);
			//new SwingWrapper<XYChart>(moveChart).displayChart();
		}

		rewardShapers.add(new TomatoProximityShaper(env, agent, (dist) -> {return 5.0 / dist;}));
		
		//bolts.add(new BasicRestrainingBolt(env, agent));
		
		
		bolts.add(new AdvancedRestrainingBolt(env, agent));
		
		QLearner learnerWithBolt = learner.addObservable(bolts.get(0).getNumberOfStates());
		
		RewardHistories rewardHistoryAfterBolt = learn(learnerWithBolt, learningEpochs / 2, actionCount);
		
		RewardHistories combinedHistory = RewardHistories.join(rewardHistory, rewardHistoryAfterBolt);
		
		{
			XYChart boltRewardChart = new XYChartBuilder()
					.title("")
					.xAxisTitle("episodes")
					.yAxisTitle("average reward")
					.theme(ChartTheme.Matlab).build();
			
			boltRewardChart.addSeries("Total reward without RB", indices, downsample(combinedHistory.baseRewards, downsampleFactor));
			
			XYSeries series2 = boltRewardChart.addSeries("Total rewards with RB", indices, downsample(combinedHistory.getTotalUnshapedReward(), downsampleFactor));
			series2.setLineStyle(SeriesLines.DASH_DASH);
			series2.setLineColor(Color.ORANGE);
			series2.setFillColor(Color.ORANGE);
			series2.setMarkerColor(Color.ORANGE);
			
			XYSeries series3 = boltRewardChart.addSeries("Bolt punishments", indices, downsample(combinedHistory.boltRewards[0], downsampleFactor));
			series3.setLineStyle(SeriesLines.DOT_DOT);
			series3.setLineColor(Color.GREEN.brighter());
			series3.setFillColor(Color.GREEN.brighter());
			series3.setMarkerColor(Color.GREEN.brighter());
			
			new SwingWrapper<XYChart>(boltRewardChart).displayChart();
		}
		rewardShapers.add(new AdvancedRewardShaper(env, agent, 3, (dist) -> {return 5.0 / dist;}));

		//rewardShapers.add(new RandomRewardShaper());
		
		QLearner learner2 = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		RewardHistories rewardHistory2 = learn(learner2, learningEpochs / 2, actionCount);
		
		{
			XYChart boltRewardComparisonChart = new XYChartBuilder()
					.title("")
					.xAxisTitle("episodes")
					.yAxisTitle("average reward")
					.theme(ChartTheme.Matlab).build();
			
			boltRewardComparisonChart.addSeries("Total reward of pretrained agent", halfIndices, downsample(rewardHistoryAfterBolt.getTotalUnshapedReward(), downsampleFactor));
			
			XYSeries series2 = boltRewardComparisonChart.addSeries("Total reward of agent starting from zero", halfIndices, downsample(rewardHistory2.getTotalUnshapedReward(), downsampleFactor));
			series2.setLineStyle(SeriesLines.DASH_DASH);
			series2.setLineColor(Color.ORANGE);
			series2.setFillColor(Color.ORANGE);
			series2.setMarkerColor(Color.ORANGE);
			
			new SwingWrapper<XYChart>(boltRewardComparisonChart).displayChart();
		}
		
		{
			XYChart boltRewardComparisonChart = new XYChartBuilder()
					.title("")
					.xAxisTitle("episodes")
					.yAxisTitle("average reward")
					.theme(ChartTheme.Matlab).build();
			
			boltRewardComparisonChart.addSeries("Punishment from the restraining bolt of pretrained agent", halfIndices, downsample(rewardHistoryAfterBolt.getTotalBoltReward(), downsampleFactor));
			
			XYSeries series2 = boltRewardComparisonChart.addSeries("Punishment from the restraining bolt of agent starting from zero", halfIndices, downsample(rewardHistory2.getTotalBoltReward(), downsampleFactor));
			series2.setLineStyle(SeriesLines.DASH_DASH);
			series2.setLineColor(Color.ORANGE);
			series2.setFillColor(Color.ORANGE);
			series2.setMarkerColor(Color.ORANGE);
			
			new SwingWrapper<XYChart>(boltRewardComparisonChart).displayChart();
		}
		
		{
			HistoryWithRewardBreakdown breakdown = performSteps(learner2, 100);
			XYChart moveChart = produceMovementChart(breakdown.movementHistory);
			new SwingWrapper<XYChart>(moveChart).displayChart();
		}
		
		//rewardShapers.remove(0);
		//rewardShapers.add(new TomatoProximityShaper(env, agent, (dist) -> {return 10.0*Math.pow(2, -dist);}));

		QLearner learner3 = produceLearnerFor(0.3, 0.9, 0.5, bolts);
		RewardHistories rewardHistory3 = learn(learner3, learningEpochs, actionCount);
		
		
		/*XYChart chart = new XYChartBuilder()
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
		series3.setMarkerColor(Color.GREEN.brighter());*/
		
		{
			XYChart boltRewardChart = new XYChartBuilder()
					.title("Base reward")
					.xAxisTitle("episodes")
					.yAxisTitle("average reward")
					.theme(ChartTheme.Matlab).build();
			
			boltRewardChart.addSeries("Total reward without RB", halfIndices, downsample(rewardHistory.baseRewards, downsampleFactor));
			
			XYSeries series2 = boltRewardChart.addSeries("Total rewards with RB", indices, downsample(rewardHistory2.getTotalUnshapedReward(), downsampleFactor));
			series2.setLineStyle(SeriesLines.DASH_DASH);
			series2.setLineColor(Color.ORANGE);
			series2.setFillColor(Color.ORANGE);
			series2.setMarkerColor(Color.ORANGE);
			
			XYSeries series3 = boltRewardChart.addSeries("Bolt punishments", indices, downsample(rewardHistory3.boltRewards[0], downsampleFactor));
			series3.setLineStyle(SeriesLines.DOT_DOT);
			series3.setLineColor(Color.GREEN.brighter());
			series3.setFillColor(Color.GREEN.brighter());
			series3.setMarkerColor(Color.GREEN.brighter());
			
			new SwingWrapper<XYChart>(boltRewardChart).displayChart();
		}
		//new SwingWrapper<XYChart>(chart).displayChart();
		
		//BubbleChart bubbleChart = new BubbleChart(200, 200, ChartTheme.Matlab);
		
		//play(learner);
	}
	
	/*private static double computeAverageRewardOfLearner(QLearner learner, int epochs, int actionCount) {
		
	}*/
	
	private static XYChart produceMovementChart(MovementHistory h) {
		int l = h.xPositions.length;
		
		double[] xChartValues = new double[l];
		double[] yChartValues = new double[l];
		double[] xWateredPositions = new double[h.getWateredCount()];
		double[] yWateredPositions = new double[h.getWateredCount()];
		int curWateredPosition = 0;
		for(int i = 0; i < l; i++) {
			double progress = ((double) i) / l;
			double xPos = h.xPositions[i] + 0.05 + 0.90 * progress;
			double yPos = h.yPositions[i] + 0.95 - 0.90 * progress;
			xChartValues[i] = xPos;
			yChartValues[i] = yPos;
			
			if(h.wateredActions[i]) {
				xWateredPositions[curWateredPosition] = xPos;
				yWateredPositions[curWateredPosition] = yPos;
				curWateredPosition++;
			}
		}
		
		double[] xChartBeforeBolt = getBeforeIndex(xChartValues, h.stepAtWhichBoltSwitches);
		double[] xChartAfterBolt = getAfterIndex(xChartValues, h.stepAtWhichBoltSwitches);
		double[] yChartBeforeBolt = getBeforeIndex(yChartValues, h.stepAtWhichBoltSwitches);
		double[] yChartAfterBolt = getAfterIndex(yChartValues, h.stepAtWhichBoltSwitches);
		
		XYChart chart = new XYChartBuilder()
				.title("")
				.xAxisTitle("x position")
				.yAxisTitle("y position")
				.theme(ChartTheme.Matlab).build();
		
		if(h.stepAtWhichBoltSwitches != 0) {
			XYSeries movementSeries = chart.addSeries("Movement before bolt switch", xChartBeforeBolt, yChartBeforeBolt);
			movementSeries.setMarker(SeriesMarkers.NONE);
		}
		if(h.stepAtWhichBoltSwitches != l) {
			XYSeries movementSeries2 = chart.addSeries("Movement after bolt switch", xChartAfterBolt, yChartAfterBolt);
			movementSeries2.setMarker(SeriesMarkers.NONE);
		}
		XYSeries waterSeries = chart.addSeries("Watering", xWateredPositions, yWateredPositions);
		
		waterSeries.setLineStyle(SeriesLines.NONE);
		waterSeries.setMarker(SeriesMarkers.TRIANGLE_UP);
		waterSeries.setMarkerColor(Color.RED);
		
		chart.getStyler().setXAxisMin(0.0).setXAxisMax(env.getWidth() + 0.0);
		chart.getStyler().setYAxisMin(0.0).setYAxisMax(env.getHeight() + 0.0);
		
		return chart;
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
	
	private static double[] getBeforeIndex(double[] data, int indexToSplitAt) {
		double[] resultLeft = new double[indexToSplitAt];
		
		for(int i = 0; i < indexToSplitAt; i++) {
			resultLeft[i] = data[i];
		}
		
		return resultLeft;
	}
	
	private static double[] getAfterIndex(double[] data, int indexToSplitAt) {
		double[] resultRight = new double[data.length - indexToSplitAt];
		
		for(int i = indexToSplitAt; i < data.length; i++) {
			resultRight[i - indexToSplitAt] = data[i];
		}
		
		return resultRight;
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
		
		return new RewardBreakdown(observations, action, baseReward, boltRewards, shapingRewards, wateredPlant);
	}
	
	static private class HistoryWithRewardBreakdown{
		public double totalBaseReward;
		public double[] totalBoltRewards;
		public double[] totalShapingRewards;
		public QLearner.History hist;
		MovementHistory movementHistory;
		
		public HistoryWithRewardBreakdown(double totalBaseReward, double[] totalBoltRewards, double[] totalShapingRewards, QLearner.History hist, MovementHistory movementHistory) {
			super();
			this.totalBaseReward = totalBaseReward;
			this.totalBoltRewards = totalBoltRewards;
			this.totalShapingRewards = totalShapingRewards;
			this.hist = hist;
			this.movementHistory = movementHistory;
		}
	}
	
	private static HistoryWithRewardBreakdown performSteps(QLearner learner, int steps) {
		double totalBaseReward = 0.0;
		double[] totalBoltRewards = new double[bolts.size()];
		double[] totalShapingRewards = new double[rewardShapers.size()];
		
		int stepAtWhichBoltSwitches = steps;
		
		for(RestrainingBolt b : bolts) {
			b.reset();
		}
		
		QLearner.History hist = learner.new History();
		MovementHistory movementHist = new MovementHistory(steps, agent.x, agent.y);
		double[] currentPotentials = getPotentialsForState();
		for(int step = 0; step < steps; step++) {
			
			if(bolts.size() >= 1 && bolts.get(0).getCurrentState() == 1 && stepAtWhichBoltSwitches > step) {
				stepAtWhichBoltSwitches = step;
			}
			
			RewardBreakdown breakdown = computeAndApplyAction(learner, currentPotentials);
			
			hist.visit(breakdown.action, breakdown.getTotalReward(), breakdown.observations);
			movementHist.visit(agent.x, agent.y, breakdown.wateredPlant);
			
			totalBaseReward += breakdown.baseReward;
			addInto(totalBoltRewards, breakdown.boltRewards);
			addInto(totalShapingRewards, breakdown.shapingRewards);
			
			env.dryout();
		}
		
		if(stepAtWhichBoltSwitches != 100) {
			System.out.println("beep" + stepAtWhichBoltSwitches);
		}
		
		movementHist.stepAtWhichBoltSwitches = stepAtWhichBoltSwitches;
		
		return new HistoryWithRewardBreakdown(totalBaseReward, totalBoltRewards, totalShapingRewards, hist, movementHist);
	}
	
	private static RewardHistories learn(QLearner learner, int episodes, int steps) {
		RewardHistories fullHistory = new RewardHistories(episodes, bolts.size(), rewardShapers.size());
		
		/*for(int i = 0; i < downsampleFactor; i++) {
			if(i%1000 == 0 && print) System.out.print("Episode: " + i);
			
			reset();
			
			HistoryWithRewardBreakdown hist = performSteps(learner, steps);
			
			if(i%1000 == 0 && print) System.out.println(" reward: " + hist.hist.getTotalReward());

			fullHistory.addEntry(hist.totalBaseReward, hist.totalBoltRewards, hist.totalShapingRewards);
		}*/
		
		for(int i = 0; i < episodes; i++) {
			if(i%1000 == 0 && print) System.out.print("Episode: " + i);
			
			reset();
			
			HistoryWithRewardBreakdown hist = performSteps(learner, steps);
			
			if(i%1000 == 0 && print) System.out.println(" reward: " + hist.hist.getTotalReward());

			hist.hist.apply(getCurrentObservations());
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
		
		public static RewardHistories join(RewardHistories... histories) {
			int totalNumberOfEpochs = 0;
			for(RewardHistories rh : histories) {
				totalNumberOfEpochs += rh.getLength();
			}
			
			RewardHistories result = new RewardHistories(totalNumberOfEpochs, histories[0].boltRewards.length, histories[0].shapingRewards.length);
			
			for(RewardHistories rh : histories) {
				for(int i = 0; i < rh.getLength(); i++) {
					result.addEntry(rh.getBaseRewardAt(i), rh.getBoltRewardsAt(i), rh.getShapingRewardsAt(i));
				}
			}
			
			return result;
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
		
		double getBaseRewardAt(int epoch) {
			return baseRewards[epoch];
		}
		
		double[] getBoltRewardsAt(int epoch) {
			double[] result = new double[boltRewards.length];
			for(int i = 0; i < result.length; i++) {
				result[i] = boltRewards[i][epoch];
			}
			return result;
		}
		double[] getShapingRewardsAt(int epoch) {
			double[] result = new double[shapingRewards.length];
			for(int i = 0; i < result.length; i++) {
				result[i] = shapingRewards[i][epoch];
			}
			return result;
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
		public boolean wateredPlant;
		
		
		public RewardBreakdown(int[] observations, int action, double baseReward, double[] boltRewards, double[] shapingRewards, boolean wateredPlant) {
			this.observations = observations;
			this.action = action;
			this.baseReward = baseReward;
			this.boltRewards = boltRewards;
			this.shapingRewards = shapingRewards;
			this.wateredPlant = wateredPlant;
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
	
	public static class MovementHistory{
		int[] xPositions;
		int[] yPositions;
		boolean[] wateredActions;
		int stepAtWhichBoltSwitches;
		
		int curIndex = 1;
		
		public MovementHistory(int stepCount, int startX, int startY) {
			this.xPositions = new int[stepCount+1];
			this.yPositions = new int[stepCount+1];
			this.wateredActions = new boolean[stepCount+1];
			
			xPositions[0] = startX;
			yPositions[0] = startY;
			wateredActions[0] = false;
		}
		
		void visit(int x, int y, boolean watered) {
			xPositions[curIndex] = x;
			yPositions[curIndex] = y;
			wateredActions[curIndex] = watered;
			curIndex++;
		}
		
		int getWateredCount() {
			int total = 0;
			for(int i = 0; i < wateredActions.length; i++) {
				if(wateredActions[i]) total++;
			}
			return total;
		}
	}
}