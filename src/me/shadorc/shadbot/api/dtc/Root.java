package me.shadorc.shadbot.api.dtc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {

	@JsonProperty("item")
	private Quote quote;

	public Quote getQuote() {
		return this.quote;
	}

	@Override
	public String toString() {
		return String.format("Root [quote=%s]", this.quote);
	}

}
