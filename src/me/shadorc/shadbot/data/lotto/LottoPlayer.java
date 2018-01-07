package me.shadorc.shadbot.data.lotto;

public class LottoPlayer {

	private final long guildID;
	private final long userID;
	private final int num;

	public LottoPlayer(long guildID, long userID, int num) {
		this.guildID = guildID;
		this.userID = userID;
		this.num = num;
	}

	public long getGuildID() {
		return guildID;
	}

	public long getUserID() {
		return userID;
	}

	public int getNum() {
		return num;
	}

}
