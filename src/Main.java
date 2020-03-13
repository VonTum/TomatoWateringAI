
public class Main {
	
	
	public static void main(String[] args) throws InterruptedException {
		Environment env = new Environment(8,  6, 35879879654161L);
		
		Agent agent = new Agent(env, 2, 2);
		
		
		for(int i = 0; i < 50; i++) {
			System.out.println(env);
			
			agent.move(Move.randomMove());
			
			Thread.sleep(50);
		}
	}
}
