package avalor.flightcenter.service.impl;

import avalor.flightcenter.service.MapService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapServiceImpl implements MapService {
    private final List<List<String>> colorMatrix = new ArrayList<>();

    private static final String[] PALETTE = new String[]{
            "#f7f7f7", // 0 - background / zero
            "#e41a1c", // 1 - red
            "#377eb8", // 2 - blue
            "#4daf4a", // 3 - green
            "#984ea3", // 4 - purple
            "#ff7f00", // 5 - orange
            "#ffff33", // 6 - yellow
            "#a65628", // 7 - brown
            "#f781bf", // 8 - pink
            "#17becf"  // 9 - cyan
    };

    @Override
    public synchronized List<List<String>> getColors() {
        if (colorMatrix.isEmpty()) {
            return List.of();
        }
        List<List<String>> copy = new ArrayList<>(colorMatrix.size());
        for (List<String> row : colorMatrix) {
            copy.add(List.copyOf(row));
        }
        return List.copyOf(copy);
    }

    @Override
    public synchronized void init(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            colorMatrix.clear();
            return;
        }
        colorMatrix.clear();
        for (int i = 0; i < rows; i++) {
            List<String> row = new ArrayList<>(cols);
            for (int j = 0; j < cols; j++) {
                row.add(PALETTE[0]);
            }
            colorMatrix.add(row);
        }
    }

    @Override
    public synchronized void setColor(int posX, int posY, int value) {
        if (colorMatrix.isEmpty()) {
            return;
        }
        if (posY < 0 || posY >= colorMatrix.size()) {
            return;
        }
        List<String> row = colorMatrix.get(posY);
        if (row == null || posX < 0 || posX >= row.size()) {
            return;
        }
        String color = PALETTE[Math.floorMod(value, PALETTE.length)];
        row.set(posX, color);
    }

    @Override
    public synchronized void reset() {
        colorMatrix.clear();
    }

    @Override
    public void clear() {
        if (colorMatrix.isEmpty()) {
            return;
        }
        for (List<String> row : colorMatrix) {
            row.replaceAll(s -> PALETTE[0]);
        }
    }
}
