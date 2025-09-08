package avalor.flightcenter.service;

import java.util.List;

public interface MapService {
    /**
     * Computes a matrix of CSS background colors (e.g., "#ffffff") for the given numeric matrix.
     * The returned list has the same shape as input.
     */
    List<List<String>> computeColors();
    void setMatrix(int rows, int cols);
    void setValue(int posX, int posY, int value);
}
