package me.shadorc.discordbot.command.rpg;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.rpg.Mob;
import me.shadorc.discordbot.rpg.User;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;

public class FightCmd extends Command {

	public FightCmd() {
		super(false, "fight");
	}

	@Override
	public void execute(Context context) {
		User user = context.getUser();
		Mob mob = new Mob(user.getLevel());

		while(true) {
			int userDmg = MathUtils.rand(user.getWeapon().getMinDamage(), user.getWeapon().getMaxDamage());
			mob.hurt(userDmg);
			user.hurt(mob.getDamage());

			if(user.getLife() == 0) {
				BotUtils.sendMessage("Vous êtes mort en vous battant contre un(e) " + mob.getName() + " niveau " + mob.getLevel() + ".", context.getChannel());
				break;
			}
			if(mob.getLife() == 0) {
				int xp = MathUtils.rand(5, 10);
				BotUtils.sendMessage(Emoji.SWORDS + " Vous êtes venu à bout d'un(e) " + mob.getName() + " niveau " + mob.getLevel() + ". "
						+ "Vous remportez " + xp + " points d'expérience !", context.getChannel());
				if(user.addXp(xp)) {
					BotUtils.sendMessage(Emoji.SHIELD + " Vous venez de gagner un niveau ! Vous êtes maintenant niveau " + user.getLevel() + ".", context.getChannel());
				}
				break;
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		// TODO
	}
}
