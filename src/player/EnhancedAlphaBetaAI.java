package player;

import core.board.Board;
import core.board.PieceColor;
import core.game.Move;
import java.util.*;

/**
 * 增强版 Alpha-Beta AI
 * 结合了威胁检测、路表评估和迭代深化搜索
 */
public class EnhancedAlphaBetaAI extends AIPlayer {
    private static final int MAX_DEPTH = 4;  // 增加搜索深度
    private static final int INF = 10000000;
    private static final int WIN_SCORE = 1000000;
    
    // 威胁等级定义
    private static final int THREAT_WIN = 100000;      // 必胜威胁（活四、双活三等）
    private static final int THREAT_URGENT = 50000;    // 紧急威胁（冲四、活三）
    private static final int THREAT_IMPORTANT = 10000; // 重要威胁（活二、眠三）
    
    private Map<Long, Integer> transpositionTable;  // 置换表（记忆化）
    private int searchDepth;

    public EnhancedAlphaBetaAI(String name) {
        super(name);
        this.transpositionTable = new HashMap<>();
        this.searchDepth = MAX_DEPTH;
    }

    @Override
    public Move findMove(Move opponentMove) {
        // 1. 处理对手移动
        if (opponentMove != null) {
            PieceColor opponentColor = color.opposite();
            board.makeMove(opponentMove.getRow1(), opponentMove.getCol1(), opponentColor);
            if (!opponentMove.isFirstMove()) {
                board.makeMove(opponentMove.getRow2(), opponentMove.getCol2(), opponentColor);
            }
        }

        // 2. 如果是黑棋第一步
        if (color == PieceColor.BLACK && opponentMove == null) {
            Move firstMove = firstMove();
            board.makeMove(firstMove.getRow1(), firstMove.getCol1(), color);
            return firstMove;
        }

        // 3. 检查是否有必胜走法或必须防守
        Move criticalMove = findCriticalMove();
        if (criticalMove != null) {
            applyMoveToBoard(criticalMove);
            return criticalMove;
        }

        // 4. 迭代深化搜索（从浅到深）
        Move bestMove = null;
        for (int depth = 2; depth <= searchDepth; depth++) {
            transpositionTable.clear();  // 清空置换表
            Move currentBest = iterativeDeepeningSearch(depth);
            if (currentBest != null) {
                bestMove = currentBest;
            }
        }

        if (bestMove == null) {
            bestMove = generateReasonableMove();
        }

        applyMoveToBoard(bestMove);
        return bestMove;
    }

    /**
     * 迭代深化搜索
     */
    private Move iterativeDeepeningSearch(int depth) {
        List<ScoredMove> candidates = generateScoredMoves();
        
        if (candidates.isEmpty()) {
            return generateReasonableMove();
        }

        Move bestMove = null;
        int bestScore = -INF;

        for (ScoredMove sm : candidates) {
            Board tempBoard = board.clone();
            tempBoard.makeMove(sm.move.getRow1(), sm.move.getCol1(), color);
            if (!sm.move.isFirstMove()) {
                tempBoard.makeMove(sm.move.getRow2(), sm.move.getCol2(), color);
            }

            int score = -alphaBetaWithMemory(tempBoard, depth - 1, -INF, INF, color.opposite());

            if (score > bestScore) {
                bestScore = score;
                bestMove = sm.move;
            }
        }

        return bestMove;
    }

    /**
     * 带记忆化的 Alpha-Beta 搜索
     */
    private int alphaBetaWithMemory(Board board, int depth, int alpha, int beta, PieceColor player) {
        // 检查置换表
        long boardHash = calculateBoardHash(board);
        if (transpositionTable.containsKey(boardHash)) {
            return transpositionTable.get(boardHash);
        }

        // 检查终止条件
        PieceColor winner = checkWinner(board);
        if (winner != null) {
            int value = (winner == color) ? WIN_SCORE : -WIN_SCORE;
            transpositionTable.put(boardHash, value);
            return value;
        }

        if (depth == 0) {
            int value = evaluateBoard(board);
            transpositionTable.put(boardHash, value);
            return value;
        }

        List<ScoredMove> moves = generateScoredMovesForBoard(board, player);
        
        if (moves.isEmpty()) {
            return 0;
        }

        int value;
        if (player == color) {
            value = -INF;
            for (ScoredMove sm : moves) {
                Board tempBoard = board.clone();
                tempBoard.makeMove(sm.move.getRow1(), sm.move.getCol1(), player);
                if (!sm.move.isFirstMove()) {
                    tempBoard.makeMove(sm.move.getRow2(), sm.move.getCol2(), player);
                }

                value = Math.max(value, alphaBetaWithMemory(tempBoard, depth - 1, alpha, beta, player.opposite()));
                alpha = Math.max(alpha, value);
                
                if (beta <= alpha) {
                    break;  // Beta剪枝
                }
            }
        } else {
            value = INF;
            for (ScoredMove sm : moves) {
                Board tempBoard = board.clone();
                tempBoard.makeMove(sm.move.getRow1(), sm.move.getCol1(), player);
                if (!sm.move.isFirstMove()) {
                    tempBoard.makeMove(sm.move.getRow2(), sm.move.getCol2(), player);
                }

                value = Math.min(value, alphaBetaWithMemory(tempBoard, depth - 1, alpha, beta, player.opposite()));
                beta = Math.min(beta, value);
                
                if (beta <= alpha) {
                    break;  // Alpha剪枝
                }
            }
        }

        transpositionTable.put(boardHash, value);
        return value;
    }

    /**
     * 寻找关键走法（必胜或必防）
     */
    private Move findCriticalMove() {
        // 1. 检查是否有必胜走法（我方可以形成六连）
        Move winningMove = findWinningMove(color);
        if (winningMove != null) {
            return winningMove;
        }

        // 2. 检查是否需要防守（对方威胁）
        Move blockingMove = findWinningMove(color.opposite());
        if (blockingMove != null) {
            return blockingMove;
        }

        // 3. 检查双威胁走法
        Move doubleThreatMove = findDoubleThreatMove();
        if (doubleThreatMove != null) {
            return doubleThreatMove;
        }

        return null;
    }

    /**
     * 寻找能直接获胜的走法
     */
    private Move findWinningMove(PieceColor playerColor) {
        List<int[]> emptyPositions = getAllEmptyPositions();
        
        // 尝试每个空位的组合
        for (int i = 0; i < Math.min(emptyPositions.size(), 20); i++) {
            for (int j = i + 1; j < Math.min(emptyPositions.size(), 20); j++) {
                int[] pos1 = emptyPositions.get(i);
                int[] pos2 = emptyPositions.get(j);
                
                Board testBoard = board.clone();
                testBoard.makeMove(pos1[0], pos1[1], playerColor);
                
                if (testBoard.checkWin(pos1[0], pos1[1], playerColor)) {
                    return new Move(pos1[0], pos1[1], pos2[0], pos2[1]);
                }
                
                testBoard.makeMove(pos2[0], pos2[1], playerColor);
                
                if (testBoard.checkWin(pos2[0], pos2[1], playerColor)) {
                    return new Move(pos1[0], pos1[1], pos2[0], pos2[1]);
                }
            }
        }
        
        return null;
    }

    /**
     * 寻找双威胁走法（形成两个活四等）
     */
    private Move findDoubleThreatMove() {
        List<ScoredMove> moves = generateScoredMoves();
        
        for (ScoredMove sm : moves) {
            if (sm.score >= THREAT_URGENT) {
                Board testBoard = board.clone();
                testBoard.makeMove(sm.move.getRow1(), sm.move.getCol1(), color);
                if (!sm.move.isFirstMove()) {
                    testBoard.makeMove(sm.move.getRow2(), sm.move.getCol2(), color);
                }
                
                // 检查是否形成多个威胁
                int threatCount = countThreats(testBoard, color);
                if (threatCount >= 2) {
                    return sm.move;
                }
            }
        }
        
        return null;
    }

    /**
     * 生成带评分的候选走法
     */
    private List<ScoredMove> generateScoredMoves() {
        return generateScoredMovesForBoard(board, color);
    }

    private List<ScoredMove> generateScoredMovesForBoard(Board board, PieceColor player) {
        List<ScoredMove> scoredMoves = new ArrayList<>();
        List<int[]> candidates = new ArrayList<>();

        // 收集候选位置（有邻居的空位）
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (board.get(i, j) == PieceColor.EMPTY && hasStrategicValue(board, i, j)) {
                    candidates.add(new int[]{i, j});
                }
            }
        }

        // 限制候选数量
        int limit = Math.min(candidates.size(), 25);
        
        // 生成双子组合并评分
        for (int i = 0; i < limit; i++) {
            for (int j = i + 1; j < limit; j++) {
                int[] pos1 = candidates.get(i);
                int[] pos2 = candidates.get(j);
                Move move = new Move(pos1[0], pos1[1], pos2[0], pos2[1]);
                
                int score = quickEvaluateMove(board, move, player);
                scoredMoves.add(new ScoredMove(move, score));
            }
        }

        // 按分数降序排序
        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        
        // 只保留前15个最佳候选
        return scoredMoves.subList(0, Math.min(15, scoredMoves.size()));
    }

    /**
     * 判断位置是否有战略价值
     */
    private boolean hasStrategicValue(Board board, int row, int col) {
        // 检查周围是否有棋子
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int r = row + dr;
                int c = col + dc;
                if (board.isValid(r, c) && board.get(r, c) != PieceColor.EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 快速评估走法（不深度搜索）
     */
    private int quickEvaluateMove(Board board, Move move, PieceColor player) {
        Board testBoard = board.clone();
        testBoard.makeMove(move.getRow1(), move.getCol1(), player);
        if (!move.isFirstMove()) {
            testBoard.makeMove(move.getRow2(), move.getCol2(), player);
        }
        
        return evaluateBoard(testBoard);
    }

    /**
     * 增强的棋盘评估函数 - 基于路表
     */
    private int evaluateBoard(Board board) {
        int myScore = evaluatePlayer(board, color);
        int oppScore = evaluatePlayer(board, color.opposite());
        return myScore - oppScore;
    }

    private int evaluatePlayer(Board board, PieceColor player) {
        int score = 0;
        
        // 评估所有可能的六连线（路表）
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                score += evaluateRoadsFromPosition(board, i, j, player);
            }
        }
        
        return score;
    }

    /**
     * 评估从某位置出发的所有路线
     */
    private int evaluateRoadsFromPosition(Board board, int row, int col, PieceColor player) {
        int score = 0;
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        
        for (int[] dir : directions) {
            score += evaluateRoad(board, row, col, dir[0], dir[1], player);
        }
        
        return score;
    }

    /**
     * 评估单条路线（六连线）
     */
    private int evaluateRoad(Board board, int row, int col, int dr, int dc, PieceColor player) {
        int myCount = 0, oppCount = 0, empty = 0;
        
        for (int i = 0; i < 6; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            
            if (!board.isValid(r, c)) return 0;
            
            PieceColor piece = board.get(r, c);
            if (piece == player) {
                myCount++;
            } else if (piece == player.opposite()) {
                oppCount++;
            } else {
                empty++;
            }
        }
        
        // 如果被对方阻断，不计分
        if (myCount > 0 && oppCount > 0) return 0;
        
        // 根据棋型评分
        if (myCount == 6) return WIN_SCORE;
        if (myCount == 5 && empty == 1) return THREAT_WIN;      // 活五
        if (myCount == 4 && empty == 2) return THREAT_URGENT;   // 活四
        if (myCount == 4 && empty == 1) return THREAT_URGENT / 2; // 冲四
        if (myCount == 3 && empty == 3) return THREAT_IMPORTANT; // 活三
        if (myCount == 3 && empty == 2) return THREAT_IMPORTANT / 2; // 眠三
        if (myCount == 2 && empty == 4) return 500;              // 活二
        if (myCount == 1 && empty == 5) return 50;               // 单子
        
        return myCount * 10;
    }

    /**
     * 计算威胁数量
     */
    private int countThreats(Board board, PieceColor player) {
        int threats = 0;
        
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                int posScore = evaluateRoadsFromPosition(board, i, j, player);
                if (posScore >= THREAT_IMPORTANT) {
                    threats++;
                }
            }
        }
        
        return threats;
    }

    /**
     * 计算棋盘哈希值（用于置换表）
     */
    private long calculateBoardHash(Board board) {
        long hash = 0;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor piece = board.get(i, j);
                if (piece != PieceColor.EMPTY) {
                    hash = hash * 3 + (piece == PieceColor.BLACK ? 1 : 2);
                    hash += i * 19 + j;
                }
            }
        }
        return hash;
    }

    private PieceColor checkWinner(Board board) {
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor color = board.get(i, j);
                if (color != PieceColor.EMPTY && board.checkWin(i, j, color)) {
                    return color;
                }
            }
        }
        return null;
    }

    private List<int[]> getAllEmptyPositions() {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (board.get(i, j) == PieceColor.EMPTY) {
                    positions.add(new int[]{i, j});
                }
            }
        }
        return positions;
    }

    private Move generateReasonableMove() {
        List<int[]> empty = getAllEmptyPositions();
        if (empty.size() >= 2) {
            return new Move(empty.get(0)[0], empty.get(0)[1], 
                          empty.get(1)[0], empty.get(1)[1]);
        }
        return new Move(9, 9);
    }

    private void applyMoveToBoard(Move move) {
        if (move != null) {
            board.makeMove(move.getRow1(), move.getCol1(), color);
            if (!move.isFirstMove()) {
                board.makeMove(move.getRow2(), move.getCol2(), color);
            }
        }
    }

    /**
     * 辅助类：带评分的走法
     */
    private static class ScoredMove {
        Move move;
        int score;

        ScoredMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}