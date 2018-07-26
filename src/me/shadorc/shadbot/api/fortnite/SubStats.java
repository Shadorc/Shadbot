package me.shadorc.shadbot.api.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubStats {

	public static final SubStats DEFAULT = new SubStats();

	@JsonProperty("top1")
	private StatValue top1;
	@JsonProperty("kd")
	private StatValue ratio;

	public SubStats() {
		this.top1 = new StatValue();
		this.ratio = new StatValue();
	}

	public StatValue getTop1() {
		return top1;
	}

	public StatValue getRatio() {
		return ratio;
	}

	@Override
	public String toString() {
		return String.format("SubStats [top1=%s, ratio=%s]", top1, ratio);
	}

}
