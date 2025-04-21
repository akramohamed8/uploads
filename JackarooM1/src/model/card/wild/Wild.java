package model.card.wild;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import exception.InvalidMarbleException;
import model.card.Card;
import model.player.Marble;

public abstract class Wild extends Card {

    public Wild(String name, String description, BoardManager boardManager, GameManager gameManager) {
        super(name, description, boardManager, gameManager);
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return 0; 
    }
    
    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException, InvalidMarbleException {
        throw new ActionException("This wild card's action is not implemented.");
    }
}