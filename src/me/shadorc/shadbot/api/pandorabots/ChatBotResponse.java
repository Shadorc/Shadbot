package me.shadorc.shadbot.api.pandorabots;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.shadorc.shadbot.utils.StringUtils;

public class ChatBotResponse {

	@JsonProperty("input")
	private String input;
	@JsonProperty("that")
	private String response;
	@JsonProperty("custid")
	private String custid;
	@JsonProperty("botid")
	private String botid;
	@JsonProperty("status")
	private int status;

	public String getInput() {
		return input;
	}

	public String getResponse() {
		return StringUtils.normalizeSpace(response.replace("<br>", "\n"));
	}

	public String getCustId() {
		return custid;
	}

	public String getBotId() {
		return botid;
	}

	public int getStatus() {
		return status;
	}

}
