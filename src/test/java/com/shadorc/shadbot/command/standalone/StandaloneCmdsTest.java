package com.shadorc.shadbot.command.standalone;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import com.shadorc.shadbot.discord4j.data.FakeMessageData;
import com.shadorc.shadbot.discord4j.data.SerializationUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.*;
import discord4j.discordjson.possible.Possible;
import discord4j.gateway.ShardInfo;
import discord4j.rest.service.WebhookService;
import discord4j.rest.util.MultipartRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StandaloneCmdsTest {

    private static final Logger LOGGER = Loggers.getLogger(StandaloneCmdsTest.class);

    private static Context mockedContext;

    @BeforeAll
    public static void setup() throws IOException {
        final DBGuild mockedDbGuild = mock(DBGuild.class);
        when(mockedDbGuild.getLocale()).thenReturn(Locale.FRENCH);

        final GatewayDiscordClient mockedGateway = mock(GatewayDiscordClient.class);
        final DiscordClient mockedClient = mock(DiscordClient.class);
        when(mockedGateway.rest()).thenReturn(mockedClient);

        final GuildData guildData = SerializationUtil.read("json/GuildData.json", GuildData.class);
        final Guild guild = new Guild(mockedGateway, guildData);
        when(mockedGateway.getGuildById(any(Snowflake.class)))
                .thenReturn(Mono.just(guild));

        final ChannelData channelData = SerializationUtil.read("json/ChannelData.json", ChannelData.class);
        final TextChannel channel = new TextChannel(mockedGateway, channelData);
        when(mockedGateway.getChannelById(any(Snowflake.class)))
                .thenReturn(Mono.just(channel));

        final MemberData memberData = SerializationUtil.read("json/MemberData.json", MemberData.class);
        final Member member = new Member(mockedGateway, memberData, guild.getId().asLong());
        when(mockedGateway.getMemberById(any(Snowflake.class), any(Snowflake.class)))
                .thenReturn(Mono.just(member));

        final WebhookService mockedWebhookService = mock(WebhookService.class);
        when(mockedClient.getWebhookService()).thenReturn(mockedWebhookService);

        when(mockedWebhookService.executeWebhook(anyLong(), anyString(), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    final WebhookExecuteRequest request = invocation.<MultipartRequest<WebhookExecuteRequest>>getArgument(3).getJsonPayload();
                    final String content = Objects.requireNonNull(request).content().toOptional().orElse("");
                    final List<EmbedData> embeds = request.embeds().toOptional().orElse(Collections.emptyList());
                    return Mono.just(new FakeMessageData(content, embeds));
                });
        when(mockedWebhookService.modifyWebhookMessage(anyLong(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    final WebhookMessageEditRequest request = invocation.getArgument(3);
                    final String content = Possible.flatOpt(request.content()).orElse("");
                    final List<EmbedData> embeds = Possible.flatOpt(request.embeds()).orElse(Collections.emptyList());
                    return Mono.just(new FakeMessageData(content, embeds));
                });

        final ShardInfo shardInfo = ShardInfo.create(3, 11);
        final InteractionData interactionData = SerializationUtil.read("json/InteractionData.json", InteractionData.class);
        final Interaction interaction = new Interaction(mockedGateway, interactionData);
        final InteractionCreateEvent mockedEvent = new InteractionCreateEvent(mockedGateway, shardInfo, interaction);
        mockedContext = new Context(mockedEvent, mockedDbGuild);
    }

    @Test
    public void testPing() {
        final PingCmd cmd = new PingCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(cmd.getName(), message);
        assertFalse(message.getContent().isBlank());
    }

    @Test
    public void testHelp() {
        final HelpCmd cmd = new HelpCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(cmd.getName(), message);
        assertFalse(message.getEmbeds().isEmpty());
    }

    @Test
    public void testInvite() {
        final InviteCmd cmd = new InviteCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(cmd.getName(), message);
        assertFalse(message.getEmbeds().isEmpty());
    }

    private static void logMessage(String cmdName, Message message) {
        if (!message.getContent().isBlank()) {
            LOGGER.info("{} content output:\n{}", cmdName, message.getContent());
        }
        if (!message.getEmbeds().isEmpty()) {
            final StringBuilder strBuilder = new StringBuilder();
            for (final Embed embed : message.getEmbeds()) {
                for (final Embed.Field field : embed.getFields()) {
                    strBuilder.append(field.getName());
                    strBuilder.append("\n\t").append(field.getValue()).append("\n");
                }
            }
            LOGGER.info("{} embed output:\n{}", cmdName, strBuilder);
        }
    }
}
