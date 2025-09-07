package avalor.flightcenter.api.dto;

import java.util.List;

public class BasicPathResponse {
    private String droneName;
    private List<PositionDTO> positions;

    public BasicPathResponse() {}

    public BasicPathResponse(String droneName, List<PositionDTO> positions) {
        this.droneName = droneName;
        this.positions = positions;
    }

    public String getDroneName() {
        return droneName;
    }

    public void setDroneName(String droneName) {
        this.droneName = droneName;
    }

    public List<PositionDTO> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionDTO> positions) {
        this.positions = positions;
    }
}
