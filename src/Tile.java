
public enum Tile {
	FLOOR(' ', false),
	WALL('=', false),
	UNWATERED_TOMATO('t', true),
	WATERED_TOMATO('T', true);
	
	public final char c;
	public final boolean isTomato;
	
	private Tile(char c, boolean isTomato) {
		this.c = c;
		this.isTomato = isTomato;
	}
}
