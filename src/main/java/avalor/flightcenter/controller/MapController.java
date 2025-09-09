package avalor.flightcenter.controller;

import avalor.flightcenter.service.MapService;
import avalor.flightcenter.service.PathService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
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

    private static final int MAX_CELLS_FOR_FULL_RENDER = 200_000; // safeguard to avoid 1M+ DOM nodes
    private final MapService mapService;
    private final PathService pathService;

    public MapController(MapService mapService, PathService pathService) {
        this.mapService = mapService;
        this.pathService = pathService;
        pathService.setMapService(mapService);
    }

    @GetMapping
    public String uploadForm() {
        return "map";
    }

    @PostMapping("/reset")
    public String reset(Model model) {
        // Reset the services
        pathService.reset();
        mapService.reset();
        // Clear the map attributes, so they can be re-initialized on the next upload
        model.addAttribute("matrix", null);
        model.addAttribute("colors", null);
        model.addAttribute("rows", null);
        model.addAttribute("cols", null);
        model.addAttribute("renderedRows", null);
        model.addAttribute("renderedCols", null);
        model.addAttribute("sampleStep", null);
        model.addAttribute("sampled", null);
        model.addAttribute("maxVal", null);
        model.addAttribute("minVal", null);
        return "redirect:/map";
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

            // Initialize the services with the new data matrix
            mapService.init(toRender.size(), toRender.getFirst().size());
            pathService.init(toRender);

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
