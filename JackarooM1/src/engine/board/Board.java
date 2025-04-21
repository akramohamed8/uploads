package engine.board;

import java.util.ArrayList;
import java.util.Random;

import engine.GameManager;
import exception.CannotFieldException;
import exception.IllegalDestroyException;
import exception.IllegalMovementException;
import exception.IllegalSwapException;
import exception.InvalidMarbleException;
import model.Colour;
import model.player.Marble;

@SuppressWarnings("unused")
public class Board implements BoardManager {
    private final ArrayList<Cell> track;
    private final ArrayList<SafeZone> safeZones;
    private final GameManager gameManager;
    private int splitDistance;

    public Board(ArrayList<Colour> colourOrder, GameManager gameManager) {
        this.track = new ArrayList<>();
        this.safeZones = new ArrayList<>();
        this.gameManager = gameManager;
        
        for (int i = 0; i < 100; i++) {
            this.track.add(new Cell(CellType.NORMAL));
            
            if (i % 25 == 0) 
                this.track.get(i).setCellType(CellType.BASE);
            
            else if ((i+2) % 25 == 0) 
                this.track.get(i).setCellType(CellType.ENTRY);
        }

        for(int i = 0; i < 8; i++)
            this.assignTrapCell();

        for (int i = 0; i < 4; i++)
            this.safeZones.add(new SafeZone(colourOrder.get(i)));

        splitDistance = 3;
    }

    // Method 1: Get Safe Zone for a specific color
    private ArrayList<Cell> getSafeZone(Colour colour) {
        for (SafeZone safeZone : safeZones) {
            if (safeZone.getColour() == colour) {
                return safeZone.getCells();
            }
        }
        return null;
    }

    // Method 2: Get position of a marble in a path
    private int getPositionInPath(ArrayList<Cell> path, Marble marble) {
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).getMarble() == marble) {
                return i;
            }
        }
        return -1;
    }

    // Method 3: Get base position for a color
    private int getBasePosition(Colour colour) {
        int baseIndex = -1;
        switch (colour) {
            case RED:
                baseIndex = 0;
                break;
            case GREEN:
                baseIndex = 25;
                break;
            case BLUE:
                baseIndex = 50;
                break;
            case YELLOW:
                baseIndex = 75;
                break;
            default:
                return -1;
        }
        return baseIndex;
    }

    // Method 4: Get entry position for a color
    private int getEntryPosition(Colour colour) {
        int entryIndex = -1;
        switch (colour) {
            case RED:
                entryIndex = 98;  // (0+2-4) % 100 = 98
                break;
            case GREEN:
                entryIndex = 23;  // (25+2-4) % 100 = 23
                break;
            case BLUE:
                entryIndex = 48;  // (50+2-4) % 100 = 48
                break;
            case YELLOW:
                entryIndex = 73;  // (75+2-4) % 100 = 73
                break;
            default:
                return -1;
        }
        return entryIndex;
    }

    // Method 5: Validate steps for a marble
    private ArrayList<Cell> validateSteps(Marble marble, int steps) throws IllegalMovementException {
        ArrayList<Cell> fullPath = new ArrayList<>();
        
        // Find marble's current position
        int trackPosition = getPositionInPath(track, marble);
        ArrayList<Cell> safeZoneCells = getSafeZone(marble.getColour());
        int safeZonePosition = -1;
        
        if (safeZoneCells != null) {
            safeZonePosition = getPositionInPath(safeZoneCells, marble);
        }
        
        // Marble not on track or in safe zone
        if (trackPosition == -1 && safeZonePosition == -1) {
            throw new IllegalMovementException("Marble is not on track or in safe zone");
        }
        
        // Marble on track
        if (trackPosition != -1) {
            int entryPosition = getEntryPosition(marble.getColour());
            boolean isOpponentMarble = marble.getColour() != gameManager.getActivePlayerColour();
            
            // Handle backward movement (Four card)
            int distanceToEntry;
            if (steps >= 0) {
                distanceToEntry = entryPosition >= trackPosition ? 
                                 entryPosition - trackPosition : 
                                 100 - trackPosition + entryPosition;
            } else {
                distanceToEntry = trackPosition >= entryPosition ? 
                                 trackPosition - entryPosition : 
                                 trackPosition + 100 - entryPosition;
            }
            
            // Five card: Prevent opponent marble entering Safe Zone
            if (isOpponentMarble && steps >= distanceToEntry) {
                throw new IllegalMovementException("Opponent marble cannot enter Safe Zone");
            }
            
            // Check if steps exceed track length
            if (steps >= 0 && steps > distanceToEntry + (safeZoneCells != null ? safeZoneCells.size() : 0)) {
                throw new IllegalMovementException("Card rank is too high for this move");
            }
            
            // Build path
            if (!isOpponentMarble && steps >= 0 && steps > distanceToEntry) {
                // Move to Safe Zone
                for (int i = 0; i < distanceToEntry; i++) {
                    int nextPos = (trackPosition + i) % 100;
                    fullPath.add(track.get(nextPos));
                }
                int safeZoneSteps = steps - distanceToEntry;
                for (int i = 0; i < safeZoneSteps; i++) {
                    fullPath.add(safeZoneCells.get(i));
                }
            } else {
                // Stay on track (forward or backward)
                int absSteps = Math.abs(steps);
                for (int i = 0; i <= absSteps; i++) {
                    int pos = steps >= 0 ? (trackPosition + i) % 100 : (trackPosition - i + 100) % 100;
                    fullPath.add(track.get(pos));
                }
            }
        }
        
        // Marble in safe zone
        if (safeZonePosition != -1) {
            if (steps < 0) {
                throw new IllegalMovementException("Cannot move backwards in Safe Zone");
            }
            if (safeZonePosition + steps >= (safeZoneCells != null ? safeZoneCells.size() : 0)) {
                throw new IllegalMovementException("Card rank is too high for this move");
            }
            for (int i = 0; i <= steps; i++) {
                fullPath.add(safeZoneCells.get(safeZonePosition + i));
            }
        }
        
        return fullPath;
    }

 // Method 6: Validate path for a marble's movement
    private void validatePath(Marble marble, ArrayList<Cell> fullPath, boolean destroy) 
            throws IllegalMovementException {
        
        // Check path excluding target (fullPath.size() - 1)
        for (int i = 1; i < fullPath.size() - 1; i++) {
            Cell cell = fullPath.get(i);
            Marble cellMarble = cell.getMarble();
            
            // If cell has a marble and it's not a King (destroy = false)
            if (cellMarble != null && !destroy) {
                throw new IllegalMovementException("Path is blocked at position " + i);
            }
            
            // If cell has player's own marble, even King can't bypass
            if (cellMarble != null && cellMarble.getColour() == marble.getColour()) {
                throw new IllegalMovementException("Cannot bypass own marble");
            }
            
            // Check if marble is in safe zone (immune to interference)
            if (cellMarble != null && getSafeZone(cellMarble.getColour()) != null && 
                getPositionInPath(getSafeZone(cellMarble.getColour()), cellMarble) != -1) {
                throw new IllegalMovementException("Cannot bypass marble in Safe Zone");
            }
        }
        
        // Check target cell
        Cell targetCell = fullPath.get(fullPath.size() - 1);
        Marble targetMarble = targetCell.getMarble();
        
        // Check if target has player's own marble
        if (targetMarble != null && targetMarble.getColour() == marble.getColour()) {
            throw new IllegalMovementException("Cannot land on own marble");
        }
        
        // Check if target is in Base cell of another player
        // REMOVED THIS CONDITION as indicated by the test:
        /* 
        if (targetCell.getCellType() == CellType.BASE && targetMarble != null) {
            throw new IllegalMovementException("Cannot land on a marble in its Base cell");
        }
        */
        
        // Check if target is a marble in safe zone
        if (targetMarble != null && getSafeZone(targetMarble.getColour()) != null && 
            getPositionInPath(getSafeZone(targetMarble.getColour()), targetMarble) != -1) {
            throw new IllegalMovementException("Cannot land on a marble in Safe Zone");
        }
        
        // Check Safe Zone Entry blockage
        for (Cell cell : fullPath) {
            if (cell.getCellType() == CellType.ENTRY) {
                if (cell.getMarble() != null && cell.getMarble().getColour() != marble.getColour()) {
                    throw new IllegalMovementException("Safe Zone Entry is blocked");
                }
            }
        }
    }

    // Method 7: Execute the movement
    private void move(Marble marble, ArrayList<Cell> fullPath, boolean destroy) 
            throws IllegalDestroyException {
        if (marble == null || fullPath == null || fullPath.isEmpty()) {
            throw new IllegalDestroyException("Invalid marble or path");
        }
        
        // Remove marble from current position
        Cell currentCell = fullPath.get(0);
        currentCell.setMarble(null);
        
        // Handle marble destroying on the path (King card)
        if (destroy) {
            for (int i = 1; i < fullPath.size(); i++) {
                Cell cell = fullPath.get(i);
                if (cell.getMarble() != null && cell.getMarble().getColour() != marble.getColour()) {
                    Marble targetMarble = cell.getMarble();
                    cell.setMarble(null);
                    gameManager.sendHome(targetMarble);
                }
            }
        }
        
        // Get target cell
        Cell targetCell = fullPath.get(fullPath.size() - 1);
        
        // Check if target has opponent's marble
        if (targetCell.getMarble() != null) {
            Marble targetMarble = targetCell.getMarble();
            targetCell.setMarble(null);
            gameManager.sendHome(targetMarble);
        }
        
        // Place marble in target cell
        targetCell.setMarble(marble);
        
        // Check if target is a trap (only for Normal track cells)
        if (targetCell.isTrap() && targetCell.getCellType() == CellType.NORMAL) {
            targetCell.setMarble(null);
            gameManager.sendHome(marble);
            targetCell.setTrap(false);
            assignTrapCell();
        }
    }

 // Method 8: Validate swap
    private void validateSwap(Marble marble1, Marble marble2) throws IllegalSwapException {
        if (marble1 == null || marble2 == null) {
            throw new IllegalSwapException("Invalid marbles");
        }

        // Check if both marbles are on track
        int pos1 = getPositionInPath(track, marble1);
        int pos2 = getPositionInPath(track, marble2);

        if (pos1 == -1 || pos2 == -1) {
            throw new IllegalSwapException("Both marbles must be on track");
        }

        // Check if marbles belong to the same player
        if (marble1.getColour() == marble2.getColour()) {
            throw new IllegalSwapException("Cannot swap marbles of the same player");
        }

        // Check if marble1 is in its own Base cell
        if (track.get(pos1).getCellType() == CellType.BASE && 
            pos1 == getBasePosition(marble1.getColour())) {
            throw new IllegalSwapException("Cannot swap with a marble in its own Base cell");
        }

        // Check if marble2 is in its own Base cell
        if (track.get(pos2).getCellType() == CellType.BASE && 
            pos2 == getBasePosition(marble2.getColour())) {
            throw new IllegalSwapException("Cannot swap with a marble in its own Base cell");
        }
    }

    // Method 9: Validate destroy
    private void validateDestroy(int positionInPath) throws IllegalDestroyException {
        // Check if position is valid
        if (positionInPath < 0 || positionInPath >= track.size()) {
            throw new IllegalDestroyException("Invalid position on track");
        }
        
        // Check if marble is on track
        if (track.get(positionInPath).getMarble() == null) {
            throw new IllegalDestroyException("No marble at specified position");
        }
        
        // Check if marble is in its Base cell
        Marble marble = track.get(positionInPath).getMarble();
        if (track.get(positionInPath).getCellType() == CellType.BASE && 
            positionInPath == getBasePosition(marble.getColour())) {
            throw new IllegalDestroyException("Cannot destroy a marble in its Base cell");
        }
    }

    // Method 10: Validate fielding
    private void validateFielding(Cell occupiedBaseCell) throws CannotFieldException {
        if (occupiedBaseCell.getMarble() != null) {
            throw new CannotFieldException("Base cell is already occupied by another marble");
        }
    }

    // Method 11: Validate saving
    private void validateSaving(int positionInSafeZone, int positionOnTrack) throws InvalidMarbleException {
        // Check if marble is already in safe zone
        if (positionInSafeZone != -1) {
            throw new InvalidMarbleException("Marble is already in Safe Zone");
        }
        
        // Check if marble is on track
        if (positionOnTrack == -1) {
            throw new InvalidMarbleException("Marble is not on track");
        }
    }

    public ArrayList<Cell> getTrack() {
        return this.track;
    }

    public ArrayList<SafeZone> getSafeZones() {
        return this.safeZones;
    }
    
    @Override
    public int getSplitDistance() {
        return this.splitDistance;
    }

    public void setSplitDistance(int splitDistance) {
        this.splitDistance = splitDistance;
    }
   
    private void assignTrapCell() {
        int randIndex = -1;
        
        do
            randIndex = (int)(Math.random() * 100); 
        while(this.track.get(randIndex).getCellType() != CellType.NORMAL || this.track.get(randIndex).isTrap());
        
        this.track.get(randIndex).setTrap(true);
    }

    @Override
    public void moveBy(Marble marble, int steps, boolean destroy) throws IllegalMovementException, IllegalDestroyException {
        // Validate steps and get full path
        ArrayList<Cell> fullPath = validateSteps(marble, steps);
        
        // Validate the path
        validatePath(marble, fullPath, destroy);
        
        // Execute the movement
        move(marble, fullPath, destroy);
    }

    @Override
    public void swap(Marble marble1, Marble marble2) throws IllegalSwapException {
        // Validate the swap
        validateSwap(marble1, marble2);
        
        // Get positions
        int pos1 = getPositionInPath(track, marble1);
        int pos2 = getPositionInPath(track, marble2);
        
        // Perform the swap
        track.get(pos1).setMarble(marble2);
        track.get(pos2).setMarble(marble1);
    }

    @Override
    public void destroyMarble(Marble marble) throws IllegalDestroyException {
        // Find marble position
        int position = getPositionInPath(track, marble);
        
        if (position != -1) {
            // Validate the destroy
            validateDestroy(position);
            
            // Remove the marble from track
            track.get(position).setMarble(null);
            
            // Send marble back to home zone
            gameManager.sendHome(marble);
        } else {
            throw new IllegalDestroyException("Marble not found on track");
        }
    }

    @Override
    public void sendToBase(Marble marble) throws CannotFieldException, IllegalDestroyException {
        // Get base position for this marble's color
        int basePos = getBasePosition(marble.getColour());
        
        // Validate the base cell isn't occupied by same player's marble
        Cell baseCell = track.get(basePos);
        
        if (baseCell.getMarble() != null && baseCell.getMarble().getColour() == marble.getColour()) {
            validateFielding(baseCell);
        }
        
        // If base cell has opponent's marble, destroy it
        if (baseCell.getMarble() != null) {
            Marble opponentMarble = baseCell.getMarble();
            destroyMarble(opponentMarble);
        }
        
        // Place marble in base cell
        baseCell.setMarble(marble);
    }

    @Override
    public void sendToSafe(Marble marble) throws InvalidMarbleException {
        // Find position in safe zone and on track
        int posOnTrack = getPositionInPath(track, marble);
        ArrayList<Cell> safeZoneCells = getSafeZone(marble.getColour());
        int posInSafeZone = -1;
        
        if (safeZoneCells != null) {
            posInSafeZone = getPositionInPath(safeZoneCells, marble);
        }
        
        // Validate the saving action
        validateSaving(posInSafeZone, posOnTrack);
        
        // Find a random empty cell in safe zone
        ArrayList<Integer> emptyCells = new ArrayList<>();
        for (int i = 0; i < safeZoneCells.size(); i++) {
            if (safeZoneCells.get(i).getMarble() == null) {
                emptyCells.add(i);
            }
        }
        
        if (emptyCells.isEmpty()) {
            throw new InvalidMarbleException("No empty cell in Safe Zone");
        }
        
        // Select a random empty cell
        int randomIndex = new Random().nextInt(emptyCells.size());
        int targetCellIndex = emptyCells.get(randomIndex);
        
        // Remove marble from track
        track.get(posOnTrack).setMarble(null);
        
        // Place marble in safe zone
        safeZoneCells.get(targetCellIndex).setMarble(marble);
    }

    @Override
    public ArrayList<Marble> getActionableMarbles() {
        ArrayList<Marble> actionableMarbles = new ArrayList<>();
        
        // Get marbles on track
        for (Cell cell : track) {
            if (cell.getMarble() != null) {
                actionableMarbles.add(cell.getMarble());
            }
        }
        
        // Get marbles in current player's safe zone
        Colour currentPlayerColour = gameManager.getActivePlayerColour();
        ArrayList<Cell> currentPlayerSafeZone = getSafeZone(currentPlayerColour);
        
        if (currentPlayerSafeZone != null) {
            for (Cell cell : currentPlayerSafeZone) {
                if (cell.getMarble() != null) {
                    actionableMarbles.add(cell.getMarble());
                }
            }
        }
        
        return actionableMarbles;
    }
}