package avalor.flightcenter.controller;

import avalor.flightcenter.api.dto.BasicInitRequest;
import avalor.flightcenter.api.dto.DroneDTO;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.api.mapper.PositionMapper;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.PathService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/init")
public class InitController {

    private final PathService pathService;
    private final PositionMapper positionMapper;

    public InitController(PathService pathService, PositionMapper positionMapper) {
        this.pathService = pathService;
        this.positionMapper = positionMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> init(@RequestBody BasicInitRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        List<DroneDTO> drones = request.getDrones();
        if (drones == null || drones.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        for (DroneDTO d : drones) {
            if (d == null) continue;
            String name = d.getName();
            PositionDTO current = d.getCurrentLocation();
            if (name == null || name.isBlank() || current == null) {
                continue; // skip invalid entries
            }
            Position currentPos = positionMapper.toDomain(current);
            if (pathService.addDrone(name, currentPos) == null) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok().build();
    }
}
