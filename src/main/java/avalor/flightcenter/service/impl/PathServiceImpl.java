package avalor.flightcenter.service.impl;

import avalor.flightcenter.calculator.PathCalculator;
import avalor.flightcenter.domain.Drone;
import avalor.flightcenter.domain.Position;
import avalor.flightcenter.service.MapService;
import avalor.flightcenter.service.PathService;
import avalor.flightcenter.utils.DecaySimulator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        setDronePositionInPlane(droneName, initialPos);

        return newDrone;
    }

    @Override
    public List<Position> getPathForDrone(String droneName) {
        List<Position> droneFlightPath = new ArrayList<>();
        if (droneName == null) {
            return droneFlightPath;
        }
        Drone crtDrone = findDroneByName(droneName);
        if (crtDrone != null) {
            droneFlightPath = crtDrone.getTargetPath();
        }
        return droneFlightPath;
    }

    @Override
    public synchronized void recordDronePosition(String droneName, Position newPosition) {
        if (droneName == null || newPosition == null) {
            return;
        }

        if (positionOutOfBounds(newPosition.getPosX(), newPosition.getPosY())) {
            return; // Out-of-bounds positions are discarded
        }
        Drone crtDrone = findDroneByName(droneName);
        // If the drone does not exist, try to add it
        if (crtDrone != null) {
            // Free up the old space in the navigation plane
            Position oldPosition = crtDrone.getCurrentPosition();
            if (oldPosition != null) {
                setNavigationPlaneOccupied(crtDrone.getName(), crtDrone.getCurrentPosition(), false);
            }
            // Set the new position
            crtDrone.setCurrentPosition(Position.builder(newPosition).build());
            setDronePositionInPlane(droneName, newPosition);
        }
    }

    private synchronized void setDronePositionInPlane(String droneName, Position position) {
        if (!visitedPositions.contains(position)) {
            visitedPositions.add(position);
        }
        // TODO Is this the right place?
        setNavigationPlaneOccupied(droneName, position, true);
    }

    private synchronized boolean positionOutOfBounds(int x, int y) {
        return navigationPlanes.isEmpty() || x < 0 || x >= navigationPlanes.getFirst().size() || y < 0 || y >= navigationPlanes.size();
    }

    @Override
    public void run() {
        synchronized (this) {
            for (Drone drone : activeDrones) {
                if (visitedPositions.size() < navigationPlanes.size() * navigationPlanes.getFirst().size()) {
                    List<Position> exclusionList = new ArrayList<>(visitedPositions);
                    for (String targetName : crtTargets.keySet()) {
                        exclusionList.add(crtTargets.get(targetName));
                    }
                    // If no target is set, or the target us reached of the target has already been visited, find a new destination
                    if (drone.isTargetReached() || isVisited(drone.getTargetPosition())) {
                        // Find a new target for this drone
                        drone.setTargetPosition(PathCalculator.getClosestTarget(drone.getCurrentPosition(), navigationPlanes, exclusionList));
                        // The path to the target is yet to be determined
                        drone.setTargetPath(null);
                        // Free position for make sure other drones are able to fly
                        setNavigationPlaneOccupied(drone.getName(), drone.getCurrentPosition(), false);
                    }
                    // If the drone has a locked target, either continue on the path or get the next path part (long distances)
                    if (drone.getTargetPosition() != null) {
                        // The current target is marked as locked
                        crtTargets.put(drone.getName(), drone.getTargetPosition());
                        if (drone.getNextPossibleMove() != null) {
                            if (!droneMovedIntoFreeSpace(drone)) {
                                // Just wait, but maybe a re-route is needed
                            }
                        } else {
                            // If there is no path set, retrieve a new path towards the target
                            List<Position> calculatedPath = PathCalculator.calculatePath(drone.getCurrentPosition(), drone.getTargetPosition(), navigationPlanes);
                            if (!calculatedPath.isEmpty()) {
                                drone.setTargetPath(calculatedPath);
                            } else {
                                // There is no possible path, target is abandoned
                                drone.setTargetPosition(null);
                                drone.setTargetPath(null);
                                crtTargets.remove(drone.getName());
                            }
                        }
                    }
                } else {
                    printTravelledPathValues();
                    System.out.println("Target reached; start over");
                    restartNavigation();
                    break;
                }
            }
            // Update decay across the whole navigation plane
            // Increase decay by 1 for non-occupied positions; clear decay for occupied ones
            applyDecayToNavigationPlanes(1, false);
        }
    }

    private synchronized void printTravelledPathValues() {
        for (Drone drone : activeDrones) {
            System.out.println("Travelled path info for drone " + drone.getName() + " Number of steps: " + drone.getHistoryPath().size()
                    + ". Path value: " + drone.getTravelledPathValue());
            System.out.println("Current position: " + drone.getCurrentPosition() + ", Target position: " + drone.getTargetPosition()
                    + ", Target assigned: " + (drone.getTargetPosition() != null));
        }
    }

    private synchronized boolean droneMovedIntoFreeSpace(Drone drone) {
        Position nextPossiblePosition = drone.getNextPossibleMove();
        boolean droneMoved = false;
        if (nextPossiblePosition != null) {
            if (isNavigationPlaneFree(nextPossiblePosition)) {
                // Only if the drone can move into the space
                recordDronePosition(drone.getName(), nextPossiblePosition);
                drone.moveToNextPosition();
                droneMoved = true;
                //Otherwise wait for the plane to be vacated
            } else {
                // Find another free space around the current position
                System.out.println("Next location is blocked; finding free space for drone " + drone.getName());
                nextPossiblePosition = findFreeSpace(drone.getCurrentPosition(), drone.getTargetPosition());
                if (nextPossiblePosition != null) {
                    drone.getTargetPath().addFirst(drone.getCurrentPosition());
                    drone.getTargetPath().addFirst(nextPossiblePosition);
                    recordDronePosition(drone.getName(), nextPossiblePosition);
                    drone.moveToNextPosition();
                    // Force path recalculation
                    drone.setTargetPath(null);
                    droneMoved = true;
                }
            }
        }
        return droneMoved;
    }

    private synchronized Position findFreeSpace(Position crtPosition, Position targetPosition) {
        if (crtPosition == null) {
            return null;
        }

        List<Position> availablePositions = getNeighbours(crtPosition);
        return availablePositions.stream().filter(this::isNavigationPlaneFree).findFirst().orElse(null);
    }

    public synchronized List<Position> getNeighbours(Position crtPosition) {
        List<Position> neighbours = new ArrayList<>();
        if (navigationPlanes.isEmpty() || crtPosition == null) {
            return neighbours;
        }

        if (positionOutOfBounds(crtPosition.getPosX(), crtPosition.getPosY())) {
            return neighbours;
        }

        int height = navigationPlanes.size();
        int width = navigationPlanes.getFirst().size();

        if (crtPosition.getPosY() + 1 < height) {
            neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() + 1).get(crtPosition.getPosX())).build());
        }
        if (crtPosition.getPosY() - 1 >= 0) {
            neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() - 1).get(crtPosition.getPosX())).build());
        }
        if (crtPosition.getPosX() + 1 < width) {
            neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY()).get(crtPosition.getPosX() + 1)).build());
            if (crtPosition.getPosY() + 1 < height)
                neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() + 1).get(crtPosition.getPosX() + 1)).build());
            if (crtPosition.getPosY() - 1 >= 0)
                neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() - 1).get(crtPosition.getPosX() + 1)).build());
        }
        if (crtPosition.getPosX() - 1 >= 0) {
            neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY()).get(crtPosition.getPosX() - 1)).build());
            if (crtPosition.getPosY() + 1 < height)
                neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() + 1).get(crtPosition.getPosX() - 1)).build());
            if (crtPosition.getPosY() - 1 >= 0)
                neighbours.add(Position.builder(navigationPlanes.get(crtPosition.getPosY() - 1).get(crtPosition.getPosX() - 1)).build());
        }
        return neighbours;
    }

    private synchronized boolean isNavigationPlaneFree(Position position) {
        if (position == null) {
            return true;
        }
        return !navigationPlanes.get(position.getPosY()).get(position.getPosX()).isOccupied();
    }

    private synchronized boolean isVisited(Position position) {
        if (position == null) {
            return false;
        }
        return visitedPositions.contains(position);
    }

    private synchronized void setNavigationPlaneOccupied(String droneName, Position position, boolean occupied) {
        if (position == null) {
            return;
        }
        navigationPlanes.get(position.getPosY()).get(position.getPosX()).setOccupied(occupied);
        // Mark the change in the map service as well
        if (occupied) {
            mapService.setColor(position.getPosX(), position.getPosY(), (activeDrones.indexOf(findDroneByName(droneName)) + 4));
        } else {
            mapService.setColor(position.getPosX(), position.getPosY(), 3);
        }
    }

    private synchronized void applyDecayToNavigationPlanes(int decayVal, boolean clearAll) {
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

    private synchronized void restartNavigation() {
        crtTargets.clear();
        visitedPositions.clear();
        mapService.clear();
        for (Drone drone : activeDrones) {
            drone.setTargetPosition(null);
            drone.setTargetPath(null);
            drone.clearHistoryPath();
            setDronePositionInPlane(drone.getName(), drone.getCurrentPosition());
        }
        // Clear decay as well
        applyDecayToNavigationPlanes(0, true);
    }
}
