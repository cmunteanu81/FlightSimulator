package avalor.flightcenter.api.dto;

public class DroneDTO {
    private String name;
    private PositionDTO currentLocation;
    private PositionDTO targetLocation; // optional

    public DroneDTO() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PositionDTO getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(PositionDTO currentLocation) {
        this.currentLocation = currentLocation;
    }

    public PositionDTO getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(PositionDTO targetLocation) {
        this.targetLocation = targetLocation;
    }
}