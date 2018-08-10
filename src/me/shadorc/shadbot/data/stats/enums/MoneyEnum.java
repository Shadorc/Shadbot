package me.shadorc.shadbot.data.stats.enums;

public enum MoneyEnum {
	MONEY_GAINED,
	MONEY_LOST
}

// TODO
/*
	public static EmbedCreateSpec getAverageEmbed() {
		Map<String, AtomicLong> moneyGained = MONEY_STATS_MAP.getOrDefault(MoneyEnum.MONEY_GAINED, Collections.emptyMap());
		Map<String, AtomicLong> moneyLost = MONEY_STATS_MAP.getOrDefault(MoneyEnum.MONEY_LOST, Collections.emptyMap());

		// TODO Add avatar icon
		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
				.setAuthor("Stats: average", null, null);

		List<String> gamesName = new ArrayList<>();
		gamesName.addAll(moneyGained.keySet());
		gamesName.addAll(moneyLost.keySet());
		gamesName = gamesName.stream().distinct().collect(Collectors.toList());

		if(gamesName.isEmpty()) {
			return embed.setDescription("No statistics yet.");
		}

		Map<String, AtomicInteger> commandsUsed = CommandStatsManager.get(CommandEnum.COMMAND_USED);

		Map<String, Tuple2<Float, Long>> averageMap = new HashMap<>();
		for(String gameName : gamesName) {
			long gains = moneyGained.getOrDefault(gameName, new AtomicLong(0)).get();
			long losses = moneyLost.getOrDefault(gameName, new AtomicLong(0)).get();
			long usages = commandsUsed.get(gameName).get();
			float average = ((float) gains - losses) / usages;
			averageMap.put(gameName, Tuples.of(average, usages));
		}

		Comparator<Entry<String, Tuple2<Float, Long>>> comparator = (v1, v2) -> v1.getValue().getT2().compareTo(v2.getValue().getT2());
		Map<String, Tuple2<Float, Long>> sortedMap = Utils.sortByValue(averageMap, comparator.reversed());

		return embed.addField("Name", String.join("\n", sortedMap.keySet()), true)
				.addField("Average", FormatUtils.format(sortedMap.values().stream().map(Tuple2::getT1), num -> FormatUtils.formatNum(num.intValue()), "\n"), true)
				.addField("Count", FormatUtils.format(sortedMap.values().stream().map(Tuple2::getT2), num -> FormatUtils.formatNum(num), "\n"), true);
	}
 */
