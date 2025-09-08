package avalor.flightcenter.controller;
import avalor.flightcenter.service.MapService;
import avalor.flightcenter.service.PathService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/map")
public class MapController {

    private final MapService mapService;
    private final PathService pathService;

    public MapController(MapService mapService, PathService pathService) {
        this.mapService = mapService;
        this.pathService = pathService;
        pathService.setMapService(mapService);
    }

    private static final int MAX_CELLS_FOR_FULL_RENDER = 200_000; // safeguard to avoid 1M+ DOM nodes

    @GetMapping
    public String uploadForm() {
        return "map";
    }

    // Returns latest computed colors for the last rendered matrix (if any)
    @GetMapping(value = "/colors", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<List<String>> getLatestColors() {
        List<List<String>> local = mapService.getColors();
        if (local == null || local.isEmpty()) {
            return List.of();
        }
        return local;
    }

    @PostMapping
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please select a non-empty .txt file containing a numeric matrix.");
            return "map";
        }
        try {
            List<List<Integer>> matrix = parseMatrix(file);
            int rows = matrix.size();
            int cols = matrix.getFirst().size();

            int cells = rows * cols;
            int sampleStep = 1;
            boolean sampled = false;
            if (cells > MAX_CELLS_FOR_FULL_RENDER) {
                // Compute a sampling step to keep rendered cells under threshold
                double factor = Math.ceil(Math.sqrt((double) cells / MAX_CELLS_FOR_FULL_RENDER));
                sampleStep = Math.max(1, (int) factor);
                sampled = sampleStep > 1;
            }

            List<List<Integer>> toRender;
            if (sampled) {
                toRender = downsample(matrix, sampleStep);
            } else {
                toRender = matrix;
            }

            // Initialize the navigation planes
            mapService.initColorMatrix(toRender.size(), toRender.getFirst().size());

            pathService.setNavigationPlanes(toRender);
//            pathService.addDrone("Drone1", new avalor.flightcenter.domain.Position(0, 0, matrix.get(0).get(0)));
//            pathService.addDrone("Drone2", new avalor.flightcenter.domain.Position(1, 0, matrix.get(1).get(0)));
//            pathService.addDrone("Drone3", new avalor.flightcenter.domain.Position(2, 0, matrix.get(2).get(0)));
//            pathService.addDrone("Drone4", new avalor.flightcenter.domain.Position(3, 0, matrix.get(3).get(0)));
//            pathService.addDrone("Drone5", new avalor.flightcenter.domain.Position(4, 0, matrix.get(4).get(0)));

            model.addAttribute("matrix", toRender);
            model.addAttribute("colors", mapService.getColors());
            model.addAttribute("maxVal", findMax(matrix));
            model.addAttribute("minVal", findMin(matrix));
            model.addAttribute("rows", rows);
            model.addAttribute("cols", cols);
            model.addAttribute("renderedRows", toRender.size());
            model.addAttribute("renderedCols", toRender.getFirst().size());
            model.addAttribute("sampleStep", sampleStep);
            model.addAttribute("sampled", sampled);
            return "map";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "map";
        } catch (IOException e) {
            model.addAttribute("error", "Failed to read file: " + e.getMessage());
            return "map";
        }
    }

    private List<List<Integer>> parseMatrix(MultipartFile file) throws IOException {
        List<List<Integer>> matrix = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int expectedCols = -1;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // skip blank lines
                }
                String[] parts = line.split("\\s+");
                List<Integer> row = new ArrayList<>(parts.length);
                for (String p : parts) {
                    if (!StringUtils.hasText(p)) continue;
                    try {
                        row.add(Integer.parseInt(p));
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("Invalid number at line " + lineNo + ": '" + p + "'");
                    }
                }
                if (row.isEmpty()) continue;
                if (expectedCols == -1) {
                    expectedCols = row.size();
                } else if (row.size() != expectedCols) {
                    throw new IllegalArgumentException("Inconsistent row length at line " + lineNo + ". Expected " + expectedCols + ", got " + row.size());
                }
                matrix.add(row);
            }
        }
        if (matrix.isEmpty()) {
            throw new IllegalArgumentException("The provided file does not contain any numbers.");
        }
        return matrix;
    }

    private List<List<Integer>> downsample(List<List<Integer>> matrix, int step) {
        List<List<Integer>> sampled = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i += step) {
            List<Integer> row = matrix.get(i);
            List<Integer> outRow = new ArrayList<>();
            for (int j = 0; j < row.size(); j += step) {
                outRow.add(row.get(j));
            }
            sampled.add(outRow);
        }
        return sampled;
    }

    private int findMax(List<List<Integer>> matrix) {
        int max = Integer.MIN_VALUE;
        for (List<Integer> row : matrix) for (int v : row) if (v > max) max = v;
        return max;
    }

    private int findMin(List<List<Integer>> matrix) {
        int min = Integer.MAX_VALUE;
        for (List<Integer> row : matrix) for (int v : row) if (v < min) min = v;
        return min;
    }
}
