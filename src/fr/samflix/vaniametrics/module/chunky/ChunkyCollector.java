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
 * Chunky — l'avancement de la prégénération.
 *
 * <p>MÉTRIQUE À DURÉE DE VIE COURTE, et c'est normal : elle vaut tout pendant une génération, et
 * rien le reste du temps. Une prégénération dure des heures et sature le disque et le processeur ;
 * savoir où elle en est évite de confondre « le serveur rame » avec « le serveur travaille ».
 *
 * <p>CHUNKY N'ÉMET PAS D'ÉVÉNEMENTS BUKKIT. Il a son propre bus, atteint par un service Bukkit,
 * et on s'y abonne par {@code onGenerationProgress}. L'abonnement se fait une seule fois, au
 * branchement — Chunky ne propose pas de se désabonner, ce qui est sans conséquence ici puisque le
 * module vit aussi longtemps que le serveur.
 */
public final class ChunkyCollector implements Collector {

	private final Platform plateforme;

	private Gauge avancement;
	private Gauge chunksFaits;
	private Gauge chunksTotal;
	private Counter taches;

	/** L'état de chaque tâche en cours, alimenté par le bus de Chunky. */
	private final Map<String, double[]> etat = new ConcurrentHashMap<>();

	public ChunkyCollector(Platform plateforme) {
		this.plateforme = plateforme;
	}

	@Override
	public String nom() {
		return "pregen";
	}

	@Override
	public String origine() {
		return "Chunky";
	}

	@Override
	public void declarer(MetricRegistry r) {
		avancement = r.gauge("pregen_progress_ratio",
				"Avancement de la prégénération, de 0 à 1. Absent quand rien ne tourne.", "world");
		chunksFaits = r.gauge("pregen_chunks_done", "Chunks générés pour la tâche en cours.", "world");
		chunksTotal = r.gauge("pregen_chunks_total", "Chunks à générer pour la tâche en cours.", "world");
		taches = r.counter("pregen_tasks_finished_total", "Prégénérations menées à terme.", "world");
		brancher();
	}

	@Override
	public void relever(MetricRegistry r) {
		// Une tâche terminée doit CESSER d'être publiée, sinon un serveur au repos afficherait
		// éternellement « 100 % » d'une génération finie la semaine dernière.
		avancement.clear();
		chunksFaits.clear();
		chunksTotal.clear();
		etat.forEach((monde, v) -> {
			avancement.set(v[2], monde);
			chunksFaits.set(v[0], monde);
			chunksTotal.set(v[1], monde);
		});
	}

	private void brancher() {
		ChunkyAPI api = Bukkit.getServicesManager().load(ChunkyAPI.class);
		if (api == null) {
			plateforme.avertir("Chunky : service introuvable, l'avancement ne sera pas publié");
			return;
		}
		api.onGenerationProgress(e -> {
			String monde = e.world().toLowerCase(Locale.ROOT);
			etat.put(monde, new double[] {e.chunks(), e.chunks() / Math.max(1e-9, e.progress() / 100.0),
					e.progress() / 100.0});
		});
		api.onGenerationComplete(e -> {
			String monde = e.world().toLowerCase(Locale.ROOT);
			etat.remove(monde);
			taches.inc(monde);
		});
		plateforme.info("collecteur pregen — abonné au bus de Chunky");
	}
}
