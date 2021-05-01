package com.shadorc.shadbot.api.json.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record R34Post(String tags,
                      @JsonProperty("file_url") String fileUrl,
                      String source,
                      int width,
                      int height,
                      @JsonProperty("has_children") boolean hasChildren) {

    public List<String> getTags() {
        return StringUtil.split(this.tags, " ");
    }

    public Optional<String> getSource() {
        return Optional.of(this.source).filter(Predicate.not(String::isBlank));
    }

}
