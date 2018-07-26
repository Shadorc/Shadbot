package me.shadorc.shadbot.api.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatValue {

	@JsonProperty("valueInt")
	private int valueInt;
	@JsonProperty("valueDec")
	private double valueDec;

	public StatValue() {
		this.valueInt = 0;
		this.valueDec = 0;
	}

	@Override
	public String toString() {
		if(valueDec != 0) {
			return Double.toString(valueDec);
		}
		return Integer.toString(valueInt);
	}

}
