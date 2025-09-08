package avalor.flightcenter.domain;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Drone {
    @NotNull
    private final String name;
    private Position currentPosition;
    private Position targetPosition;
    private List<Position> targetPath;
    private final List<Position> historyPath = Collections.synchronizedList(new ArrayList<>());
    private DroneState droneState;


    public Drone(String name, Position initialPosition) {
        this.name = name;
        this.currentPosition = Position.builder(initialPosition).build();
        this.targetPosition = null;
        targetPath = null;
        droneState = DroneState.IDLE;
    }

    public String getName() {
        return name;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Position currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Position getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Position targetPosition) {
        this.targetPosition = targetPosition;
    }

    public void setTargetPath(List<Position> targetPath) {
            this.targetPath = targetPath;
    }

    public boolean isMoving() {
        return droneState == DroneState.MOVING;
    }

    public boolean isTargetReached() {
        return currentPosition.equals(targetPosition);
    }

    public DroneState getDroneState() {
        return droneState;
    }

    public void setDroneState(DroneState droneState) {
        this.droneState = droneState;
    }

    public List<Position> getTargetPath() {
        return targetPath;
    }

    public synchronized Position getNextPossibleMove() {
        if (targetPath == null || targetPath.isEmpty()) {
            return null;
        }
        return targetPath.getFirst();
    }

    public synchronized Position moveToNextPosition() {
        if (targetPath == null || targetPath.isEmpty()) {
            return null;
        }
        Position nextPosition = targetPath.removeFirst();
        if (nextPosition != null) {
            historyPath.add(currentPosition);
            currentPosition = nextPosition;
            return currentPosition;
        } else {
            return null;
        }
    }

    public synchronized Position goBack() {
        if (historyPath.isEmpty()) {
            return null;
        }
        Position lastPosition = historyPath.removeLast();
        if (lastPosition != null) {
            currentPosition = lastPosition;
            return currentPosition;
        }
        return null;
    }

    public List<Position> getHistoryPath() {
        return historyPath;
    }

    public synchronized void clearHistory() {
            historyPath.clear();
    }
}
