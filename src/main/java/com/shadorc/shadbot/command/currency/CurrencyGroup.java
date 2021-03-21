package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class CurrencyGroup extends BaseCmdGroup {

    public CurrencyGroup() {
        super(CommandCategory.CURRENCY, "currency", "Currency commands",
                List.of(new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd()));
    }

}
