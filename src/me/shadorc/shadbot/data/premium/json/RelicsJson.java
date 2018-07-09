package me.shadorc.shadbot.data.premium.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelicsJson {

	@JsonProperty("relics")
	private List<Relic> relics;

	public RelicsJson() {
		this.relics = new ArrayList<>();
	}

	public List<Relic> getRelics() {
		return relics;
	}

	public void addRelic(Relic relic) {
		relics.add(relic);
	}

}
