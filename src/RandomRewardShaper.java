import java.util.Random;

public class RandomRewardShaper implements RewardShaper {
	Random rand = new Random();
	
	
	@Override
	public double getPotential() {
		return rand.nextDouble() * 20.0 - 10.0;
	}
}
