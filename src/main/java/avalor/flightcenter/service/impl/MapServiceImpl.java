package avalor.flightcenter.service.impl;

import avalor.flightcenter.service.MapService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapServiceImpl implements MapService {
    private final List<List<Integer>> colorMatrix = new ArrayList<>();

    private static final String[] PALETTE = new String[]{
            "#f0f0f0", // 0
            "#e1f5fe", // 1
            "#b3e5fc", // 2
            "#81d4fa", // 3
            "#4fc3f7", // 4
            "#ffecb3", // 5
            "#ffe082", // 6
            "#ffcc80", // 7
            "#ffab91", // 8
            "#ef9a9a"  // 9
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
                    cRow.add(PALETTE[0]); // special case for zero
                } else {
                    cRow.add(PALETTE[9]);
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
