package avalor.flightcenter.domain;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Drone {
    @NotNull
    private final String name;
    private final List<Position> historyPath = Collections.synchronizedList(new ArrayList<>());
    private Position currentPosition;
    private Position targetPosition;
    private List<Position> targetPath;


    public Drone(String name, Position initialPosition) {
        this.name = name;
        this.currentPosition = Position.builder(initialPosition).build();
        this.targetPosition = null;
        targetPath = null;
    }

    public String getName() {
        return name;
    }

    public synchronized Position getCurrentPosition() {
        return currentPosition;
    }

    public synchronized void setCurrentPosition(Position currentPosition) {
        this.currentPosition = currentPosition;
    }

    public synchronized Position getTargetPosition() {
        return targetPosition;
    }

    public synchronized void setTargetPosition(Position targetPosition) {
        this.targetPosition = Position.builder(targetPosition).build();
    }

    public synchronized List<Position> getTargetPath() {
        return targetPath;
    }

    public synchronized void setTargetPath(List<Position> targetPath) {
        this.targetPath = targetPath;
    }


    public synchronized List<Position> getHistoryPath() {
        return historyPath;
    }

    public synchronized void clearHistoryPath() {
        historyPath.clear();
    }

    public synchronized boolean isTargetReached() {
        return (targetPosition == null || currentPosition.equals(targetPosition));
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
}
