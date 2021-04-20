package com.shadorc.shadbot.api.json.pandorabots;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtil;

public record ChatBotResult(@JsonProperty("that") String response,
                            String custid) {

    public String getResponse() {
        return StringUtil.normalizeSpace(this.response.replace("<br>", "\n"));
    }

}
