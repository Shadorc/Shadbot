package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

public record Competitive(CompetitiveRank tank,
                          CompetitiveRank damage,
                          CompetitiveRank support) {

}
