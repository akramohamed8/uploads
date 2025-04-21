package model.card.standard;

import java.util.ArrayList;

import engine.GameManager;
import engine.board.BoardManager;
import exception.ActionException;
import model.card.Card;
import model.player.Marble;

public class Standard extends Card {
    private final int rank;
    private final Suit suit;

    public Standard(String name, String description, int rank, Suit suit, BoardManager boardManager, GameManager gameManager) {
        super(name, description, boardManager, gameManager);
        this.rank = rank;
        this.suit = suit;
    }

    public int getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }
    
    @Override
    protected int getExpectedMarbleCount() {
        return 1;
    }
    
    @Override
    public void act(ArrayList<Marble> marbles) throws ActionException {
        try {
            boardManager.moveBy(marbles.get(0), rank, false);
        } catch (Exception e) {
            throw new ActionException(e.getMessage());
        }
    }
}