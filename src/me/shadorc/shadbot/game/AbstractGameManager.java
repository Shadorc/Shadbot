package me.shadorc.shadbot.game;

import java.util.ArrayList;
import java.util.List;

import sx.blah.discord.handle.obj.IUser;

public abstract class AbstractGameManager {

	private final List<IUser> players;

	public AbstractGameManager() {
		this.players = new ArrayList<>();
	}

	public abstract void start();

	public abstract void stop();

	public void addPlayer(IUser user) {
		players.add(user);
	}

	public boolean isPlaying(IUser user) {
		return players.contains(user);
	}

	public List<IUser> getPlayers() {
		return players;
	}

}
