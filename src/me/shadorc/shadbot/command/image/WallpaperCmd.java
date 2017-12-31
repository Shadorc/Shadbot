package me.shadorc.shadbot.command.image;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.ivkos.wallhaven4j.Wallhaven;
import com.ivkos.wallhaven4j.models.misc.Ratio;
import com.ivkos.wallhaven4j.models.misc.Resolution;
import com.ivkos.wallhaven4j.models.misc.enums.Category;
import com.ivkos.wallhaven4j.models.misc.enums.Purity;
import com.ivkos.wallhaven4j.models.wallpaper.Wallpaper;
import com.ivkos.wallhaven4j.util.searchquery.SearchQueryBuilder;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "wallpaper" }, alias = "wp")
public class WallpaperCmd extends AbstractCommand {

	private Wallhaven wallhaven;

	// TODO: refactor this mess
	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(wallhaven == null) {
			wallhaven = new Wallhaven(APIKeys.get(APIKey.WALLHAVEN_LOGIN), APIKeys.get(APIKey.WALLHAVEN_PASSWORD));
		}

		Options options = new Options();
		options.addOption("p", "purity", true, FormatUtils.formatArray(Purity.values(), purity -> purity.toString().toLowerCase(), ", "));
		options.addOption("c", "category", true, FormatUtils.formatArray(Category.values(), cat -> cat.toString().toLowerCase(), ", "));
		options.addOption("rat", "ratio", true, "image ratio");
		options.addOption("res", "resolution", true, "image resolution");

		Option keyOpt = new Option("k", "keyword", true, "keyword");
		keyOpt.setValueSeparator(',');
		options.addOption(keyOpt);

		CommandLine cmd;
		try {
			List<String> args = StringUtils.split(context.getArg());
			cmd = new DefaultParser().parse(options, args.toArray(new String[args.size()]));
		} catch (UnrecognizedOptionException err) {
			BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " %s. Use `%shelp %s` for more information.",
					err.getMessage(), context.getPrefix(), this.getName()), context.getChannel());
			return;
		} catch (ParseException err) {
			ExceptionUtils.handle("getting a wallpaper", context, err);
			return;
		}

		SearchQueryBuilder queryBuilder = new SearchQueryBuilder();

		String purity = cmd.getOptionValue("purity", Purity.SFW.toString());
		if(purity != null) {
			if(!Arrays.stream(Purity.values()).anyMatch(purityValue -> purityValue.toString().equalsIgnoreCase(purity))) {
				this.sendInvalidArg("purity", context);
				return;
			}

			if(purity.matches("nsfw|sketchy") && !context.getChannel().isNSFW()) {
				BotUtils.sendMessage(TextUtils.mustBeNSFW(context.getPrefix()), context.getChannel());
				return;
			}

			queryBuilder.purity(Purity.valueOf(purity.toUpperCase()));
		}

		String category = cmd.getOptionValue("category");
		if(category != null) {
			if(!Arrays.stream(Category.values()).anyMatch(catValue -> catValue.toString().equalsIgnoreCase(category))) {
				this.sendInvalidArg("category", context);
				return;
			}
			queryBuilder.categories(Category.valueOf(category.toUpperCase()));
		}

		String ratio = cmd.getOptionValue("ratio");
		if(ratio != null) {
			Dimension dim = this.parseDimension(ratio);
			if(dim == null) {
				this.sendInvalidArg("ratio", context);
				return;
			}
			queryBuilder.ratios(new Ratio((int) dim.getWidth(), (int) dim.getHeight()));
		}

		String resolution = cmd.getOptionValue("resolution");
		if(resolution != null) {
			Dimension dim = this.parseDimension(resolution);
			if(dim == null) {
				this.sendInvalidArg("resolution", context);
				return;
			}
			queryBuilder.resolutions(new Resolution((int) dim.getWidth(), (int) dim.getHeight()));
		}

		String keyword = cmd.getOptionValue("keyword");
		if(keyword != null) {
			queryBuilder.keywords(keyword.split(","));
		}

		List<Wallpaper> wallpapers = wallhaven.search(queryBuilder.pages(1).build());
		if(wallpapers.isEmpty()) {
			BotUtils.sendMessage(TextUtils.noResult(context.getMessage().getContent()), context.getChannel());
			return;
		}

		Wallpaper wallpaper = wallpapers.get(MathUtils.rand(wallpapers.size()));

		String tags = FormatUtils.formatList(wallpaper.getTags(), tag -> "`" + tag.toString().replace("#", "") + "`", " ");
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Wallpaper")
				.withUrl(wallpaper.getUrl())
				.withImage(wallpaper.getImageUrl())
				.appendField("Resolution", wallpaper.getResolution().toString(), false)
				.appendField("Tags", tags, false);

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	private void sendInvalidArg(String arg, Context context) {
		BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " Invalid %s. Use `%shelp %s` for more information.",
				arg, context.getPrefix(), this.getName()), context.getChannel());
	}

	private Dimension parseDimension(String arg) {
		String[] ratioArray = arg.split(arg.contains("x") ? "x" : "\\*");
		if(ratioArray.length != 2 || !StringUtils.isPositiveInt(ratioArray[0]) || !StringUtils.isPositiveInt(ratioArray[1])) {
			return null;
		}
		return new Dimension(Integer.parseInt(ratioArray[0]), Integer.parseInt(ratioArray[1]));
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Search for a wallpaper.")
				.setUsage("[-p purity] [-c category] [-rat ratio] [-res resolution] [-k keywords]")
				.addArg("purity", FormatUtils.formatArray(Purity.values(), purity -> purity.toString().toLowerCase(), ", "), true)
				.addArg("category", FormatUtils.formatArray(Category.values(), cat -> cat.toString().toLowerCase(), ", "), true)
				.addArg("ratio", "image ratio (e.g. 16x9)", true)
				.addArg("resolution", "image resolution (e.g. 1920x1080)", true)
				.addArg("keyword", "keywords (e.g. doom,game)", true)
				.setExample(String.format("Search a *SFW* wallpaper in category *Anime*, with a *16x9* ratio :"
						+ "%n`%s%s -p sfw -c anime -rat 16x9`", context.getPrefix(), this.getName()))
				.appendField("Source", "https://alpha.wallhaven.cc/", false)
				.build();
	}
}