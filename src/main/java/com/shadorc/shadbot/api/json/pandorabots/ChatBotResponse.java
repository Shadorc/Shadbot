package com.shadorc.shadbot.api.json.pandorabots;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

public class ChatBotResponse {

    @JsonProperty("that")
    private String response;
    @JsonProperty("custid")
    private String custid;

    public String getResponse() {
        return StringUtils.normalizeSpace(this.response.replace("<br>", "\n"));
    }

    public String getCustId() {
        return this.custid;
    }

}
