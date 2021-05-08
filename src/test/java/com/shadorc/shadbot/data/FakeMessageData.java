package com.shadorc.shadbot.data;

import discord4j.discordjson.Id;
import discord4j.discordjson.json.*;
import discord4j.discordjson.possible.Possible;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FakeMessageData implements MessageData {

    private final String content;
    private final List<EmbedData> embeds;

    public FakeMessageData(String content, List<EmbedData> embeds) {
        this.content = content;
        this.embeds = embeds;
    }

    @Override
    public Id id() {
        return Id.of(1234);
    }

    @Override
    public Id channelId() {
        return Id.of(1234);
    }

    @Override
    public Possible<Id> guildId() {
        return Possible.absent();
    }

    @Override
    public UserData author() {
        return null;
    }

    @Override
    public Possible<PartialMemberData> member() {
        return Possible.absent();
    }

    @Override
    public String content() {
        return this.content;
    }

    @Override
    public String timestamp() {
        return "";
    }

    @Override
    public Optional<String> editedTimestamp() {
        return Optional.empty();
    }

    @Override
    public boolean tts() {
        return false;
    }

    @Override
    public boolean mentionEveryone() {
        return false;
    }

    @Override
    public List<UserWithMemberData> mentions() {
        return Collections.emptyList();
    }

    @Override
    public List<String> mentionRoles() {
        return Collections.emptyList();
    }

    @Override
    public Possible<List<ChannelMentionData>> mentionChannels() {
        return Possible.absent();
    }

    @Override
    public List<AttachmentData> attachments() {
        return Collections.emptyList();
    }

    @Override
    public List<EmbedData> embeds() {
        return this.embeds;
    }

    @Override
    public Possible<List<ReactionData>> reactions() {
        return Possible.absent();
    }

    @Override
    public Possible<Object> nonce() {
        return Possible.absent();
    }

    @Override
    public boolean pinned() {
        return false;
    }

    @Override
    public Possible<Id> webhookId() {
        return Possible.absent();
    }

    @Override
    public int type() {
        return 0;
    }

    @Override
    public Possible<MessageActivityData> activity() {
        return Possible.absent();
    }

    @Override
    public Possible<MessageApplicationData> application() {
        return Possible.absent();
    }

    @Override
    public Possible<MessageReferenceData> messageReference() {
        return Possible.absent();
    }

    @Override
    public Possible<Integer> flags() {
        return Possible.absent();
    }

    @Override
    public Possible<List<StickerData>> stickers() {
        return Possible.absent();
    }

    @Override
    public Possible<Optional<MessageData>> referencedMessage() {
        return Possible.absent();
    }

    @Override
    public Possible<MessageInteractionData> interaction() {
        return Possible.absent();
    }

    @Override
    public String toString() {
        return "FakeMessageData{" +
                "content='" + this.content + '\'' +
                ", embeds=" + this.embeds +
                '}';
    }
}
