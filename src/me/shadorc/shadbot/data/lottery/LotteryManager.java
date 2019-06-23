package me.shadorc.shadbot.data.lottery;

import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.Utils;

import java.io.IOException;
import java.time.Duration;

public class LotteryManager extends Data {

    private static LotteryManager instance;

    static {
        try {
            LotteryManager.instance = new LotteryManager();
        } catch (final IOException err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", LotteryManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.value());
        }
    }

    private final Lottery lottery;

    private LotteryManager() throws IOException {
        super("lotto_data.json", Duration.ofMinutes(30), Duration.ofMinutes(30));

        this.lottery = this.getFile().exists() ? Utils.MAPPER.readValue(this.getFile(), Lottery.class) : new Lottery();
    }

    public Lottery getLottery() {
        return this.lottery;
    }

    @Override
    public Object getData() {
        return this.lottery;
    }

    public static LotteryManager getInstance() {
        return LotteryManager.instance;
    }

}
