package com.shadorc.shadbot.command.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.FakeMessageData;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import discord4j.common.JacksonResources;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StandaloneCmdsTest {

    private static final Logger LOGGER = Loggers.getLogger(StandaloneCmdsTest.class);

    private static ObjectMapper mapper;
    private static Context mockedContext;

    @BeforeAll
    public static void setup() throws IOException {
        mapper = JacksonResources.INITIALIZER
                .andThen(JacksonResources.HANDLE_UNKNOWN_PROPERTIES)
                .apply(new ObjectMapper());

        final DBGuild mockedDbGuild = mock(DBGuild.class);
        when(mockedDbGuild.getLocale()).thenReturn(Locale.FRENCH);

        final GatewayDiscordClient mockedGateway = mock(GatewayDiscordClient.class);
        final DiscordClient mockedClient = mock(DiscordClient.class);
        when(mockedGateway.rest()).thenReturn(mockedClient);

        final GuildData guildData = read("json/GuildData.json", GuildData.class);
        final Guild guild = new Guild(mockedGateway, guildData);
        when(mockedGateway.getGuildById(any(Snowflake.class)))
                .thenReturn(Mono.just(guild));

        final ChannelData channelData = read("json/ChannelData.json", ChannelData.class);
        final TextChannel channel = new TextChannel(mockedGateway, channelData);
        when(mockedGateway.getChannelById(any(Snowflake.class)))
                .thenReturn(Mono.just(channel));

        final MemberData memberData = read("json/MemberData.json", MemberData.class);
        final Member member = new Member(mockedGateway, memberData, guild.getId().asLong());
        when(mockedGateway.getMemberById(any(Snowflake.class), any(Snowflake.class)))
                .thenReturn(Mono.just(member));

        final WebhookService mockedWebhookService = mock(WebhookService.class);
        when(mockedClient.getWebhookService()).thenReturn(mockedWebhookService);

        when(mockedWebhookService.executeWebhook(anyLong(), anyString(), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    final WebhookExecuteRequest request = invocation.<MultipartRequest<WebhookExecuteRequest>>getArgument(3).getJsonPayload();
                    assert request != null;
                    final String content = request.content().toOptional().orElse("");
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
        final InteractionData interactionData = read("json/InteractionData.json", InteractionData.class);
        final Interaction interaction = new Interaction(mockedGateway, interactionData);
        final InteractionCreateEvent mockedEvent = new InteractionCreateEvent(mockedGateway, shardInfo, interaction);
        mockedContext = new Context(mockedEvent, mockedDbGuild);
    }

    @Test
    public void testPing() {
        final PingCmd cmd = new PingCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(message);
        assertFalse(message.getContent().isBlank());
    }

    @Test
    public void testHelp() {
        final HelpCmd cmd = new HelpCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(message);
        assertFalse(message.getEmbeds().isEmpty());
    }

    @Test
    public void testInvite() {
        final InviteCmd cmd = new InviteCmd();
        final Message message = cmd.execute(mockedContext).cast(Message.class).block();
        assertNotNull(message);
        logMessage(message);
        assertFalse(message.getEmbeds().isEmpty());
    }

    private static <T> T read(String from, Class<T> into) throws IOException {
        return mapper.readValue(StandaloneCmdsTest.class.getClassLoader().getResourceAsStream(from), into);
    }

    private static void logMessage(Message message) {
        if (!message.getContent().isBlank()) {
            LOGGER.info(message.getContent());
        }
        if (!message.getEmbeds().isEmpty()) {
            for (final Embed embed : message.getEmbeds()) {
                for (final Embed.Field field : embed.getFields()) {
                    LOGGER.info("{}", field.getName());
                    LOGGER.info("\t{}", field.getValue());
                }
            }
        }
    }

}
