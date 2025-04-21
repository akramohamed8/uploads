package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import engine.board.Board;
import engine.board.SafeZone;
import exception.CannotDiscardException;
import exception.CannotFieldException;
import exception.GameException;
import exception.IllegalDestroyException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import model.Colour;
import model.card.Card;
import model.card.Deck;
import model.player.*;

@SuppressWarnings("unused")
public class Game implements GameManager {
    private final Board board;
    private final ArrayList<Player> players;
	private int currentPlayerIndex;
    private final ArrayList<Card> firePit;
    private int turn;

    public Game(String playerName) throws IOException {
        turn = 0;
        currentPlayerIndex = 0;
        firePit = new ArrayList<>();

        ArrayList<Colour> colourOrder = new ArrayList<>();
        
        colourOrder.addAll(Arrays.asList(Colour.values()));
        
        Collections.shuffle(colourOrder);
        
        this.board = new Board(colourOrder, this);
        
        Deck.loadCardPool(this.board, (GameManager)this);
        
        this.players = new ArrayList<>();
        this.players.add(new Player(playerName, colourOrder.get(0)));
        
        for (int i = 1; i < 4; i++) 
            this.players.add(new CPU("CPU " + i, colourOrder.get(i), this.board));
        
        for (int i = 0; i < 4; i++) 
            this.players.get(i).setHand(Deck.drawCards());
        
    }
    
    public Board getBoard() {
        return board;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<Card> getFirePit() {
        return firePit;
    }
    @Override
    public void sendHome(Marble marble) {
        for (Player player : players) {
            if (player.getColour() == marble.getColour()) {
                player.getMarbles().add(marble);
                break;
            }
        }
    }

    @Override
    public void fieldMarble() throws CannotFieldException, IllegalDestroyException {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (currentPlayer.getMarbles().isEmpty()) {
            throw new CannotFieldException("No marbles in home zone");
        }
        Marble marble = currentPlayer.getMarbles().remove(0);
        board.sendToBase(marble);
    }

    @Override
    public void discardCard(Colour colour) throws CannotDiscardException {
        Player targetPlayer = null;
        for (Player player : players) {
            if (player.getColour() == colour) {
                targetPlayer = player;
                break;
            }
        }
        
        if (targetPlayer == null) {
            throw new CannotDiscardException("Player with color " + colour + " not found");
        }
        
        if (targetPlayer.getHand().isEmpty()) {
            throw new CannotDiscardException("Player has no cards to discard");
        }
        
        int randomIndex = (int)(Math.random() * targetPlayer.getHand().size());
        Card discardedCard = targetPlayer.getHand().remove(randomIndex);
        
        firePit.add(discardedCard);
    }

    @Override
    public void discardCard() throws CannotDiscardException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (currentPlayer.getHand().isEmpty()) {
            throw new CannotDiscardException("Player has no cards to discard");
        }
        
        Card selectedCard = currentPlayer.getSelectedCard();
        
        if (!currentPlayer.getHand().remove(selectedCard)) {
            throw new CannotDiscardException("Selected card not found in player's hand");
        }
        
        firePit.add(selectedCard);
    }

    @Override
    public Colour getActivePlayerColour() {
        return players.get(currentPlayerIndex).getColour();
    }

    @Override
    public Colour getNextPlayerColour() {
        int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return players.get(nextPlayerIndex).getColour();
    }
    public void selectCard(Card card) throws InvalidCardException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (!currentPlayer.getHand().contains(card)) {
            throw new InvalidCardException("Selected card is not in player's hand");
        }
        
        currentPlayer.setSelectedCard(card);
    }

    public void selectMarble(Marble marble) throws InvalidMarbleException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (marble.getColour() != currentPlayer.getColour()) {
            throw new InvalidMarbleException("Cannot select marble of another player");
        }
        
        currentPlayer.addSelectedMarble(marble);
    }

    public void deselectAll() {
        Player currentPlayer = players.get(currentPlayerIndex);
        currentPlayer.setSelectedCard(null);
        currentPlayer.clearSelectedMarbles();
    }

    public void editSplitDistance(int splitDistance) throws SplitOutOfRangeException {
        if (splitDistance < 1 || splitDistance > 6) {
            throw new SplitOutOfRangeException("Split distance must be between 1 and 6");
        }
        
        board.setSplitDistance(splitDistance);
    }

    public boolean canPlayTurn() {
        Player currentPlayer = players.get(currentPlayerIndex);
        return !currentPlayer.getHand().isEmpty();
    }

    public void playPlayerTurn() throws GameException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (currentPlayer instanceof CPU) {
            ((CPU) currentPlayer).play();
        } else {
            if (currentPlayer.getSelectedCard() != null) {
                currentPlayer.getSelectedCard().act(currentPlayer.getSelectedMarbles());
            } else {
                throw new InvalidCardException("No card selected");
            }
        }
    }

    public void endPlayerTurn() throws CannotDiscardException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (currentPlayer.getSelectedCard() != null) {
            discardCard();
        }
        
        deselectAll();
        
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        
        if (currentPlayerIndex == 0) {
            turn++;
        }
        
        if (turn % 4 == 0 && turn > 0) {
            startNewRound();
        }
    }

    private void startNewRound() {
        turn = 0;
        for (Player player : players) {
            player.setHand(Deck.drawCards());
        }
        
        if (Deck.getPoolSize() < 4) {
            Deck.refillPool(firePit);
            firePit.clear();
        }
    }

    public Colour checkWin() {
        for (SafeZone safeZone : board.getSafeZones()) {
            if (safeZone.isFull()) {
                return safeZone.getColour();
            }
        }
        return null;
    }
    public void fieldMarble(Marble marble) throws CannotFieldException, IllegalDestroyException {
        if (!players.get(currentPlayerIndex).getMarbles().contains(marble)) {
            throw new CannotFieldException("Marble not owned by current player");
        }
        players.get(currentPlayerIndex).getMarbles().remove(marble);
        board.sendToBase(marble);
    }
    @Override
    public void discardCard(boolean isRandom) throws CannotDiscardException {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (currentPlayer.getHand().isEmpty()) {
            throw new CannotDiscardException("Player has no cards to discard");
        }
        
        Card cardToDiscard;
        
        if (isRandom) {
            // Randomly select a card from player's hand
            int randomIndex = (int)(Math.random() * currentPlayer.getHand().size());
            cardToDiscard = currentPlayer.getHand().remove(randomIndex);
        } else {
            // Use the currently selected card
            cardToDiscard = currentPlayer.getSelectedCard();
            
            if (!currentPlayer.getHand().remove(cardToDiscard)) {
                throw new CannotDiscardException("Selected card not found in player's hand");
            }
        }
        
        firePit.add(cardToDiscard);
    }
    
    
    
    
}
