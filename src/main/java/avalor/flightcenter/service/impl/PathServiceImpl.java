package avalor.flightcenter.service.impl;

import avalor.flightcenter.api.dto.BasicPathResponse;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.PathService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PathServiceImpl implements PathService {
    @Override
    public List<Position> getPath(String droneName) {
        // Minimal placeholder path: a simple straight line of positions
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            positions.add(new Position(i, i, 0));
        }
        return positions;
    }
}
