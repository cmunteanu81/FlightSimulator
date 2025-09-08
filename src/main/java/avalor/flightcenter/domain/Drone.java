package avalor.flightcenter.domain;

import jakarta.validation.constraints.NotNull;

import java.util.LinkedList;
import java.util.List;

public class Drone {
    @NotNull
    private final String name;
    private Position currentPosition;
    private Position targetPosition;
    private LinkedList<Position> targetPath;
    private LinkedList<Position> historyPath;
    private DroneState droneState;


    public Drone(String name, Position initialPosition) {
        this.name = name;
        this.currentPosition = Position.builder(initialPosition).build();
        this.targetPosition = null;
        targetPath = null;
        historyPath = null;
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
        this.targetPath = new LinkedList<>(targetPath);
    }

    public boolean isMoving() {
        return droneState == DroneState.MOVING;
    }

    public boolean isTargetReached() {
        return currentPosition.equals(targetPosition);
    }

    public void clearHistoryPath() {
        if (historyPath != null) {
            historyPath.clear();
        }
    }

    public DroneState getDroneState() {
        return droneState;
    }

    public void setDroneState(DroneState droneState) {
        this.droneState = droneState;
    }

    public LinkedList<Position> getCurrentPath() {
        return targetPath;
    }

    public Position getNextPossibleMove() {
        if (targetPath == null || targetPath.isEmpty()) {
            return null;
        }

        return targetPath.peekFirst();
    }

    public Position moveToNextPosition() {
        if (targetPath == null || targetPath.isEmpty()) {
            return null;
        }

        Position nextPosition = targetPath.pollFirst();
        if (nextPosition != null) {
            if (historyPath == null) {
                historyPath = new LinkedList<>();
            }
            historyPath.add(currentPosition);
            currentPosition = nextPosition;
            return currentPosition;
        } else {
            return null;
        }
    }

    public Position goBack() {
        if (historyPath == null || historyPath.isEmpty()) {
            return null;
        }
        Position lastPosition = historyPath.pollLast();
        if (lastPosition != null) {
            currentPosition = lastPosition;
            return currentPosition;
        }
        return null;
    }

    public List<Position> getHistoryPath() {
        return historyPath;
    }

    public void clearHistory() {
        if (historyPath != null) {
            historyPath.clear();
        }
    }
}
