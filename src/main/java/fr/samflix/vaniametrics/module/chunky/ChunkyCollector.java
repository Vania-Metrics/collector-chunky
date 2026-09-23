package fr.samflix.vaniametrics.module.chunky;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import org.popcraft.chunky.api.ChunkyAPI;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.Counter;
import fr.samflix.vaniametrics.api.Gauge;
import fr.samflix.vaniametrics.api.MetricRegistry;
import fr.samflix.vaniametrics.api.Platform;

/**
 * Chunky — pregeneration progress.
 *
 * <p>SHORT-LIVED METRIC, and that's expected: it's meaningful during a generation, and nothing
 * the rest of the time. A pregeneration runs for hours and saturates disk and CPU; knowing where
 * it stands avoids confusing "the server is lagging" with "the server is working".
 *
 * <p>CHUNKY DOES NOT EMIT BUKKIT EVENTS. It has its own bus, reached through a Bukkit service,
 * subscribed to via {@code onGenerationProgress}. The subscription happens once, at hookup —
 * Chunky offers no way to unsubscribe, which has no consequence here since the module lives as
 * long as the server.
 */
public final class ChunkyCollector implements Collector {

	private final Platform platform;

	private Gauge progress;
	private Gauge chunksDone;
	private Gauge chunksTotal;
	private Counter tasks;

	/** State of each running task, fed by Chunky's bus. */
	private final Map<String, double[]> state = new ConcurrentHashMap<>();

	public ChunkyCollector(Platform platform) {
		this.platform = platform;
	}

	@Override
	public String name() {
		return "pregen";
	}

	@Override
	public String source() {
		return "Chunky";
	}

	@Override
	public void declare(MetricRegistry r) {
		progress = r.gauge("pregen_progress_ratio",
				"Pregeneration progress, from 0 to 1. Absent when nothing is running.", "world");
		chunksDone = r.gauge("pregen_chunks_done", "Chunks generated for the current task.", "world");
		chunksTotal = r.gauge("pregen_chunks_total", "Chunks to generate for the current task.", "world");
		tasks = r.counter("pregen_tasks_finished_total", "Pregenerations completed.", "world");
		hook();
	}

	@Override
	public void collect(MetricRegistry r) {
		// A finished task must STOP being published, otherwise an idle server would forever
		// show "100%" of a generation that finished last week.
		progress.clear();
		chunksDone.clear();
		chunksTotal.clear();
		state.forEach((world, v) -> {
			progress.set(v[2], world);
			chunksDone.set(v[0], world);
			chunksTotal.set(v[1], world);
		});
	}

	private void hook() {
		ChunkyAPI api = Bukkit.getServicesManager().load(ChunkyAPI.class);
		if (api == null) {
			platform.warn("Chunky: service not found, progress will not be published");
			return;
		}
		api.onGenerationProgress(e -> {
			String world = e.world().toLowerCase(Locale.ROOT);
			state.put(world, new double[] {e.chunks(), e.chunks() / Math.max(1e-9, e.progress() / 100.0),
					e.progress() / 100.0});
		});
		api.onGenerationComplete(e -> {
			String world = e.world().toLowerCase(Locale.ROOT);
			state.remove(world);
			tasks.inc(world);
		});
		platform.info("pregen collector — subscribed to Chunky's bus");
	}
}
