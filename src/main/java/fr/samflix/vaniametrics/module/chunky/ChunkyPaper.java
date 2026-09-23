package fr.samflix.vaniametrics.module.chunky;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * Chunky pregeneration metrics.
 *
 * <p>Short-lived metric: it's meaningful during a generation, and nothing the rest of the time.
 */
public final class ChunkyPaper extends JavaPlugin {

	private ChunkyCollector collector;

	@Override
	public void onEnable() {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new ChunkyCollector(metrics.platform());
		metrics.register(collector);
	}

	@Override
	public void onDisable() {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
