package avalor.flightcenter.api.dto;

public class BasicLocationRequest {
    private String droneName;
    private PositionDTO currentLocation;
    private PositionDTO targetLocation; // can be null

    public BasicLocationRequest() {}

    public String getDroneName() {
        return droneName;
    }

    public void setDroneName(String droneName) {
        this.droneName = droneName;
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
