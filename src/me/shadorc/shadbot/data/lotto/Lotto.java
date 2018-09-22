package me.shadorc.shadbot.data.lotto;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;

public class Lotto {

	@JsonProperty("historic")
	private LottoHistoric historic;
	@JsonProperty("jackpot")
	private final AtomicInteger jackpot;
	@JsonProperty("gamblers")
	private final List<LottoGambler> gamblers;

	public Lotto() {
		this.historic = null;
		this.jackpot = new AtomicInteger(0);
		this.gamblers = new CopyOnWriteArrayList<>();
	}

	@Nullable
	public LottoHistoric getHistoric() {
		return this.historic;
	}

	public int getJackpot() {
		return this.jackpot.get();
	}

	public List<LottoGambler> getGamblers() {
		return this.gamblers;
	}

	public void setHistoric(LottoHistoric historic) {
		this.historic = historic;
	}

	public void addToJackpot(int coins) {
		final int newPool = this.jackpot.get() + (int) Math.ceil(coins / 100f);
		this.jackpot.set(NumberUtils.between(newPool, 0, Config.MAX_COINS));
	}

	public void addGambler(Snowflake guildId, Snowflake userId, int number) {
		this.gamblers.add(new LottoGambler(guildId, userId, number));
	}

	public void resetJackpot() {
		this.jackpot.set(0);
	}

	public void resetGamblers() {
		this.gamblers.clear();
	}

	@Override
	public String toString() {
		return String.format("Lotto [historic=%s, jackpot=%s, gamblers=%s]", this.historic, this.jackpot, this.gamblers);
	}

}
