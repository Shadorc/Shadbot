package me.shadorc.shadbot.object.casino;

public enum Value {

	TWO(2),
	THREE(3),
	FOUR(4),
	FIVE(5),
	SIX(6),
	SEVEN(7),
	EIGHT(8),
	NINE(9),
	TEN(10),
	JACK(11, "J"),
	QUEEN(12, "Q"),
	KING(13, "K"),
	ACE(1, "A");

	private final int numeric;
	private final String ident;

	Value(int numeric, String ident) {
		this.numeric = numeric;
		this.ident = ident;
	}

	Value(int numeric) {
		this(numeric, Integer.toString(numeric));
	}

	public int getNumeric() {
		return this.numeric;
	}

	public String getIdent() {
		return this.ident;
	}

}
