package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.stats.entity.resources.ResourcesStats;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public class ResourceStatsCmd extends BaseCmd {

    public ResourceStatsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("resource_stats", "resource-stats", "resourcestats"), "res_stats");
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getStats()
                .getResourcesStats()
                .map(dailyResources -> {
                    // Create dataset
                    final TimeSeries cpuUsageSeries = new TimeSeries("CPU usage");
                    final TimeSeries ramUsageSeries = new TimeSeries("RAM usage");
                    for (final ResourcesStats bean : dailyResources.getResourcesUsage()) {
                        cpuUsageSeries.add(new FixedMillisecond(bean.getTimestamp().toEpochMilli()), bean.getCpuUsage());
                        ramUsageSeries.add(new FixedMillisecond(bean.getTimestamp().toEpochMilli()), bean.getRamUsage());
                    }

                    // Set dataset
                    final XYPlot plot = new XYPlot();
                    plot.setDataset(0, new TimeSeriesCollection(cpuUsageSeries));
                    plot.setDataset(1, new TimeSeriesCollection(ramUsageSeries));

                    // Set axis
                    final DateAxis xAxis = new DateAxis("timestamp");
                    plot.setDomainAxis(xAxis);

                    final NumberAxis cpuAxis = new NumberAxis("% (CPU)");
                    cpuAxis.setRange(0, 100);
                    plot.setRangeAxis(0, cpuAxis);

                    final NumberAxis ramAxis = new NumberAxis("Mb (RAM)");
                    ramAxis.setAutoRange(true);
                    plot.setRangeAxis(1, ramAxis);

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

                    //Map the data to the appropriate axis
                    plot.mapDatasetToRangeAxis(0, 0);
                    plot.mapDatasetToRangeAxis(1, 1);

                    // Create chart
                    final JFreeChart chart = new JFreeChart("System resources utilisation",
                            JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                    final ChartPanel chartPanel = new ChartPanel(chart, false);

                    // Draw jpeg
                    final int width = 512;
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
                            return context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(spec -> spec.addFile("chart.jpeg", is), channel, false))
                                    .then();
                        }
                    } catch (final IOException err) {
                        return Mono.error(err);
                    }
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Display resources utilisation statistics.")
                .build();
    }

}
