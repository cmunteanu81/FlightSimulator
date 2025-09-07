package avalor.flightcenter.controller;

import avalor.flightcenter.api.dto.BasicLocationRequest;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.api.mapper.PositionMapper;
import avalor.flightcenter.domain.Position;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/location")
public class LocationController {

    private final PositionMapper positionMapper;

    public LocationController(PositionMapper positionMapper) {
        this.positionMapper = positionMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicLocationRequest postLocation(@RequestBody BasicLocationRequest request) {
        // Validate minimal requirements
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (request.getDroneName() == null || request.getDroneName().isBlank()) {
            throw new IllegalArgumentException("droneName must not be null or blank");
        }
        PositionDTO current = request.getCurrentLocation();
        if (current == null) {
            throw new IllegalArgumentException("currentLocation must not be null");
        }
        // targetLocation may be null by requirement; map if present just to exercise the mapper
        Position currentDomain = positionMapper.toDomain(current);
        Position targetDomain = request.getTargetLocation() != null ? positionMapper.toDomain(request.getTargetLocation()) : null;
        // For now, this endpoint echoes the request back. In the future, you can route to a service.
        return request;
    }
}
