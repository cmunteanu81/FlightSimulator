package avalor.flightcenter.calculator;

import avalor.flightcenter.domain.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathCalculator {

    private PathCalculator() {}

    public static List<Position> calculatePath(Position startPosition, List<List<Position>> navigationPlanes, List<Position> visitedPositions) {
        if (startPosition == null || navigationPlanes == null || navigationPlanes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Position> path = new ArrayList<>();
        Position crtPosition = Position.builder(startPosition).build();
        // Get the closest position from the most valuable targets available
        Position nextTarget = getClosestTarget(crtPosition, getMostValuableTargets(navigationPlanes, visitedPositions));
        if (nextTarget != null) {
//            System.out.println("New target acquired: (" + nextTarget.getPosX() + "," + nextTarget.getPosY() + "); value: " + nextTarget.getValue());
        }

        while (crtPosition != null && !crtPosition.equals(nextTarget) ) {
            // Get the next node towards the target
            crtPosition = getNextPositionInPath(navigationPlanes, crtPosition, nextTarget);
            path.add(crtPosition);
        }

        return path;
    }

    public static List<Position> getMostValuableTargets(List<List<Position>> navigationPlanes, List<Position> exclude) {
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
        allPositions.sort(
                java.util.Comparator
                        .comparingInt((Position p) -> p.getValue() + p.getDecay())
                        .thenComparingInt(Position::getValue)
                        .reversed()
        );
        final int maxValue = allPositions.getFirst().getValue();
        allPositions = allPositions.stream().filter(n -> n.getValue() == maxValue).toList();
        return allPositions;
    }

    private static Position getClosestTarget(Position crtPosition, List<Position> positions) {
        if (crtPosition == null || positions == null || positions.isEmpty()) {
            return null;
        }
        Position closestPosition = null;
        double minDistance = Double.MAX_VALUE;
        for (Position position : positions) {
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

    private static Position getNextPositionInPath(List<List<Position>> navigationPlanes, Position startPosition, Position targetPosition) {
        if (startPosition == null || targetPosition == null) {
            System.out.println("Start or target position is null.");
            return null;
        }

        if (navigationPlanes == null || navigationPlanes.isEmpty()) {
            System.out.println("Navigation planes incorrectly initialized");
            return null;
        }
        int widthBoundary = navigationPlanes.getFirst().size();
        int heightBoundary = navigationPlanes.size();

        if (startPosition.getPosX() >= widthBoundary || startPosition.getPosY() >= heightBoundary || targetPosition.getPosX() >= widthBoundary || targetPosition.getPosY() >= heightBoundary) {
            System.out.println("Start or target position is out of bounds.");
            return null;
        }

        int nextX = startPosition.getPosX();
        int nextY = startPosition.getPosY();
        int targetX = targetPosition.getPosX();
        int targetY = targetPosition.getPosY();
        String direction;

        if (nextX == targetX && nextY == targetY) {
            System.out.println("Already at target location( " + nextX + "," + nextY + " )");
            return Position.builder(navigationPlanes.get(nextY).get(nextX)).build();
        } else if (nextX != targetX && nextY != targetY) {
            direction = "DIAGONALLY";
        } else if (nextX != targetX) {
            direction = nextX > targetX ? "LEFT" : "RIGHT";
        } else direction = nextY > targetY ? "UP" : "DOWN";

        switch (direction) {
            case "DIAGONALLY":
                nextX += (nextX > targetX) ? -1 : 1;
                nextY += (nextY > targetY) ? -1 : 1;
                break;
            case "DOWN":
                nextY += (nextY < heightBoundary - 1) ? 1 : 0;
                break;
            case "UP":
                nextY += (nextY > 0) ? -1 : 0;
                break;
            case "LEFT":
                nextX += (nextX > 0) ? -1 : 0;
                break;
            case "RIGHT":
                nextX += (nextX < widthBoundary - 1) ? 1 : 0;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + direction);
        }

        return Position.builder(navigationPlanes.get(nextY).get(nextX)).build();
    }


}
