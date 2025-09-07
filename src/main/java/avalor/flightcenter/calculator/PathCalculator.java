package avalor.flightcenter.calculator;

import avalor.flightcenter.domain.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathCalculator {
    private final List<List<Position>> navigationPlanes;
    private List<Position> visitedPositions = new ArrayList<>();
    private Map<String,List<Position>> travelledPath = new HashMap<>();

    public PathCalculator(List<List<Position>> navigationPlanes) {
        this.navigationPlanes = navigationPlanes;
    }

    public void reset() {
        if (visitedPositions != null) {
            visitedPositions.clear();
        }
        if (travelledPath != null) {
            travelledPath.clear();
        }
    }

    public boolean addDrone(String droneName) {
        if (droneName != null && !travelledPath.containsKey(droneName)) {
            travelledPath.put(droneName, new ArrayList<>());
            return true;
        }
        return false;
    }

    public boolean removeDrone(String droneName) {
        if (droneName != null && travelledPath.containsKey(droneName)) {
            travelledPath.remove(droneName);
            return true;
        }
        return false;
    }

    private List<Position> getMostValuableTargets(List<Position> exclude) {
        List<Position> allPositions = new ArrayList<>();
        for (List<Position> row : navigationPlanes) {
            for (Position position : row) {
                if (exclude != null && exclude.contains(position)) {
                    continue;
                }
                allPositions.add(Position.builder(position).build());
            }
        }
        if (allPositions.isEmpty()) {
            return allPositions;
        }
        allPositions.sort((a, b) -> Integer.compare((b.getValue() + b.getDecay()), (a.getValue() + a.getDecay())));
        final int maxValue = allPositions.getFirst().getValue();
        allPositions = allPositions.stream().filter(n -> n.getValue() == maxValue).toList();
        return allPositions;
    }

    private Position getClosestTarget(Position crtPosition, List<Position> positions, List<Position> exclude) {
        if (crtPosition == null || positions == null || positions.isEmpty()) {
            return null;
        }
        Position closestPosition = null;
        double minDistance = Double.MAX_VALUE;
        for (Position position : positions) {
            if (exclude != null && exclude.contains(position)) {
                continue;
            }
            double distance = calculateDistance(crtPosition, position);
            if (distance < minDistance) {
                minDistance = distance;
                closestPosition = position;
            }
        }
        return closestPosition;
    }

    private static double calculateDistance(Position source, Position destination) {
        if (source == null || destination == null) {
            return Double.MAX_VALUE;
        }
        return Math.hypot(source.getPosX() - destination.getPosX(), source.getPosY() - destination.getPosY());
    }
}
