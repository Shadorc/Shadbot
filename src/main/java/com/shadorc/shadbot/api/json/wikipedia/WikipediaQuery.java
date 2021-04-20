package com.shadorc.shadbot.api.json.wikipedia;

import java.util.Map;

public record WikipediaQuery(Map<String, WikipediaPage> pages) {

}
