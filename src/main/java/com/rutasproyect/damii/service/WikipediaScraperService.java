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
                if (rows.isEmpty()) continue;
                
                Elements headers = rows.get(0).select("th");
                int codeIdx = -1, aliasIdx = -1, originIdx = -1, destIdx = -1, companyIdx = -1;

                for (int j = 0; j < headers.size(); j++) {
                    String hText = headers.get(j).text().toLowerCase();
                    if (hText.contains("ruta") || hText.contains("código") || hText.equals("línea")) codeIdx = j;
                    else if (hText.contains("alias") || hText.contains("seudónimo")) aliasIdx = j;
                    else if (hText.contains("inicial") || hText.contains("origen") || hText.contains("sur")) originIdx = j;
                    else if (hText.contains("final") || hText.contains("terminal") || hText.contains("norte")) destIdx = j;
                    else if (hText.contains("empresa") || hText.contains("operadora")) companyIdx = j;
                }

                // Fallbacks si la tabla no tiene los nombres exactos
                if (codeIdx == -1) codeIdx = 0;
                if (companyIdx == -1) companyIdx = headers.size() > 5 ? 5 : 1;
                if (originIdx == -1) originIdx = headers.size() > 5 ? 3 : 2;
                if (destIdx == -1) destIdx = headers.size() > 5 ? 4 : 3;
                if (aliasIdx == -1 && headers.size() > 5) aliasIdx = 2;

                for (int i = 1; i < rows.size(); i++) { // Evitar el thead
                    Elements cols = rows.get(i).select("td, th");
                    if (cols.size() >= 4) {
                        String code = codeIdx < cols.size() ? cols.get(codeIdx).text().trim() : "";
                        String company = companyIdx < cols.size() && companyIdx >= 0 ? cols.get(companyIdx).text().trim() : "";
                        String origin = originIdx < cols.size() && originIdx >= 0 ? cols.get(originIdx).text().trim() : "";
                        String destination = destIdx < cols.size() && destIdx >= 0 ? cols.get(destIdx).text().trim() : "";
                        String alias = aliasIdx < cols.size() && aliasIdx >= 0 ? cols.get(aliasIdx).text().trim() : "";

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
                                route.setId((int) (Math.random() * 100000)); // ID temporal
                                route.setRouteRef(code);
                            }

                            // Nombre mejorado para contexto
                            String routeName = alias.isEmpty() ? company : company + " (" + alias + ")";
                            if (routeName.isEmpty()) routeName = origin + " - " + destination;
                            if (routeName.isEmpty()) routeName = code;

                            route.setName(routeName);
                            route.setNetwork(company);
                            route.setOrigin(origin);
                            route.setDestination(destination);
                            route.setStatus("ACTIVA");

                            routeRepository.save(route);
                        }
                    }
                    current++;
                    if (current % 10 == 0) {
                        optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo(
                                "RUNNING", current, totalRoutes, "Sincronizando ruta..."));
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
