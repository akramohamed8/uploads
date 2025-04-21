package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import model.player.Marble;

public class Seven extends Standard {
    private int splitDistance;

    public Seven(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 7, suit, boardManager, gameManager);
        this.splitDistance = 0; // Default value
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return splitDistance > 0 ? 2 : 1;
    }
    
    public void setSplitDistance(int distance) throws SplitOutOfRangeException {
        if (distance < 1 || distance > 6) {
            throw new SplitOutOfRangeException("Split distance must be between 1 and 6.");
        }
        this.splitDistance = distance;
    }

    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException, InvalidMarbleException {
        try {
            if (splitDistance > 0) {
                if (marbles.size() != 2) {
                    throw new InvalidMarbleException("Split movement requires two marbles.");
                }
                
                boardManager.moveBy(marbles.get(0), splitDistance, false);
                
                boardManager.moveBy(marbles.get(1), 7 - splitDistance, false);
            } else {
                boardManager.sendToSafe(marbles.get(0));
            }
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }
}