package me.shadorc.discordbot.command.image;

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

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class WallpaperCmd extends AbstractCommand {

	private Wallhaven wallhaven;

	public WallpaperCmd() {
		super(CommandCategory.IMAGE, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "wallpaper");
		this.setAlias("wp");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(wallhaven == null) {
			wallhaven = new Wallhaven(Config.get(APIKey.WALLHAVEN_LOGIN), Config.get(APIKey.WALLHAVEN_PASSWORD));
		}

		Options options = new Options();
		options.addOption("p", "purity", true, "nsfw, sketchy, sfw");
		options.addOption("c", "category", true, "people, anime, general");
		options.addOption("rat", "ratio", true, "image ratio");
		options.addOption("res", "resolution", true, "image resolution");

		Option keyOpt = new Option("k", "keyword", true, "keyword");
		keyOpt.setValueSeparator(',');
		options.addOption(keyOpt);

		CommandLine cmd;
		try {
			cmd = new DefaultParser().parse(options, StringUtils.getSplittedArg(context.getArg()));
		} catch (UnrecognizedOptionException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " " + err.getMessage() + ". "
					+ "Use `" + context.getPrefix() + "help " + this.getFirstName() + "` for more information.", context.getChannel());
			return;
		} catch (ParseException err) {
			ExceptionUtils.manageException("getting a wallpaper", context, err);
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
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This must be a NSFW-channel. If you're an admin, you can use "
						+ "`" + context.getPrefix() + "settings " + Setting.NSFW + " toggle`", context.getChannel());
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

		EmbedBuilder embed = Utils.getDefaultEmbed()
				.withAuthorName("Wallpaper")
				.withUrl(wallpaper.getUrl())
				.withImage(wallpaper.getImageUrl())
				.appendField("Resolution", wallpaper.getResolution().toString(), false)
				.appendField("Tags", FormatUtils.formatList(wallpaper.getTags(), tag -> "`" + tag.toString().replace("#", "") + "`", " "), false);

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	private void sendInvalidArg(String arg, Context context) {
		BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid " + arg + ". Use `" + context.getPrefix() + "help "
				+ this.getFirstName() + "` for more information.", context.getChannel());
	}

	private Dimension parseDimension(String arg) {
		String[] ratioArray = arg.split(arg.contains("x") ? "x" : "\\*");
		if(ratioArray.length != 2 || !StringUtils.isPositiveInt(ratioArray[0]) || !StringUtils.isPositiveInt(ratioArray[1])) {
			return null;
		}
		return new Dimension(Integer.parseInt(ratioArray[0]), Integer.parseInt(ratioArray[1]));
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.withDescription("**Search for a wallpaper.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [-p purity] [-c category] [-rat ratio] [-res resolution] [-k keywords]`", false)
				.appendField("Arguments", "**purity** - " + FormatUtils.formatArray(Purity.values(), purity -> purity.toString().toLowerCase(), ", ")
						+ "\n**category** - " + FormatUtils.formatArray(Category.values(), cat -> cat.toString().toLowerCase(), ", ")
						+ "\n**ratio** - image ratio (e.g. 16x9)"
						+ "\n**resolution** - image resolution (e.g. 1920x1080)"
						+ "\n**keyword** - keywords (e.g. doom,game)", true)
				.appendField("Example", "Search a *SFW* wallpaper in category *Anime*, with a *16x9* ratio :"
						+ "\n`" + context.getPrefix() + this.getFirstName() + " -p sfw -c anime -rat 16x9`", false)
				.appendField("Source", "https://alpha.wallhaven.cc/", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}