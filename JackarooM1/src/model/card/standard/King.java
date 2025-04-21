package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class King extends Standard {

    public King(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 13, suit, boardManager, gameManager);
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return 1;
    }

    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException {
        if (marbles.isEmpty()) {
            throw new ActionException("King card requires at least one marble");
        }
        Marble m = marbles.get(0);
        if (m.isAtHome()) {
            try {
                gameManager.fieldMarble(m);
            } catch (Exception e) {
                throw new ActionException(e.getMessage());
            }
        } else {
            try {
                boardManager.moveBy(m, 13, true);
            } catch (Exception e) {
                throw new ActionException(e.getMessage());
            }
        }
    }
}