package avalor.flightcenter.controller;

import avalor.flightcenter.api.dto.BasicPathResponse;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.api.mapper.PositionMapper;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.PathService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/path")
public class PathController {

    private final PathService pathService;
    private final PositionMapper positionMapper;

    public PathController(PathService pathService, PositionMapper positionMapper) {
        this.pathService = pathService;
        this.positionMapper = positionMapper;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicPathResponse getPath(@RequestParam("droneName") String droneName) {
        List<Position> positions = pathService.getPath(droneName);
        List<PositionDTO> dtoList = positionMapper.toDtoList(positions);
        return new BasicPathResponse(droneName, dtoList);
    }
}
