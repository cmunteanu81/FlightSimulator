package avalor.flightcenter.service.impl;

import avalor.flightcenter.calculator.PathCalculator;
import avalor.flightcenter.domain.Drone;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.MapService;
import avalor.flightcenter.service.PathService;
import avalor.flightcenter.utils.DecaySimulator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PathServiceImpl implements PathService, Runnable {
    private final int MAX_DRONES = 10;
    private List<List<Position>> navigationPlanes = Collections.synchronizedList(new ArrayList<>());
    private final List<Position> visitedPositions = Collections.synchronizedList(new ArrayList<>());
    private final List<Drone> activeDrones = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentMap<String, Position> crtTargets = new ConcurrentHashMap<>();
    private MapService mapService = null;

    @Override
    public void setMapService(MapService service) {
        mapService = service;
    }

    @Override
    public List<Position> getPath(String droneName) {
        // Minimal placeholder path: a simple straight line of positions
        List<Position> positions = new ArrayList<>();

//        positions = PathCalculator.calculatePath(dronePositions.get(droneName), navigationPlanes, visitedPositions);
        return positions;
    }

    @Override
    public synchronized void setPosition(String droneName, Position newPosition) {
        if (droneName == null || newPosition == null) {
            return; //  Sanity check
        }

        if (positionOutOfBounds(newPosition.getPosX(), newPosition.getPosY())) {
            return; // Out of bounds position is discarded
        }
        Drone crtDrone = findDroneByName(droneName);
        // If drone does not exist, try to add it
        if (crtDrone != null) {
            // Free up the old space in the navigation plane
            Position oldPosition = crtDrone.getCurrentPosition();
            if (oldPosition != null) {
                navigationPlanes.get(crtDrone.getCurrentPosition().getPosY()).get(crtDrone.getCurrentPosition().getPosX()).setOccupied(false);
            }
            // Set the new position
            crtDrone.setCurrentPosition(Position.builder(newPosition).build());
            recordVisitedPosition(droneName, newPosition);
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

    @Override
    public synchronized Drone findDroneByName(String name) {
        for (Drone drone : activeDrones) {
            if (drone.getName().equals(name)) {
                return drone;
            }
        }
        return null;
    }

    @Override
     public synchronized Drone addDrone(String droneName, Position initialPos) {
        if (activeDrones.size() >= MAX_DRONES) {
            System.out.println("Max drones reached");
            return null; // Max drones reached
        }
        Drone newDrone = new Drone(droneName, initialPos);
        activeDrones.add(newDrone);
        recordVisitedPosition(droneName, initialPos);

        return newDrone;
    }

    private synchronized void recordVisitedPosition(String droneName, Position position) {
        if (!visitedPositions.contains(position)) {
            visitedPositions.add(position);
        }
        // TODO Is this the right place?
        navigationPlanes.get(position.getPosY()).get(position.getPosX()).setOccupied(true);
        mapService.setColor(position.getPosX(), position.getPosY(), (activeDrones.indexOf(findDroneByName(droneName)) + 3));
    }

    private synchronized boolean positionOutOfBounds(int x, int y) {
        return false;
    }

    @Override
    public void run() {
//        System.out.println("Map decay running");
        synchronized (this) {
            for (Drone drone : activeDrones) {
                List<Position> dronePath = drone.getCurrentPath();

                if (dronePath != null && !dronePath.isEmpty()) {
                    if (navigationPlanes.get(dronePath.getFirst().getPosY()).get(dronePath.getFirst().getPosX()).isOccupied()) {
                        System.out.println("Drone " + drone.getName() + " waiting at position: ("
                                + drone.getCurrentPosition().getPosX() + ", " + drone.getCurrentPosition().getPosY() + ")");
                        continue;
                    }
                    // Move along the path
                    Position nextPosition = dronePath.removeFirst();
                    setPosition(drone.getName(), nextPosition);
                } else {
                    System.out.println("1");
                    if (visitedPositions.size() < navigationPlanes.size() * navigationPlanes.getFirst().size()) {
                        List<Position> exclusionList = new ArrayList<>(visitedPositions);
                        for (String targetName : crtTargets.keySet()) {
                            exclusionList.add(crtTargets.get(targetName));
                        }
                        // If no destination or destination reached, find a new destination
                        if (drone.getTargetPosition() == null || drone.getTargetPosition().equals(drone.getCurrentPosition())) {
                            Position nextTarget = PathCalculator.getClosestTarget(drone.getCurrentPosition(), navigationPlanes, exclusionList);
                            drone.setTargetPosition(nextTarget);
                            if (nextTarget != null) {
                                crtTargets.put(drone.getName(), nextTarget);
                            }
                        }
                        // Check if the distance is too big maybe here?
                        List<Position> calculatedPath = PathCalculator.calculatePath(drone.getCurrentPosition(), drone.getTargetPosition(), navigationPlanes);
                        if (!calculatedPath.isEmpty()) {
                            drone.setTargetPath(calculatedPath);
                        } else {
                            System.out.println("No new target for drone " + drone.getName() + ". Stopped at position: ("
                                            + drone.getCurrentPosition().getPosX() + ", " + drone.getCurrentPosition().getPosY() + ")");
                            drone.setTargetPosition(null);
                            crtTargets.remove(drone.getName());
                            // Free position for make sures other drones are able to fly
                            navigationPlanes.get(drone.getCurrentPosition().getPosY()).get(drone.getCurrentPosition().getPosX()).setOccupied(false);
                        }
//                        System.out.println("Crt targets size: " + crtTargets.size());
                    } else {
                        System.out.println("Target reached; start over");
                        visitedPositions.clear();
                        mapService.initColorMatrix(navigationPlanes.getFirst().size(), navigationPlanes.size());
                    }
                }
            }

            // TODO Add map decay here
        }
    }
}
