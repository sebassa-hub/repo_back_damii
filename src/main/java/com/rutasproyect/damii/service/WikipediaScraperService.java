package com.rutasproyect.damii.service;

import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class WikipediaScraperService {

    @Autowired
    private TransportRouteRepository routeRepository;

    @Autowired
    private DataOptimizationService optimizationService; // Para reusar el ProgressMap

    @Async
    public void scrapeAndSyncRoutes() {
        String taskId = "global_wiki_sync";
        optimizationService.getProgressMap().put(taskId,
                new com.rutasproyect.damii.dto.ProgressInfo("RUNNING", 0, 100, "Conectando a Wikipedia..."));

        try {
            Document doc = Jsoup.connect(
                    "https://es.wikipedia.org/wiki/Anexo:Directorio_de_rutas_de_transporte_p%C3%BAblico_de_Lima_Metropolitana_y_Callao")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(15000)
                    .get();

            Elements tables = doc.select("table.wikitable");
            int totalRoutes = 0;
            for (Element table : tables) {
                totalRoutes += table.select("tr").size() - 1; // Restar headers
            }

            optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("RUNNING", 0,
                    totalRoutes, "Extrayendo " + totalRoutes + " rutas..."));

            int current = 0;
            for (Element table : tables) {
                Elements rows = table.select("tr");
                for (int i = 1; i < rows.size(); i++) { // Evitar el thead
                    Elements cols = rows.get(i).select("td, th");
                    String code = "";
                    if (cols.size() >= 4) {
                        code = cols.get(0).text().trim();
                        String network = cols.get(1).text().trim();
                        String origin = cols.get(2).text().trim();
                        String destination = cols.get(3).text().trim();

                        // Guardar o actualizar en base de datos
                        if (!code.isEmpty()) {
                            final String currentCode = code;
                            Optional<TransportRoute> existingOpt = routeRepository.findAll().stream()
                                    .filter(r -> currentCode.equalsIgnoreCase(r.getRouteRef())
                                            || currentCode.equalsIgnoreCase(r.getName()))
                                    .findFirst();

                            TransportRoute route;
                            if (existingOpt.isPresent()) {
                                route = existingOpt.get();
                            } else {
                                route = new TransportRoute();
                                route.setId((int) (Math.random() * 100000)); // ID temporal, si no es auto-incremental.
                                route.setName(code);
                                route.setRouteRef(code);
                            }

                            route.setNetwork(network);
                            route.setOrigin(origin);
                            route.setDestination(destination);
                            route.setStatus("ACTIVA");

                            routeRepository.save(route);
                        }
                    }
                    current++;
                    if (current % 10 == 0) {
                        optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo(
                                "RUNNING", current, totalRoutes, "Sincronizando ruta " + code + "..."));
                    }
                }
            }

            optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("COMPLETED",
                    current, totalRoutes, "¡Catálogo de rutas actualizado con éxito!"));

        } catch (Exception e) {
            optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("ERROR", 0,
                    100, "Error en Wikipedia Scraping: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
