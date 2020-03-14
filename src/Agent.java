
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
		Position newPos = env.getPositionWhenMoving(x, y, m);
		
		this.x = newPos.x;
		this.y = newPos.y;
		
		if(env.getTile(newPos.x, newPos.y) == Tile.UNWATERED_TOMATO) {
			env.setTile(newPos.x, newPos.y, Tile.WATERED_TOMATO);
			return true;
		}
		
		return false;
	}
}
