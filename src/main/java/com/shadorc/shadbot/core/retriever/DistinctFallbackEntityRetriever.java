package com.shadorc.shadbot.core.retriever;

import discord4j.core.object.entity.Member;
import discord4j.core.retriever.EntityRetriever;
import discord4j.core.retriever.FallbackEntityRetriever;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Flux;

public class DistinctFallbackEntityRetriever extends FallbackEntityRetriever {

    public DistinctFallbackEntityRetriever(EntityRetriever first, EntityRetriever fallback) {
        super(first, fallback);
    }

    @Override
    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return super.getGuildMembers(guildId).distinct();
    }
}
