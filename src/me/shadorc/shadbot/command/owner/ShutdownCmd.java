package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class ShutdownCmd extends BaseCmd {

	public ShutdownCmd() {
		super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("shutdown"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		return Shadbot.quit(ExitCode.NORMAL);
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Shutdown the bot.")
				.build();
	}

}
