package core.game;

import core.board.Board;
import core.board.PieceColor;
import player.AIPlayer;

public class GameController {
    private Board board;
    private AIPlayer blackPlayer;
    private AIPlayer whitePlayer;
    private PieceColor currentPlayer;
    private boolean gameOver;
    private PieceColor winner;
    private int moveCount;

    public GameController(AIPlayer black, AIPlayer white) {
        this.board = new Board();
        this.blackPlayer = black;
        this.whitePlayer = white;

        blackPlayer.setColor(PieceColor.BLACK);
        whitePlayer.setColor(PieceColor.WHITE);
        blackPlayer.setBoard(board);
        whitePlayer.setBoard(board);

        this.currentPlayer = PieceColor.BLACK;
        this.gameOver = false;
        this.moveCount = 0;
    }

    public void playGame() {
        Move lastMove = null;
        boolean isFirstMoveOfGame = true;  // 标记游戏的第一步

        while (!gameOver && moveCount < 181) {
            AIPlayer player = (currentPlayer == PieceColor.BLACK) ? blackPlayer : whitePlayer;
            String playerName = player.getName() + "(" + (currentPlayer == PieceColor.BLACK ? "黑" : "白") + ")";

            System.out.println("\n" + playerName + " 思考中...");

            // 第一步：黑棋只能下一子
            Move move;
            if (isFirstMoveOfGame) {
                move = player.findMove(null);  // 黑棋第一步
                if (move == null || !move.isFirstMove()) {
                    System.out.println("错误：第一步必须只下一子！");
                    break;
                }
                isFirstMoveOfGame = false;
            } else {
                // 后续步骤：每次下两子
                move = player.findMove(lastMove);
            }

            if (move == null) {
                System.out.println("无效移动，游戏结束");
                break;
            }

            // 执行移动
            if (move.isFirstMove()) {
                // 只下一个子（黑棋第一步）
                System.out.println(playerName + " 落子: (" + move.getRow1() + "," + move.getCol1() + ")");
                board.makeMove(move.getRow1(), move.getCol1(), currentPlayer);

                // 检查胜利
                if (board.checkWin(move.getRow1(), move.getCol1(), currentPlayer)) {
                    gameOver = true;
                    winner = currentPlayer;
                    System.out.println("\n═══════════════════════════════");
                    System.out.println(playerName + " 获胜！（六连）");
                    System.out.println("═══════════════════════════════");
                    break;
                }
            } else {
                // 下两个子
                System.out.println(playerName + " 落子: (" + move.getRow1() + "," + move.getCol1() +
                        "), (" + move.getRow2() + "," + move.getCol2() + ")");

                // 下第一个子
                board.makeMove(move.getRow1(), move.getCol1(), currentPlayer);

                // 立即检查第一个子是否获胜
                if (board.checkWin(move.getRow1(), move.getCol1(), currentPlayer)) {
                    gameOver = true;
                    winner = currentPlayer;
                    System.out.println("\n═══════════════════════════════");
                    System.out.println(playerName + " 获胜！（第一子形成六连）");
                    System.out.println("═══════════════════════════════");
                    break;
                }

                // 下第二个子
                board.makeMove(move.getRow2(), move.getCol2(), currentPlayer);

                // 检查第二个子是否获胜
                if (board.checkWin(move.getRow2(), move.getCol2(), currentPlayer)) {
                    gameOver = true;
                    winner = currentPlayer;
                    System.out.println("\n═══════════════════════════════");
                    System.out.println(playerName + " 获胜！（第二子形成六连）");
                    System.out.println("═══════════════════════════════");
                    break;
                }
            }

            // 打印当前棋盘状态（可选）
            // printBoard();

            lastMove = move;
            moveCount++;

            // 切换玩家
            currentPlayer = currentPlayer.opposite();
        }

        if (!gameOver) {
            System.out.println("\n═══════════════════════════════");
            System.out.println("平局！棋盘已满");
            System.out.println("═══════════════════════════════");
        }

        System.out.println("\n总步数: " + moveCount);
    }

    /**
     * 打印棋盘状态（用于调试）
     */
    private void printBoard() {
        System.out.println("\n当前棋盘:");
        System.out.print("   ");
        for (int i = 0; i < Board.SIZE; i++) {
            System.out.printf("%2d ", i);
        }
        System.out.println();

        for (int i = 0; i < Board.SIZE; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor color = board.get(i, j);
                if (color == PieceColor.BLACK) {
                    System.out.print(" ● ");
                } else if (color == PieceColor.WHITE) {
                    System.out.print(" ○ ");
                } else {
                    System.out.print(" · ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public PieceColor getWinner() {
        return winner;
    }

    public Board getBoard() {
        return board;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * 主方法：用于测试AI对战
     */
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("   六子棋 AI 对战测试");
        System.out.println("═══════════════════════════════");

        // 创建两个AI玩家进行对战
        AIPlayer black = new player.AlphaBetaAI("AI-Alpha");
        AIPlayer white = new player.AlphaBetaAI("AI-Beta");

        GameController controller = new GameController(black, white);

        long startTime = System.currentTimeMillis();
        controller.playGame();
        long endTime = System.currentTimeMillis();

        System.out.println("对局用时: " + (endTime - startTime) / 1000.0 + " 秒");

        if (controller.getWinner() != null) {
            String winnerName = (controller.getWinner() == PieceColor.BLACK) ? "黑棋" : "白棋";
            System.out.println("获胜方: " + winnerName);
        }
    }
}