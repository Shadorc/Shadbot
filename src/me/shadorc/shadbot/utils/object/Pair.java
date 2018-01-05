package me.shadorc.shadbot.utils.object;

public class Pair<L, R> {

	private L first;
	private R second;

	public Pair(L first, R second) {
		this.first = first;
		this.second = second;
	}

	public void setFirst(L first) {
		this.first = first;
	}

	public void setSecond(R second) {
		this.second = second;
	}

	public L getFirst() {
		return first;
	}

	public R getSecond() {
		return second;
	}
}
