package avalor.flightcenter.service.impl;

import avalor.flightcenter.service.MapService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapServiceImpl implements MapService {

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
    public List<List<String>> computeColors(List<List<Integer>> matrix) {
        int rows = matrix.size();
        int cols = matrix.getFirst().size();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (List<Integer> row : matrix) {
            for (int v : row) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        // Handle edge case: all equal values -> use middle of palette
        int range = Math.max(1, max - min);

        List<List<String>> colors = new ArrayList<>(rows);
        for (List<Integer> row : matrix) {
            List<String> cRow = new ArrayList<>(cols);
            for (int v : row) {
                int bucket = (int) Math.floor((double) (v - min) / range * (PALETTE.length - 1));
                if (bucket < 0) bucket = 0;
                if (bucket >= PALETTE.length) bucket = PALETTE.length - 1;
                cRow.add(PALETTE[bucket]);
            }
            colors.add(cRow);
        }
        return colors;
    }
}
