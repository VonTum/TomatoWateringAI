
import java.util.Random;

public class Environment {
	
	private Tile[][] tileMap;
	
	public Agent agent;
	
	public long seed;
	
	public double wetRatio;
	public double dryoutChance;
	
	public int ourTomatoesWidth;
	public int neighborTomatoesWidth;
	Random random = new Random(seed);
	
	public Environment(int width, int height, int ourTomatoesWidth, int neighborTomatoesWidth, long seed, double wetRatio, double dryoutChance) {
		this.tileMap = new Tile[width][height];
		this.seed = seed;
		this.wetRatio = wetRatio;
		this.dryoutChance = dryoutChance;
		this.ourTomatoesWidth = ourTomatoesWidth;
		this.neighborTomatoesWidth = neighborTomatoesWidth;
		
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
	
	public void reset() {
		reset(this.seed);
	}
	
	private void resetTomato(int x, int y) {
		setTile(x, y, (random.nextDouble() > wetRatio)? Tile.UNWATERED_TOMATO : Tile.WATERED_TOMATO);
	}
	
	public void reset(long seed) {
		for(int x = 1; x < ourTomatoesWidth + 1; x++) {
			resetTomato(x, 1);
			resetTomato(x, getHeight() - 2);
		}
		for(int x = getWidth() - neighborTomatoesWidth - 1; x < getWidth() - 1; x++) {
			resetTomato(x, 1);
			resetTomato(x, getHeight() - 2);
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
		int h = getHeight() - 2;
		
		return (y - 1) * h + (x - 1);
	}
	
	public int getStateForPos(Position pos) {
		return getStateForPos(pos.x, pos.y);
	}
	
	public Position getPosForState(int dfaState) {
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
	
	public void dryout() {
		Random random = new Random();
		for(int x = 1; x < getWidth() - 1; x++) {
			for(int y = 1; y < getHeight() - 1; y++) {
				if(getTile(x, y) == Tile.WATERED_TOMATO) {
					if(random.nextDouble() < dryoutChance) {
						setTile(x, y, Tile.UNWATERED_TOMATO);
					}
				}
			}
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
