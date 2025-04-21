package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class Five extends Standard {

    public Five(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 5, suit, boardManager, gameManager);
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return 1;
    }

    @Override
    public boolean validateMarbleColours(ArrayList<Marble> marbles) {
        return true; 
    }

    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException {
        try {
            boardManager.moveBy(marbles.get(0), 5, false);
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }
}