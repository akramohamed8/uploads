// In model/card/standard/Jack.java

package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class Jack extends Standard {

    public Jack(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 11, suit, boardManager, gameManager);
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return 2;
    }

    @Override
    public boolean validateMarbleColours(ArrayList<Marble> marbles) {
        if (marbles.size() != 2) return false;
        return marbles.get(0).getColour() != marbles.get(1).getColour();
    }

    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException {
        if (marbles.size() != 2) {
            throw new ActionException("Jack card requires exactly two marbles");
        }
        if (!validateMarbleColours(marbles)) {
            throw new ActionException("Marbles must belong to different players");
        }
        try {
            boardManager.swap(marbles.get(0), marbles.get(1));
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }
}