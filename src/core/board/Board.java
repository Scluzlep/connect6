package core.board;

public class Board {
    public static final int SIZE = 19;
    private PieceColor[][] board;
    private int moveCount;

    public Board() {
        board = new PieceColor[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = PieceColor.EMPTY;
            }
        }
        moveCount = 0;
    }

    public boolean makeMove(int row, int col, PieceColor color) {
        if (isValid(row, col) && board[row][col] == PieceColor.EMPTY) {
            board[row][col] = color;
            moveCount++;
            return true;
        }
        return false;
    }

    public boolean isValid(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public PieceColor get(int row, int col) {
        if (!isValid(row, col)) return PieceColor.EMPTY;
        return board[row][col];
    }

    public boolean checkWin(int row, int col, PieceColor color) {
        return checkDirection(row, col, 0, 1, color) || // 横向
               checkDirection(row, col, 1, 0, color) || // 纵向
               checkDirection(row, col, 1, 1, color) || // 斜向
               checkDirection(row, col, 1, -1, color);  // 反斜向
    }

    private boolean checkDirection(int row, int col, int dr, int dc, PieceColor color) {
        int count = 1;
        
        for (int i = 1; i < 6; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            if (isValid(r, c) && board[r][c] == color) {
                count++;
            } else {
                break;
            }
        }
        
        for (int i = 1; i < 6; i++) {
            int r = row - i * dr;
            int c = col - i * dc;
            if (isValid(r, c) && board[r][c] == color) {
                count++;
            } else {
                break;
            }
        }
        
        return count >= 6;
    }

    public Board clone() {
        Board newBoard = new Board();
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(this.board[i], 0, newBoard.board[i], 0, SIZE);
        }
        newBoard.moveCount = this.moveCount;
        return newBoard;
    }

    public int getMoveCount() {
        return moveCount;
    }
}