
public class Agent {
	Environment env;
	public int x, y;
	
	public Agent(Environment env, int x, int y) {
		this.env = env;
		this.x = x;
		this.y = y;
		
		env.agent = this;
	}
	
	/**
	 * @param m the move to make
	 * @return true if moving onto the tile watered the tomato
	 */
	public boolean move(Move m) {
		int newX = x + m.dx;
		int newY = y + m.dy;
		
		if(env.getTile(newX, newY) != Tile.WALL) {
			this.x = newX;
			this.y = newY;
			
			if(env.getTile(newX, newY) == Tile.UNWATERED_TOMATO) {
				env.setTile(newX, newY, Tile.WATERED_TOMATO);
				return true;
			}
		}
		return false;
	}
}
