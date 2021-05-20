package com.locibot.locibot.core.command;

import com.locibot.locibot.database.guilds.entity.DBGuild;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.Member;

public class PrivateContext extends Context{
    public PrivateContext(InteractionCreateEvent event) {
        super(event, null);
    }

    public Member getAuthor() {
        return this.getEvent().getInteraction().getMember().orElseThrow();
    }

    public boolean isPrivate(){
        return true;
    }

}
