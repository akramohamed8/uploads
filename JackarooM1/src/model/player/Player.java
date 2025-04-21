package model.player;

import java.util.ArrayList;

import exception.GameException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.ActionException;
import model.Colour;
import model.card.Card;

public class Player {
    private final String name;
    private final Colour colour;
    private ArrayList<Card> hand;
    private final ArrayList<Marble> marbles;
    private Card selectedCard;
    private final ArrayList<Marble> selectedMarbles;

    public Player(String name, Colour colour) {
        this.name = name;
        this.colour = colour;
        this.hand = new ArrayList<>();
        this.selectedMarbles = new ArrayList<>();
        this.marbles = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            this.marbles.add(new Marble(colour));
        }

        this.selectedCard = null; 
    }

    public String getName() {
        return name;
    }

    public Colour getColour() {
        return colour;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public void setHand(ArrayList<Card> hand) {
        this.hand = hand;
    }

    public ArrayList<Marble> getMarbles() {
        return marbles;
    }

    public Card getSelectedCard() {
        return selectedCard;
    }

    public void setSelectedCard(Card card) {
        this.selectedCard = card;
    }

    public ArrayList<Marble> getSelectedMarbles() {
        return selectedMarbles;
    }

    public void addSelectedMarble(Marble marble) {
        this.selectedMarbles.add(marble);
    }

    public void clearSelectedMarbles() {
        this.selectedMarbles.clear();
    }

    public void regainMarble(Marble marble) {
        marbles.add(marble);
    }

    public Marble getOneMarble() {
        return marbles.isEmpty() ? null : marbles.get(0);
    }

    public void selectCard(Card card) throws InvalidCardException {
        if (!hand.contains(card)) {
            throw new InvalidCardException("Card not in hand.");
        }
        this.selectedCard = card;
    }

    public void selectMarble(Marble marble) throws InvalidMarbleException {
        if (selectedMarbles.size() >= 2) {
            throw new InvalidMarbleException("Cannot select more than 2 marbles.");
        }
        selectedMarbles.add(marble);
    }

    public void deselectAll() {
        selectedCard = null;
        selectedMarbles.clear();
    }

    public void play() throws GameException {
        if (selectedCard == null) {
            throw new InvalidCardException("No card selected.");
        }

        if (!selectedCard.validateMarbleSize(selectedMarbles)) {
            throw new InvalidMarbleException("Invalid number of marbles selected for this card.");
        }

        if (!selectedCard.validateMarbleColours(selectedMarbles)) {
            throw new InvalidMarbleException("Selected marbles don't match required colours for this card.");
        }

        try {
            selectedCard.act(selectedMarbles);
        } catch (ActionException | InvalidMarbleException e) {
            throw new InvalidCardException("Card action failed: " + e.getMessage());
        }
    }
}