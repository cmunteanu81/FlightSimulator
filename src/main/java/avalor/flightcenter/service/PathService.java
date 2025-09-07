package avalor.flightcenter.service;

import avalor.flightcenter.api.dto.BasicPathResponse;
import avalor.flightcenter.domain.Position;

import java.util.List;

public interface PathService {
    List<Position> getPath(String droneName);
}
