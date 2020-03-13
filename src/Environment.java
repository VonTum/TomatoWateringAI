
import java.util.Random;

public class Environment {
	
	private Tile[][] tileMap;
	
	public Agent agent;
	
	public Environment(int width, int height, long seed) {
		this.tileMap = new Tile[width][height];
		
		Random random = new Random(seed);
		
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
		
		for(int x = 1; x < width - 1; x++) {
			setTile(x, 1, random.nextBoolean()? Tile.UNWATERED_TOMATO : Tile.WATERED_TOMATO);
			setTile(x, height - 2, random.nextBoolean()? Tile.UNWATERED_TOMATO : Tile.WATERED_TOMATO);
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
	
	public Position[] getAllTomatoes() {
		Position[] result = new Position[getNumberOfTomatoes()];
		
		int count = 0;
		for(int x = 0; x < getWidth(); x++) {
			for(int y = 0; y < getHeight(); y++) {
				if(getTile(x, y).isTomato) {
					count++;
					result[count] = new Position(x, y);
				}
			}
		}
		
		return result;
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
