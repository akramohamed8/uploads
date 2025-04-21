package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class Ace extends Standard {

    public Ace(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 1, suit, boardManager, gameManager);
    }
    protected int getExpectedMarbleCount() {
        return 1;
    }
    
    public void act(ArrayList<Marble> marbles) throws ActionException {
        Marble m = marbles.get(0);
        if (m.isAtHome()) {
            try {
                game.fieldMarble(m);
            } catch (Exception e) {
                throw new ActionException(e.getMessage());
            }
        } else {
            try {
                board.moveBy(m, 1, false); // acts as standard move
            } catch (Exception e) {
                throw new ActionException(e.getMessage());
            }
        }
    }

}
