package avalor.flightcenter.api.mapper;

import avalor.flightcenter.api.dto.DroneDTO;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.domain.Drone;
import avalor.flightcenter.domain.Position;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PositionMapper.class})
public interface DroneMapper {

    // Domain -> DTO
    @Mappings({
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "currentLocation", source = "currentPosition"),
            @Mapping(target = "targetLocation", source = "targetPosition")
    })
    DroneDTO toDto(Drone drone);

    List<DroneDTO> toDtoList(List<Drone> drones);

    // DTO -> Domain
    @Mappings({
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "currentPosition", source = "currentLocation"),
            @Mapping(target = "targetPosition", source = "targetLocation"),
            @Mapping(target = "targetPath", ignore = true),
            @Mapping(target = "historyPath", ignore = true),
            @Mapping(target = "droneState", ignore = true)
    })
    Drone toDomain(DroneDTO dto);

    List<Drone> toDomainList(List<DroneDTO> dtos);
}
