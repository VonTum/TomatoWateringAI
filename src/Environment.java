
import java.util.Random;

public class Environment {
	
	private Tile[][] tileMap;
	
	public Agent agent;
	
	public long seed;
	
	public Environment(int width, int height, long seed) {
		this.tileMap = new Tile[width][height];
		this.seed = seed;
		
		for(int x = 1; x < width - 1; x++) {
			for(int y = 1; y < height - 1; y++) {
				setTile(x, y, Tile.FLOOR);
			}
		}
		
		for(int x = 0; x < width; x++) {
			setTile(x, 0, Tile.WALL);
			setTile(x, height-1, Tile.WALL);
		}
		
		for(int y = 0; y < height; y++) {
			setTile(0, y, Tile.WALL);
			setTile(width - 1, y, Tile.WALL);
		}
		
		reset();
	}
	
	private static boolean nextBool(Random random) {
		return random.nextDouble() > 0.2;
	}
	
	public void reset() {
		Random random = new Random(seed);
		
		for(int x = 1; x < getWidth() - 1; x++) {
			setTile(x, 1, nextBool(random)? Tile.UNWATERED_TOMATO : Tile.WATERED_TOMATO);
			setTile(x, getHeight() - 2, nextBool(random)? Tile.UNWATERED_TOMATO : Tile.WATERED_TOMATO);
		}
	}
	
	public Tile getTile(int x, int y) {
		return tileMap[x][y];
	}
	
	public void setTile(int x, int y, Tile newTile) {
		tileMap[x][y] = newTile;
	}
	
	public int getWidth() {
		return tileMap.length;
	}
	
	public int getHeight() {
		return tileMap[0].length;
	}
	
	public int getNumberOfTomatoes() {
		int count = 0;
		for(int x = 0; x < getWidth(); x++) {
			for(int y = 0; y < getHeight(); y++) {
				if(getTile(x, y).isTomato) {
					count++;
				}
			}
		}
		return count;
	}
	
	public int getTomatoBitmap() {
		int totalBitmap = 0;
		for(int x = 1; x < getWidth() - 1; x++) {
			for(int y = 1; y < getHeight() - 1; y++) {
				Tile t = getTile(x, y);
				switch(t) {
				case UNWATERED_TOMATO:
					totalBitmap = (totalBitmap << 1) + 1;
					break;
				case WATERED_TOMATO:
					totalBitmap = (totalBitmap << 1);
					break;
				default:
					break;
				}
			}
		}
		return totalBitmap;
	}
	
	public int getStateForPos(int x, int y) {
		int w = getWidth() - 2;
		int h = getHeight() - 2;
		
		return (y - 1) * h + (x - 1);
	}
	
	public int getStateForPos(Position pos) {
		return getStateForPos(pos.x, pos.y);
	}
	
	public Position getPosForState(int dfaState) {
		int w = getWidth() - 2;
		int h = getHeight() - 2;
		
		int x = dfaState % h;
		int y = (dfaState - x) / h;
		
		return new Position(x + 1, y + 1);
	}
	
	public Position getPositionWhenMoving(int curX, int curY, Move move) {
		int newX = curX + move.dx;
		int newY = curY + move.dy;
		
		if(getTile(newX, newY) == Tile.WALL) {
			return new Position(curX, curY);
		} else {
			return new Position(newX, newY);
		}
	}
	
	public DFASystem getMovementDFA() {
		int w = getWidth();
		int h = getHeight();
		
		DFASystem result = new DFASystem((w - 2) * (h - 2), 4);
		
		for(int x = 1; x < w - 1; x++) {
			for(int y = 1; y < h - 1; y++) {
				int state = getStateForPos(x, y);
				for(Move m : Move.values()) {
					result.setNextState(state, m.ordinal(), getStateForPos(getPositionWhenMoving(x, y, m)));
				}
			}
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		int agentX = -1;
		int agentY = -1;
		if(agent != null) {
			agentX = agent.x;
			agentY = agent.y;
		}
		
		
		for(int y = 0; y < getHeight(); y++) {
			for(int x = 0; x < getWidth(); x++) {
				if(x == agentX && y == agentY) {
					result.append("A ");
				} else {
					result.append(getTile(x, y).c).append(' ');
				}
			}
			result.append('\n');
		}
		
		return result.toString();
	}
}
