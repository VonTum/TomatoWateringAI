import java.util.Random;

public enum Move {
	UP(0,1),
	RIGHT(1,0),
	DOWN(0,-1),
	LEFT(-1,0);
	
	
	public final int dx, dy;
	
	private Move(int dx, int dy) {
		this.dx = dx;
		this.dy = dy;
	}

	private static final Random r = new Random();
	public static Move randomMove() {
		int m = r.nextInt(4);
		
		return Move.values()[m];
	}
}
