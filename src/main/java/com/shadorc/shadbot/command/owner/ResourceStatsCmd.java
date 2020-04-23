package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.stats.entity.resources.ResourceStats;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class ResourceStatsCmd extends BaseCmd {

    public static final Duration UPDATE_INTERVAL = Duration.ofSeconds(10);
    public static final Duration MAX_DURATION = Duration.ofHours(4);

    public ResourceStatsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("resource_stats", "resource-stats"), "res_stats");
    }

    @Override
    public Mono<Void> execute(Context context) {
        if (context.getArg().map("drop"::equals).orElse(false)) {
            return DatabaseManager.getStats()
                    .dropSystemStats()
                    .then(context.getChannel())
                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.INFO + " System stats collection dropped.", channel))
                    .then();
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.ATTACH_FILES)
                        .then(DatabaseManager.getStats().getResourcesStats())
                        .map(dailyResources -> {
                            // Create dataset
                            final TimeSeries cpuUsageSeries = new TimeSeries("CPU usage");
                            final TimeSeries ramUsageSeries = new TimeSeries("RAM usage");
                            final TimeSeries threadUsageSeries = new TimeSeries("Thread count");
                            for (final ResourceStats bean : dailyResources.getResourcesUsage()) {
                                cpuUsageSeries.add(new FixedMillisecond(bean.getTimestamp().toEpochMilli()), bean.getCpuUsage());
                                ramUsageSeries.add(new FixedMillisecond(bean.getTimestamp().toEpochMilli()), bean.getRamUsage());
                                threadUsageSeries.add(new FixedMillisecond(bean.getTimestamp().toEpochMilli()), bean.getThreadCount());
                            }

                            // Set dataset
                            final XYPlot plot = new XYPlot();
                            plot.setDataset(0, new TimeSeriesCollection(cpuUsageSeries));
                            plot.setDataset(1, new TimeSeriesCollection(ramUsageSeries));
                            plot.setDataset(2, new TimeSeriesCollection(threadUsageSeries));

                            // Set axis
                            final DateAxis xAxis = new DateAxis("time");
                            plot.setDomainAxis(xAxis);

                            final NumberAxis cpuAxis = new NumberAxis("% (CPU)");
                            cpuAxis.setRange(0, 100);
                            plot.setRangeAxis(0, cpuAxis);

                            final NumberAxis ramAxis = new NumberAxis("Mb (RAM)");
                            plot.setRangeAxis(1, ramAxis);

                            final NumberAxis threadAxis = new NumberAxis("Count");
                            plot.setRangeAxis(2, threadAxis);

                            // Set renderer
                            final XYSplineRenderer cpuRenderer = new XYSplineRenderer();
                            cpuRenderer.setPrecision(7);
                            cpuRenderer.setSeriesShapesVisible(0, false);
                            plot.setRenderer(0, cpuRenderer);

                            final XYSplineRenderer ramRenderer = new XYSplineRenderer();
                            ramRenderer.setPrecision(7);
                            ramRenderer.setSeriesShapesVisible(0, false);
                            ramRenderer.setSeriesFillPaint(0, Color.BLUE);
                            plot.setRenderer(1, ramRenderer);

                            final XYSplineRenderer threadRenderer = new XYSplineRenderer();
                            threadRenderer.setPrecision(7);
                            threadRenderer.setSeriesShapesVisible(0, false);
                            threadRenderer.setSeriesFillPaint(0, Color.GREEN);
                            plot.setRenderer(2, threadRenderer);

                            //Map the data to the appropriate axis
                            plot.mapDatasetToRangeAxis(0, 0);
                            plot.mapDatasetToRangeAxis(1, 1);
                            plot.mapDatasetToRangeAxis(2, 2);

                            // Create chart
                            final JFreeChart chart = new JFreeChart("System resources utilisation",
                                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                            final ChartPanel chartPanel = new ChartPanel(chart, false);

                            // Draw jpeg
                            final int width = 640;
                            final int height = 360;
                            final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
                            final Graphics graphics = bufferedImage.getGraphics();
                            chartPanel.setBounds(0, 0, width, height);
                            chartPanel.paint(graphics);

                            return bufferedImage;
                        })
                        .flatMap(bufferedImage -> {
                            try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                                ImageIO.write(bufferedImage, "jpeg", os);
                                try (final InputStream is = new ByteArrayInputStream(os.toByteArray())) {
                                    return DiscordUtils.sendMessage(spec -> spec.addFile("chart.jpeg", is), channel, false);
                                }
                            } catch (final IOException err) {
                                return Mono.error(err);
                            }
                        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Display resources utilisation statistics.")
                .addArg("drop", "drop the collection", true)
                .build();
    }

}
