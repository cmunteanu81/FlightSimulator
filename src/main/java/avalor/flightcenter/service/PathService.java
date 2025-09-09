package avalor.flightcenter.service;

import avalor.flightcenter.domain.Drone;
import avalor.flightcenter.domain.Position;

import java.util.List;

public interface PathService {
    void setMapService(MapService service);
    void init(List<List<Integer>> navigationMatrix);
    void reset();
    Drone findDroneByName(String name);
    Drone addDrone(String droneName, Position initialPosition);
    List<Position> getPathForDrone(String droneName);
    void recordDronePosition(String droneName, Position newPosition);
}
