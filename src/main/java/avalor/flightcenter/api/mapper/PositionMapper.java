package avalor.flightcenter.api.mapper;

import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.domain.Position;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PositionMapper {
    // Domain -> DTO: map posX->x and posY->y
    @Mapping(target = "x", source = "posX")
    @Mapping(target = "y", source = "posY")
    @Mapping(target = "value", source = "value")
    PositionDTO toDto(Position position);

    // DTO -> Domain: we don't have value/decay/occupied in DTO; initialize value to 0
    @Mapping(target = "posX", source = "x")
    @Mapping(target = "posY", source = "y")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "decay", ignore = true)
    @Mapping(target = "occupied", ignore = true)
    Position toDomain(PositionDTO dto);

    List<PositionDTO> toDtoList(List<Position> positions);
    List<Position> toDomainList(List<PositionDTO> dtos);
}
