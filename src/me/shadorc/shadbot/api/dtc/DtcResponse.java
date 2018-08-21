package me.shadorc.shadbot.api.dtc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DtcResponse {

	@JsonProperty("root")
	private Root root;

	public Root getRoot() {
		return root;
	}

	@Override
	public String toString() {
		return String.format("DtcResponse [root=%s]", root);
	}

}
