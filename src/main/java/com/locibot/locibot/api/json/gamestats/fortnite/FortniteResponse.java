package com.locibot.locibot.api.json.gamestats.fortnite;

import java.util.Optional;

public record FortniteResponse(Optional<String> error,
                               Stats stats) {


}
