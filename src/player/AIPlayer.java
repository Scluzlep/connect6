package player;

import core.board.Board;
import core.board.PieceColor;
import core.game.Move;

public abstract class AIPlayer {
    protected PieceColor color;
    protected Board board;
    protected String playerName;

    public AIPlayer(String name) {
        this.playerName = name;
    }

    public void setColor(PieceColor color) {
        this.color = color;
    }

    public PieceColor getColor() {
        return color;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public String getName() {
        return playerName;
    }

    public abstract Move findMove(Move opponentMove);

    protected Move firstMove() {
        return new Move(Board.SIZE / 2, Board.SIZE / 2);
    }
}