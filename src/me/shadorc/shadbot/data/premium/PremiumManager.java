package me.shadorc.shadbot.data.premium;

import com.fasterxml.jackson.databind.JavaType;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.utils.Utils;
import reactor.util.annotation.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PremiumManager extends Data {

	private final List<Relic> relics;

	public PremiumManager() throws IOException {
		super("premium_data.json", Duration.ofHours(1), Duration.ofHours(1));

		this.relics = new CopyOnWriteArrayList<>();
		if(this.getFile().exists()) {
			final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Relic.class);
			this.relics.addAll(Utils.MAPPER.readValue(this.getFile(), valueType));
		}
	}

	public Relic generateRelic(RelicType type) {
		final Relic relic = new Relic(UUID.randomUUID().toString(), TimeUnit.DAYS.toMillis(180), type);
		this.relics.add(relic);
		return relic;
	}

	public void activateRelic(@Nullable Snowflake guildId, Snowflake userId, String relicId) throws RelicActivationException {
		final Optional<Relic> relicOpt = this.relics.stream()
				.filter(relicItr -> relicItr.getId().equals(relicId))
				.findFirst();

		if(relicOpt.isEmpty()) {
			throw new RelicActivationException("This key is already activated or doesn't exist.");
		}

		final Relic relic = relicOpt.get();

		if(relic.getType().equals(RelicType.GUILD.toString()) && guildId == null) {
			throw new RelicActivationException("You must activate a Legendary Relic in the desired server.");
		}

		relic.activate(userId);

		if(relic.getType().equals(RelicType.GUILD.toString())) {
			relic.setGuildId(guildId);
		}
	}

	public List<Relic> getRelicsForUser(Snowflake userId) {
		return this.relics.stream()
				.filter(relic -> relic.getUserId().map(userId::equals).orElse(false))
				.collect(Collectors.toList());
	}

	public boolean isPremium(Snowflake guildId, Snowflake userId) {
		return this.isGuildPremium(guildId) || this.isUserPremium(userId);
	}

	public boolean isGuildPremium(Snowflake guildId) {
		return this.relics.stream()
				.filter(relic -> relic.getGuildId().map(guildId::equals).orElse(false))
				.anyMatch(relic -> !relic.isExpired());
	}

	private boolean isUserPremium(Snowflake userId) {
		return this.getRelicsForUser(userId).stream()
				.anyMatch(relic -> this.isValid(relic, RelicType.USER));
	}

	private boolean isValid(Relic relic, RelicType type) {
		return relic.getType().equals(type.toString()) && !relic.isExpired();
	}

	@Override
	public Object getData() {
		return this.relics;
	}

}
