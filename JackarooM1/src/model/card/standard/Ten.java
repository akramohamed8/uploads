package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.player.Marble;

public class Ten extends Standard {

    public Ten(String name, String description, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, 10, suit, boardManager, gameManager);
    }
    
    protected int getExpectedMarbleCount() {
        return 0;
    }

    
    public void act(ArrayList<Marble> marbles) throws ActionException {
        try {
            game.discardCard(false); // From next player
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }

}
