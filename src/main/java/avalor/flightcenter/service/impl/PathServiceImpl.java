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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PathServiceImpl implements PathService, Runnable {
    private final int MAX_DRONES = 10;
    private List<List<Position>> navigationPlanes = Collections.synchronizedList(new ArrayList<>());
    private final List<Position> visitedPositions = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentMap<String,List<Position>> travelledPath = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Position> dronePositions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Position> crtTargets = new ConcurrentHashMap<>();
    private MapService mapService = null;

    private List<String> droneNames = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentMap<String,List<Position>> dronePaths = new ConcurrentHashMap<>();


    @Override
    public void setMapService(MapService service) {
        mapService = service;
    }

    @Override
    public List<Position> getPath(String droneName) {
        // Minimal placeholder path: a simple straight line of positions
        List<Position> positions = new ArrayList<>();

        if (!dronePositions.containsKey(droneName)) {
            return positions;
        }
//        positions = PathCalculator.calculatePath(dronePositions.get(droneName), navigationPlanes, visitedPositions);
        return positions;
    }

    @Override
    public synchronized void setPosition(String droneName, Position newPosition) {
        if (!dronePositions.containsKey(droneName)) {
            addDrone(droneName, newPosition.getPosX(), newPosition.getPosY());
        } else {
            Position crtPos = dronePositions.get(droneName);
            // Mark the former position as unoccupied
            navigationPlanes.get(crtPos.getPosY()).get(crtPos.getPosX()).setOccupied(false);
            navigationPlanes.get(newPosition.getPosY()).get(newPosition.getPosX()).setOccupied(true);
            dronePositions.put(droneName, newPosition);
            travelledPath.get(droneName).add(newPosition);
            if (!visitedPositions.contains(newPosition)) {
                visitedPositions.add(newPosition);
            }
            mapService.setValue(newPosition.getPosX(), newPosition.getPosY(), (droneNames.indexOf(droneName) + 3));
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

        DecaySimulator.start(500, this);
    }

     public synchronized boolean addDrone(String droneName, int x, int y) {
        if (dronePositions.size() >= MAX_DRONES) {
            System.out.println("Max drones reached");
            return false; // Max drones reached
        }
        if (positionOutOfBounds(x, y)) {
            return false;
        }

        if (droneNames.contains(droneName)) {
            return false; // Drone with this name already exists
        } else {
            droneNames.add(droneName);
        }

        if (dronePositions.containsKey(droneName)) {
            return false; // Drone with this name already exists
        }
        Position dronePosition = Position.builder(navigationPlanes.get(y).get(x)).build();
        if (!visitedPositions.contains(dronePosition)) {
            visitedPositions.add(dronePosition);
        }
        navigationPlanes.get(y).get(x).setOccupied(true);
        dronePositions.put(droneName, dronePosition);
        travelledPath.put(droneName, new ArrayList<>());
        travelledPath.get(droneName).add(dronePosition);
        mapService.setValue(dronePosition.getPosX(), dronePosition.getPosY(), (droneNames.indexOf(droneName) + 3));
        System.out.println("Initializing drone " + droneName + " at position (" + x + "," + y + ")");

        // TODO: Remove this crap
//         dronePaths.put(droneName, new ArrayList<>());
//        dronePaths.put(droneName, PathCalculator.calculatePath(dronePosition, navigationPlanes, visitedPositions));
        return true;
    }

    private synchronized boolean positionOutOfBounds(int x, int y) {
        return false;
    }

    @Override
    public void run() {
//        System.out.println("Map decay running");
        synchronized (this) {
            for (String droneName : droneNames) {
                List<Position> dronePath = dronePaths.get(droneName);

                if (dronePath != null && !dronePath.isEmpty()) {
                    if (navigationPlanes.get(dronePath.getFirst().getPosY()).get(dronePath.getFirst().getPosX()).isOccupied()) {
//                        System.out.println("Drone " + droneName + " waiting at position: ("
//                                + dronePositions.get(droneName).getPosX() + ", " + dronePositions.get(droneName).getPosY() + ")");
                        continue;
                    }
                    Position nextPosition = dronePath.removeFirst();
                    setPosition(droneName, nextPosition);
                } else {
                    if (visitedPositions.size() < navigationPlanes.size() * navigationPlanes.getFirst().size()) {
                        // System.out.println("Calculating new path");
                        List<Position> exclusionList = new ArrayList<>(visitedPositions);
                        for (String targetName : crtTargets.keySet()) {
                            exclusionList.add(crtTargets.get(targetName));
                        }
                        List<Position> calculatedPath = PathCalculator.calculatePath(dronePositions.get(droneName), navigationPlanes, exclusionList);
                        if (!calculatedPath.isEmpty()) {
                            dronePaths.put(droneName, calculatedPath);
                            crtTargets.put(droneName, calculatedPath.getLast());
                        } else {
//                            System.out.println("No new target for drone " + droneName + ". Stopped at position: ("
//                                            + dronePositions.get(droneName).getPosX() + ", " + dronePositions.get(droneName).getPosY() + ")");
                            dronePaths.remove(droneName);
                            crtTargets.remove(droneName);
                            // Free position for make sures other drones are able to fly
                            navigationPlanes.get(dronePositions.get(droneName).getPosY()).get(dronePositions.get(droneName).getPosX()).setOccupied(false);
                        }
//                        System.out.println("Crt targets size: " + crtTargets.size());
                    } else {
                        System.out.println("Target reached; start over");
                        visitedPositions.clear();
                        travelledPath.get(droneName).clear();
                        mapService.setMatrix(navigationPlanes.getFirst().size(), navigationPlanes.size());

                        setPosition(droneName, dronePositions.get(droneName));
                    }
                }
            }
        }
    }
}
