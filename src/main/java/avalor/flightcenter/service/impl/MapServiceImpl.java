package avalor.flightcenter.service.impl;

import avalor.flightcenter.service.MapService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapServiceImpl implements MapService {
    private final List<List<Integer>> colorMatrix = new ArrayList<>();

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
    public List<List<String>> computeColors() {
        if (colorMatrix.isEmpty() || colorMatrix.getFirst().isEmpty()) {
            return new ArrayList<>();
        }
        int rows = colorMatrix.size();
        int cols = colorMatrix.getFirst().size();

        List<List<String>> colors = new ArrayList<>(rows);
        for (List<Integer> row : colorMatrix) {
            List<String> cRow = new ArrayList<>(cols);
            for (int v : row) {
                if (v == 0) {
                    cRow.add(PALETTE[v]); // special case for zero
                } else {
                    cRow.add(PALETTE[v]);
                }
            }
            colors.add(cRow);
        }
        return colors;
    }

    @Override
    public void setMatrix(int rows, int cols) {
        colorMatrix.clear();
        for (int i = 0; i < rows; i++) {
            colorMatrix.add(new ArrayList<>());
            for (int j = 0; j < rows; j++) {
                colorMatrix.get(i).add(0);
            }
        }
    }

    @Override
    public void setValue(int posX, int posY, int value) {
        colorMatrix.get(posY).set(posX, value);

    }
}
