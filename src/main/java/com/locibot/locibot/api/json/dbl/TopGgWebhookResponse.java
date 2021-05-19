package com.locibot.locibot.api.json.dbl;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TopGgWebhookResponse(@JsonProperty("user") String userId) {

}
