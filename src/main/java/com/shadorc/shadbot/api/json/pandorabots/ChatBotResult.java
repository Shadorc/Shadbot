package com.shadorc.shadbot.api.json.pandorabots;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtil;

public class ChatBotResult {

    @JsonProperty("that")
    private String response;
    @JsonProperty("custid")
    private String custid;

    public String getResponse() {
        return StringUtil.normalizeSpace(this.response.replace("<br>", "\n"));
    }

    public String getCustId() {
        return this.custid;
    }

}
