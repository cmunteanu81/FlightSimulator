package avalor.flightcenter.service.impl;

import avalor.flightcenter.api.dto.BasicPathResponse;
import avalor.flightcenter.api.dto.PositionDTO;
import avalor.flightcenter.calculator.PathCalculator;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.MapService;
import avalor.flightcenter.service.PathService;
import avalor.flightcenter.utils.DecaySimulator;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PathServiceImpl implements PathService, Runnable {
    private final int MAX_DRONES = 3;
    private List<List<Position>> navigationPlanes = new ArrayList<>();
    private final List<Position> visitedPositions = new ArrayList<>();
    private final Map<String,List<Position>> travelledPath = new HashMap<>();
    private final Map<String, Position> dronePositions = new HashMap<>();
    private MapService mapService = null;


    private List<Position> dronePath;

    @Override
    public List<Position> getPath(String droneName) {
        // Minimal placeholder path: a simple straight line of positions
        List<Position> positions = new ArrayList<>();

        if (!dronePositions.containsKey(droneName)) {
            return positions;
        }
        positions = PathCalculator.calculatePath(dronePositions.get(droneName), navigationPlanes, visitedPositions);
        return positions;
    }

    @Override
    public void setPosition(String droneName, Position newPosition) {
        if (!dronePositions.containsKey(droneName)) {
            addDrone(droneName, newPosition.getPosX(), newPosition.getPosY());
        } else {
            dronePositions.put(droneName, newPosition);
            travelledPath.get(droneName).add(newPosition);
            if (!visitedPositions.contains(newPosition)) {
                visitedPositions.add(newPosition);
            }
            mapService.setValue(newPosition.getPosX(), newPosition.getPosY(), 4);
        }
    }

    @Override
    public void setNavigationPlanes(List<List<Integer>> valueMatrix) {
        for (int i = 0; i < valueMatrix.size(); i++) {
            List<Integer> row = valueMatrix.get(i);
            List<Position> positionRow = new ArrayList<>();
            for (int j = 0; j < row.size(); j++) {
                Integer value = row.get(j);
                positionRow.add(new Position(j, i, value));
            }
            navigationPlanes.add(positionRow);
        }

        DecaySimulator.start(200, this);
    }

    @Override
    public void setMapService(MapService service) {
        mapService = service;
    }

    @Override
    public void run() {
//        System.out.println("Map decay running");
        if (dronePath != null && !dronePath.isEmpty()) {
            Position nextPosition = dronePath.removeFirst();
            for (String droneName : dronePositions.keySet()) {
                setPosition(droneName, nextPosition);
            }
        } else {
            if (visitedPositions.size() < navigationPlanes.size() * navigationPlanes.getFirst().size()) {
//                System.out.println("Calculating new path");
                dronePath = PathCalculator.calculatePath(dronePositions.get("Drone1"), navigationPlanes, visitedPositions);
            } else {
                System.out.println("Target reached; start over");
                visitedPositions.clear();
                travelledPath.get("Drone1").clear();
                mapService.setMatrix(navigationPlanes.getFirst().size(), navigationPlanes.size());

                setPosition("Drone1", dronePositions.get("Drone1"));
            }
        }
    }

    public boolean addDrone(String droneName, int x, int y) {
        if (dronePositions.size() >= MAX_DRONES) {
            System.out.println("Max drones reached");
            return false; // Max drones reached
        }
        if (positionOutOfBounds(x, y)) {
            return false;
        }

        if (dronePositions.containsKey(droneName)) {
            return false; // Drone with this name already exists
        }
        Position dronePosition = Position.builder(navigationPlanes.get(y).get(x)).build();
        if (!visitedPositions.contains(dronePosition)) {
            visitedPositions.add(dronePosition);
        }
        dronePositions.put(droneName, dronePosition);
        travelledPath.put(droneName, new ArrayList<>());
        travelledPath.get(droneName).add(dronePosition);
        System.out.println("Initializing drone " + droneName + " at position (" + x + "," + y + ")");
        // TODO: Remove this crap
        dronePath = PathCalculator.calculatePath(dronePosition, navigationPlanes, visitedPositions);
        return true;
    }

    private boolean positionOutOfBounds(int x, int y) {
        return false;
    }
}
