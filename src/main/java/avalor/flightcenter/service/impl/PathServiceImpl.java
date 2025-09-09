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
    private final List<List<Position>> navigationPlanes = Collections.synchronizedList(new ArrayList<>());
    private final List<Position> visitedPositions = Collections.synchronizedList(new ArrayList<>());
    private final List<Drone> activeDrones = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentMap<String, Position> crtTargets = new ConcurrentHashMap<>();
    private MapService mapService = null;

    @Override
    public void setMapService(MapService service) {
        mapService = service;
    }

    @Override
    public synchronized void reset() {
        activeDrones.clear();
        navigationPlanes.clear();
        restartNavigation();
    }

    @Override
    public void init(List<List<Integer>> navigationMatrix) {
        for (int i = 0; i < navigationMatrix.size(); i++) {
            List<Integer> row = navigationMatrix.get(i);
            List<Position> positionRow = new ArrayList<>();
            for (int j = 0; j < row.size(); j++) {
                Integer value = row.get(j);
                positionRow.add(new Position(j, i, value));
            }
            navigationPlanes.add(positionRow);
        }
        // TODO This is just for testing purposes, remove it when not needed
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
         if (initialPos == null || positionOutOfBounds(initialPos.getPosX(), initialPos.getPosY())) {
             return null;
        }

        Drone newDrone = new Drone(droneName, initialPos);
        activeDrones.add(newDrone);
        recordVisitedPosition(droneName, initialPos);

        return newDrone;
    }

    @Override
    public List<Position> getPathForDrone(String droneName) {
        // TODO Fix this or remove the functionality
        List<Position> positions = new ArrayList<>();

//        positions = PathCalculator.calculatePath(dronePositions.get(droneName), navigationPlanes, visitedPositions);
        return positions;
    }

    @Override
    public synchronized void recordDronePosition(String droneName, Position newPosition) {
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
                setOccupied(crtDrone.getName(), crtDrone.getCurrentPosition(), false);
            }
            // Set the new position
            crtDrone.setCurrentPosition(Position.builder(newPosition).build());
            recordVisitedPosition(droneName, newPosition);
        }
    }

    private synchronized void recordVisitedPosition(String droneName, Position position) {
        if (!visitedPositions.contains(position)) {
            visitedPositions.add(position);
        }
        // TODO Is this the right place?
        setOccupied(droneName, position, true);
    }

    private synchronized boolean positionOutOfBounds(int x, int y) {
        return navigationPlanes.isEmpty() || x < 0 || x >= navigationPlanes.getFirst().size() || y < 0 || y >= navigationPlanes.size();
    }

    @Override
    public void run() {
//        System.out.println("Map decay running");
        synchronized (this) {
            for (Drone drone : activeDrones) {
                if (visitedPositions.size() < navigationPlanes.size() * navigationPlanes.getFirst().size()) {
                    List<Position> exclusionList = new ArrayList<>(visitedPositions);
                    for (String targetName : crtTargets.keySet()) {
                        exclusionList.add(crtTargets.get(targetName));
                    }
                    // If no target is set, or the target us reached of the target has already been visited, find a new destination
                    if (drone.getTargetPosition() == null || drone.isTargetReached() /*|| isVisited(drone.getTargetPosition())*/) {
                        Position nextTarget = PathCalculator.getClosestTarget(drone.getCurrentPosition(), navigationPlanes, exclusionList);
                        drone.setTargetPosition(nextTarget);
                        drone.setTargetPath(null);
                        if (nextTarget != null) {
//                            System.out.println("Next target " + drone.getName() + "  at position: ("
//                                    + drone.getTargetPosition().getPosX() + ", " + drone.getTargetPosition().getPosY() + "). "
//                                    + " Starting from position: (" + drone.getCurrentPosition().getPosX() + ", " + drone.getCurrentPosition().getPosY() + "). ");
                            crtTargets.put(drone.getName(), nextTarget);
                        } else {
                            setOccupied(drone.getName(), drone.getCurrentPosition(), false);
                        }
                    }
                    if (drone.getTargetPosition() != null) {
                        List<Position> dronePath = drone.getTargetPath();
                        if (dronePath != null && !dronePath.isEmpty()) {
                            if (isOccupied(dronePath.getFirst())) {
                                System.out.println("Drone " + drone.getName() + " waiting at position: ("
                                        + drone.getCurrentPosition().getPosX() + ", " + drone.getCurrentPosition().getPosY() + "). " +
                                        "Occupied position: (" + dronePath.getFirst().getPosX() + ", " + dronePath.getFirst().getPosY() + ")");
                                continue;
                            }
                            // Move along the path
                            Position nextPosition = dronePath.removeFirst();
                            recordDronePosition(drone.getName(), nextPosition);
                        } else {
                            // Check if the distance is too big maybe here?
                            List<Position> calculatedPath = PathCalculator.calculatePath(drone.getCurrentPosition(), drone.getTargetPosition(), navigationPlanes);
                            if (!calculatedPath.isEmpty()) {
                                drone.setTargetPath(calculatedPath);
                            } else {
//                                System.out.println("No new target for drone " + drone.getName() + ". Stopped at position: ("
//                                        + drone.getCurrentPosition().getPosX() + ", " + drone.getCurrentPosition().getPosY() + ")");
                                drone.setTargetPosition(null);
                                drone.setTargetPath(null);
                                crtTargets.remove(drone.getName());
                                // Free position for make sures other drones are able to fly
                                setOccupied(drone.getName(), drone.getCurrentPosition(), false);
                            }
                        }
                    }
                } else {
                    System.out.println("Target reached; start over");
                    restartNavigation();
                }
            }

            // Update decay across the whole navigation plane
            // Increase decay by 1 for non-occupied positions; clear decay for occupied ones
            decay(1, false);
        }
    }

    private synchronized void printOccupiedNodes() {
        System.out.println("Occupied positions");
        for (List<Position> row : navigationPlanes) {
            for (Position position : row) {
                if (position.isOccupied()) {
                    System.out.println(position.getPosX() + "," + position.getPosY());
                }
            }
        }
    }

    private synchronized boolean isOccupied(Position position) {
        if (position == null) {
            return false;
        }
        return navigationPlanes.get(position.getPosY()).get(position.getPosX()).isOccupied();
    }

    private synchronized void setOccupied(String droneName, Position position, boolean occupied) {
        if (position == null) {
            return;
        }
        navigationPlanes.get(position.getPosY()).get(position.getPosX()).setOccupied(occupied);
        if (occupied) {
            mapService.setColor(position.getPosX(), position.getPosY(), (activeDrones.indexOf(findDroneByName(droneName)) + 3));
        } else {
            mapService.setColor(position.getPosX(), position.getPosY(), (activeDrones.indexOf(findDroneByName(droneName)) + 4));
        }
    }

    private synchronized boolean isVisited(Position position) {
        if (position == null) {
            return false;
        }
        return visitedPositions.contains(position);
    }

    private synchronized void restartNavigation() {
        crtTargets.clear();
        visitedPositions.clear();
        mapService.clear();
        for (Drone drone : activeDrones) {
            drone.setTargetPosition(null);
            drone.setTargetPath(null);
            drone.clearHistory();
            recordVisitedPosition(drone.getName(), drone.getCurrentPosition());
        }
        // Clear decay as well
        decay(0, true);
    }
    
    private synchronized void decay(int decayVal, boolean clearAll) {
        for (List<Position> row : navigationPlanes) {
            for (Position p : row) {
                if (clearAll) {
                    p.setDecay(decayVal);
                } else {
                    p.setDecay(p.isOccupied() ? 0 : p.getDecay() + decayVal);
                }
            }
        }
    }
}
