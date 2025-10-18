package core.game;

public class Move {
    private int row1, col1;
    private int row2, col2;
    private boolean isFirstMove;

    // 第一步（单子）
    public Move(int row, int col) {
        this.row1 = row;
        this.col1 = col;
        this.isFirstMove = true;
    }

    // 普通步（双子）
    public Move(int row1, int col1, int row2, int col2) {
        this.row1 = row1;
        this.col1 = col1;
        this.row2 = row2;
        this.col2 = col2;
        this.isFirstMove = false;
    }

    public int getRow1() { return row1; }
    public int getCol1() { return col1; }
    public int getRow2() { return row2; }
    public int getCol2() { return col2; }
    public boolean isFirstMove() { return isFirstMove; }

    @Override
    public String toString() {
        if (isFirstMove) {
            return String.format("(%d,%d)", row1, col1);
        }
        return String.format("(%d,%d),(%d,%d)", row1, col1, row2, col2);
    }
}