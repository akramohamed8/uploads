package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class Four  extends Standard {

    public Four(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 4, suit, boardManager, gameManager);
    }
    protected int getExpectedMarbleCount() {
        return 1;
    }

    
    public void act(ArrayList<Marble> marbles) throws ActionException {
        try {
            board.moveBy(marbles.get(0), -4, false); // Negative for backward
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }

}
