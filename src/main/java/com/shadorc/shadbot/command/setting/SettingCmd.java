package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;

import java.util.List;

public class SettingCmd extends BaseCmdGroup {

    public SettingCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, "setting", "Configure Shadbot",
                List.of());
    }

/* TODO
    private static Mono<Consumer<EmbedCreateSpec>> show(Context context, Settings settings) {
        return Flux.fromIterable(SettingManager.getInstance().getSettings().values())
                .distinct()
                .flatMap(setting -> setting.show(context, settings))
                .collectList()
                .map(fields -> ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Settings", null, context.getAvatarUrl());

                            if (fields.isEmpty()) {
                                embed.setDescription("There is no custom settings for this server.");
                            } else {
                                for (final ImmutableEmbedFieldData field : fields) {
                                    embed.addField("**" + field.name() + "**", field.value(),
                                            field.inline().toOptional().orElse(false));
                                }
                            }
                        }));
    }
*/

/* TODO?
    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        final HelpBuilder embed = CommandHelpBuilder.create(this, context)
                .setThumbnail("https://i.imgur.com/QA2PUjM.png")
                .setDescription("Change Shadbot's settings for this server.")
                .addArg("name", false)
                .addArg("args", false)
                .addField("Additional Help", String.format("`%s%s <name> help`",
                        context.getPrefix(), this.getName()), false)
                .addField("Current settings", String.format("`%s%s show`",
                        context.getPrefix(), this.getName()), false);

        SettingManager.getInstance().getSettings()
                .values()
                .stream()
                .distinct()
                .forEach(setting -> embed.addField(String.format("Name: %s", setting.getName()),
                        setting.getDescription(), false));

        return embed.build();
    }
*/

}
