package avalor.flightcenter.service;

import avalor.flightcenter.api.dto.BasicPathResponse;
import avalor.flightcenter.domain.Drone;
import avalor.flightcenter.domain.Position;

import java.util.List;

public interface PathService {
    List<Position> getPath(String droneName);
    Drone findDroneByName(String name);
    Drone addDrone(String droneName, Position initialPosition);
    void setPosition(String droneName, Position newPosition);
    void setNavigationPlanes(List<List<Integer>> valueMatrix);
    void setMapService(MapService service);
}
