package fr.samflix.vaniametrics.module.chunky;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * Métriques de prégénération Chunky.
 *
 * <p>Métrique à durée de vie courte : elle vaut tout pendant une génération, et rien le reste du temps.
 */
public final class ChunkyPaper extends JavaPlugin {

	private ChunkyCollector collecteur;

	@Override
	public void onEnable() {
		VaniaMetrics metriques = VaniaMetricsProvider.get();
		collecteur = new ChunkyCollector(metriques.plateforme());
		metriques.enregistrer(collecteur);
	}

	@Override
	public void onDisable() {
		if (collecteur != null) {
			VaniaMetricsProvider.chercher().ifPresent(m -> m.retirer(collecteur));
		}
	}
}
