package com.locibot.locibot.command.setting;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.command.Setting;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocaleSetting extends BaseCmd {

    public LocaleSetting() {
        super(CommandCategory.SETTING, "language", "Manage default server language");

        final List<String> locales = Arrays.stream(I18nManager.LOCALES)
                .map(Locale::toLanguageTag)
                .collect(Collectors.toList());

        this.addOption(option -> option.name("value")
                .description("The default language of the server")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .choices(DiscordUtil.toOptions(locales)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String locale = context.getOptionAsString("value").orElseThrow();
        return context.getDbGuild()
                .updateSetting(Setting.LOCALE, locale)
                .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("setting.locale.message")
                        .formatted(locale)));
    }
}
