package avalor.flightcenter.api.dto;

import java.util.List;

public class BasicInitRequest {
    private List<DroneDTO> drones;

    public BasicInitRequest() {}

    public List<DroneDTO> getDrones() {
        return drones;
    }

    public void setDrones(List<DroneDTO> drones) {
        this.drones = drones;
    }
}