package me.shadorc.shadbot.api.pandorabots;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

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
		return this.input;
	}

	public String getResponse() {
		return StringUtils.normalizeSpace(this.response.replace("<br>", "\n"));
	}

	public String getCustId() {
		return this.custid;
	}

	public String getBotId() {
		return this.botid;
	}

	public int getStatus() {
		return this.status;
	}

	@Override
	public String toString() {
		return String.format("ChatBotResponse [input=%s, response=%s, custid=%s, botid=%s, status=%s]",
				this.input, this.response, this.custid, this.botid, this.status);
	}

}
