package player;

import core.board.Board;
import core.board.PieceColor;
import core.game.Move;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 高级混合策略AI（完整优化版 V2）:
 * 1. 迭代深化Alpha-Beta剪枝（修复版，深度6层）
 * 2. 中后局蒙特卡洛树搜索（MCTS）
 * 3. 高级棋型识别与威胁分析
 * 4. 分离式评估与自适应权重
 * 5.  【修正版】增强防守策略与紧急威胁应对：
 *     - 引入威胁对象（Threat），精确识别单个威胁（如活四、活三）。
 *     - 智能防守决策，确保正确封堵活四两端，或同时应对多个不同威胁。
 */
public class AlphaBetaAI extends AIPlayer {
    // ================== 搜索与评估参数（与前一版相同） ==================
    private static final int MAX_DEPTH = 6;
    private static final int SEARCH_TIMEOUT_MS = 9800;
    private static final int INF = 10_000_000;
    private static final int WIN_SCORE = 5_000_000;
    private static final int MCTS_TURN_THRESHOLD = 5;
    private static final double UCB_C_BASE = 1.5;
    private static final double UCB_C_ADJUST_FACTOR = 0.15;
    private static final int MCTS_SIMULATION_DEPTH = 12;
    private static final int MCTS_SIMULATION_COUNT = 25000;
    private static final long ITERATION_TIME_RESERVE = 1000;
    private static final long GREEDY_UPDATE_INTERVAL = 50;
    private static final int[] SCORE_SELF = {
            5000000, 5000000, 25000, 800, 0, 20000, 1500, 0, 600, 400, 200, 0, 500, 2, 0
    };
    private static final int[] SCORE_OPPONENT = {
            1000000, 1000000, 1000000, 1000000, 0, 1000000, 1000000, 0, 700, 500, 300, 0, 600, 10, 0
    };
    private static final int PATTERN_SIX = 0;
    private static final int PATTERN_LONG = 1;
    private static final int PATTERN_LIVE_FIVE = 2;
    private static final int PATTERN_SLEEP_FIVE = 3;
    private static final int PATTERN_DEAD_FIVE = 4;
    private static final int PATTERN_LIVE_FOUR = 5;
    private static final int PATTERN_SLEEP_FOUR = 6;
    private static final int PATTERN_DEAD_FOUR = 7;
    private static final int PATTERN_LIVE_THREE = 8;
    private static final int PATTERN_HAZY_THREE = 9;
    private static final int PATTERN_SLEEP_THREE = 10;
    private static final int PATTERN_DEAD_THREE = 11;
    private static final int PATTERN_LIVE_TWO = 12;
    private static final int PATTERN_SLEEP_TWO = 13;
    private static final int PATTERN_DEAD_TWO = 14;
    private static final int THREAT_WIN = 500_000;
    private static final int THREAT_URGENT = 50_000;
    private static final int THREAT_IMPORTANT = 10_000;
    private static final double OPP_THREAT_BIAS = 0.45;
    private static final int[][] POSITION_SCORE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 5, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 0},
            {0, 3, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 3, 0},
            {0, 3, 5, 10, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 10, 5, 3, 0},
            {0, 3, 5, 8, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 8, 5, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 16, 16, 16, 16, 16, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 20, 20, 20, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 24, 24, 24, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 24, 30, 24, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 24, 30, 24, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 24, 24, 24, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 20, 20, 20, 20, 20, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 16, 16, 16, 16, 16, 16, 16, 12, 8, 5, 3, 3, 0, 0},
            {0, 3, 5, 8, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 8, 5, 3, 0, 0},
            {0, 3, 5, 10, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 10, 5, 3, 0},
            {0, 3, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 3, 0},
            {0, 5, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    // ================== 核心数据结构（与前一版相同） ==================
    private static class CacheEntry {
        final int score;
        final int depth;
        final int flag;

        CacheEntry(int score, int depth, int flag) {
            this.score = score;
            this.depth = depth;
            this.flag = flag;
        }
    }

    private final ConcurrentHashMap<Long, CacheEntry> transpositionTable = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    private int turnCount = 0;
    private Map<String, Integer> pathValueCache = new HashMap<>();
    private int nodesExplored = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;

    public AlphaBetaAI(String name) {
        super(name);
    }

    @Override
    public Move findMove(Move opponentMove) {
        turnCount++;
        long startTime = System.currentTimeMillis();
        System.out.println("=== 第 " + turnCount + " 回合 ===");

        transpositionTable.clear();
        pathValueCache.clear();
        nodesExplored = 0;
        cacheHits = 0;
        cacheMisses = 0;

        if (opponentMove != null) {
            PieceColor opponentColor = color.opposite();
            board.makeMove(opponentMove.getRow1(), opponentMove.getCol1(), opponentColor);
            if (!opponentMove.isFirstMove()) {
                board.makeMove(opponentMove.getRow2(), opponentMove.getCol2(), opponentColor);
            }
        }

        // 1. 【新】检查我方是否有必胜走法 (最高优先级)
        Move winningMove = findImmediateThreatMove(board, color);
        if (winningMove != null) {
            System.out.println("AI检测到必胜走法，直接执行: " + moveToString(winningMove));
            applyMove(winningMove);
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("本回合决策用时: " + totalTime + "ms (快速决策)");
            return winningMove;
        }

        // 2. 【新】检查对方是否有必胜走法，必须立即阻挡 (次高优先级)
        Move blockingMove = findImmediateThreatMove(board, color.opposite());
        if (blockingMove != null) {
            System.out.println("AI检测到对方必胜，必须防守: " + moveToString(blockingMove));
            applyMove(blockingMove);
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("本回合决策用时: " + totalTime + "ms (快速决策)");
            return blockingMove;
        }

        // 3. 收集其他“重要但不绝对”的关键走法，注入到搜索中
        List<Move> criticalCandidates = findOtherCriticalMoves(board);

        Move finalMove;

        // 根据回合数选择搜索策略
        if (turnCount <= MCTS_TURN_THRESHOLD) {
            System.out.println("=== 开局阶段：使用混合策略（贪心为主，剪枝为辅） ===");
            finalMove = hybridSearch(board, criticalCandidates);
        } else {
            System.out.println("=== 中后局阶段：使用蒙特卡洛树搜索（MCTS） ===");
            finalMove = mctsSearch(board);
        }

        if (finalMove == null || !isValidMove(board, finalMove)) {
            System.out.println("警告：选择的走法无效，使用增强版兜底策略");
            finalMove = enhancedFallbackMove(board);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("本回合决策用时: " + totalTime + "ms, 搜索节点数: " + nodesExplored +
                ", 缓存命中率: " + String.format("%.2f%%", cacheHits * 100.0 / (cacheHits + cacheMisses + 1)));

        applyMove(finalMove);
        return finalMove;
    }

    private boolean isValidMove(Board b, Move move) {
        if (move == null) return false;
        if (move.getRow1() < 0 || move.getRow1() >= Board.SIZE || move.getCol1() < 0 || move.getCol1() >= Board.SIZE || b.get(move.getRow1(), move.getCol1()) != PieceColor.EMPTY) {
            return false;
        }
        if (move.isFirstMove()) return true;
        return move.getRow2() >= 0 && move.getRow2() < Board.SIZE && move.getCol2() >= 0 && move.getCol2() < Board.SIZE && b.get(move.getRow2(), move.getCol2()) == PieceColor.EMPTY;
    }

    // ===================== MCTS 蒙特卡洛树搜索（与前一版相同） =====================
    private class MCTSNode {
        PieceColor player;
        Move move1;
        Move move2;
        MCTSNode parent;
        List<MCTSNode> children;
        int visitCount;
        int winCount;
        boolean isTerminal;
        PieceColor winner;

        MCTSNode(PieceColor player, Move move1, Move move2, MCTSNode parent) {
            this.player = player;
            this.move1 = move1;
            this.move2 = move2;
            this.parent = parent;
            this.children = new ArrayList<>();
            this.visitCount = 0;
            this.winCount = 0;
            this.isTerminal = false;
            this.winner = null;
        }

        double getUCB(int totalVisits, int depth) {
            if (visitCount == 0) return Double.MAX_VALUE;
            double dynamic_C = UCB_C_BASE * Math.exp(-UCB_C_ADJUST_FACTOR * depth);
            double exploitation = (double) winCount / visitCount;
            double exploration = dynamic_C * Math.sqrt(Math.log(totalVisits) / visitCount);
            return exploitation + exploration;
        }

        void update(PieceColor loser) {
            visitCount += 2;
            if (loser == null) {
                winCount += 1;
            } else if (loser != player) {
                winCount += 2;
            }
        }

        MCTSNode selectBestChild(int depth) {
            MCTSNode best = null;
            double bestUCB = -1;
            for (MCTSNode child : children) {
                if (child.visitCount == 0) return child;
                double ucb = child.getUCB(visitCount, depth);
                if (ucb > bestUCB) {
                    bestUCB = ucb;
                    best = child;
                }
            }
            return best;
        }
    }

    private Move mctsSearch(Board b) {
        long startTime = System.currentTimeMillis();
        MCTSNode root = new MCTSNode(color, null, null, null);
        int iterations = 0;
        while (iterations < MCTS_SIMULATION_COUNT && System.currentTimeMillis() - startTime < SEARCH_TIMEOUT_MS) {
            Board simulationBoard = b.clone();
            MCTSNode selected = select(root, simulationBoard, 0);
            if (selected.visitCount == 0 || selected.isTerminal) {
                PieceColor loser = simulate(simulationBoard, selected.player.opposite(), iterations);
                backpropagate(selected, loser);
            } else {
                expand(selected, simulationBoard);
                if (!selected.children.isEmpty()) {
                    MCTSNode child = selected.children.get(0);
                    applyMoveToBoard(simulationBoard, child.move1, selected.player);
                    applyMoveToBoard(simulationBoard, child.move2, selected.player);
                    PieceColor loser = simulate(simulationBoard, selected.player.opposite(), iterations);
                    backpropagate(child, loser);
                }
            }
            iterations++;
            if (iterations % 1000 == 0) {
                System.out.println("[MCTS] 已完成 " + iterations + " 次模拟");
            }
        }
        System.out.println("[MCTS] 完成 " + iterations + " 次模拟");
        MCTSNode bestChild = null;
        int maxVisits = -1;
        System.out.println("[MCTS] 前5个最佳候选走法:");
        List<MCTSNode> topNodes = new ArrayList<>(root.children);
        topNodes.sort((a, n) -> Integer.compare(n.visitCount, a.visitCount));
        int showCount = Math.min(5, topNodes.size());
        for (int i = 0; i < showCount; i++) {
            MCTSNode child = topNodes.get(i);
            System.out.println("[MCTS] #" + (i + 1) + ": " + moveToString(child.move1) + " + " + moveToString(child.move2) + " 访问: " + child.visitCount + " 胜率: " + String.format("%.2f%%", 100.0 * child.winCount / Math.max(1, child.visitCount)));
            if (child.visitCount > maxVisits) {
                maxVisits = child.visitCount;
                bestChild = child;
            }
        }
        if (bestChild == null) {
            System.out.println("[MCTS] 未找到最佳走法，使用增强版兜底策略");
            return enhancedFallbackMove(b);
        }
        Move result = new Move(bestChild.move1.getRow1(), bestChild.move1.getCol1(), bestChild.move2.getRow1(), bestChild.move2.getCol1());
        System.out.println("[MCTS] 最终选择: " + moveToString(result) + " 访问: " + maxVisits + " 胜率: " + String.format("%.2f%%", 100.0 * bestChild.winCount / Math.max(1, bestChild.visitCount)));
        return result;
    }

    private MCTSNode select(MCTSNode node, Board b, int depth) {
        while (!node.children.isEmpty() && !node.isTerminal) {
            MCTSNode selected = node.selectBestChild(depth);
            if (selected.visitCount == 0) return selected;
            applyMoveToBoard(b, selected.move1, node.player);
            applyMoveToBoard(b, selected.move2, node.player);
            node = selected;
            depth++;
        }
        return node;
    }

    private void expand(MCTSNode node, Board b) {
        PieceColor winner = checkWinner(b);
        if (winner != null) {
            node.isTerminal = true;
            node.winner = winner;
            return;
        }
        List<ScoredMove> candidates = generateScoredMovesForBoard(b, node.player);
        if (candidates.isEmpty()) {
            node.isTerminal = true;
            return;
        }
        int expandLimit = Math.min(12, candidates.size());
        Set<String> visited = new HashSet<>();
        for (int i = 0; i < expandLimit; i++) {
            Move move1 = candidates.get(i).move;
            Board temp1 = b.clone();
            applyMoveToBoard(temp1, move1, node.player);
            if (checkWinner(temp1) == node.player) {
                MCTSNode winChild = new MCTSNode(node.player.opposite(), move1, candidates.get(i == 0 ? 1 : i - 1).move, node);
                winChild.isTerminal = true;
                winChild.winner = node.player;
                node.children.add(winChild);
                break;
            }
            int secondMoveRange = Math.min(15, candidates.size());
            for (int j = 0; j < secondMoveRange; j++) {
                if (i == j) continue;
                Move move2 = candidates.get(j).move;
                String key = getMovePairKey(move1, move2);
                if (visited.contains(key)) continue;
                visited.add(key);
                Board temp2 = temp1.clone();
                applyMoveToBoard(temp2, move2, node.player);
                if (checkWinner(temp2) == node.player) {
                    MCTSNode winChild = new MCTSNode(node.player.opposite(), move1, move2, node);
                    winChild.isTerminal = true;
                    winChild.winner = node.player;
                    node.children.add(winChild);
                    continue;
                }
                MCTSNode child = new MCTSNode(node.player.opposite(), move1, move2, node);
                node.children.add(child);
                if (node.children.size() >= 20) return;
            }
        }
    }

    private PieceColor simulate(Board b, PieceColor currentPlayer, int iteration) {
        int depth = 0;
        Random random = new Random();
        Board simBoard = b.clone();
        int maxSimDepth = MCTS_SIMULATION_DEPTH;
        while (depth < maxSimDepth) {
            PieceColor winner = checkWinner(simBoard);
            if (winner != null) {
                return winner.opposite();
            }
            List<int[]> candidates = getEmptyPositionsNearPieces(simBoard, 2);
            if (candidates.size() < 2) {
                List<int[]> allEmpty = new ArrayList<>();
                for (int r = 0; r < Board.SIZE; r++) {
                    for (int c = 0; c < Board.SIZE; c++) {
                        if (simBoard.get(r, c) == PieceColor.EMPTY) {
                            allEmpty.add(new int[]{r, c});
                        }
                    }
                }
                if (allEmpty.size() < 2) return null;
                candidates = allEmpty;
            }
            int idx1 = random.nextInt(candidates.size());
            int[] pos1 = candidates.get(idx1);
            int idx2 = random.nextInt(candidates.size());
            while (idx1 == idx2) {
                idx2 = random.nextInt(candidates.size());
            }
            int[] pos2 = candidates.get(idx2);
            simBoard.makeMove(pos1[0], pos1[1], currentPlayer);
            simBoard.makeMove(pos2[0], pos2[1], currentPlayer);
            currentPlayer = currentPlayer.opposite();
            depth++;
        }
        int finalScore = evaluate(simBoard);
        if (finalScore > WIN_SCORE / 10) return color.opposite();
        if (finalScore < -WIN_SCORE / 10) return color;
        return null;
    }

    private void backpropagate(MCTSNode node, PieceColor loser) {
        while (node != null) {
            node.update(loser);
            node = node.parent;
        }
    }

    private String getMovePairKey(Move m1, Move m2) {
        int r1 = m1.getRow1(), c1 = m1.getCol1();
        int r2 = m2.getRow1(), c2 = m2.getCol1();
        if (r1 > r2 || (r1 == r2 && c1 > c2)) {
            int tr = r1, tc = c1;
            r1 = r2;
            c1 = c2;
            r2 = tr;
            c2 = tc;
        }
        return r1 + "," + c1 + "-" + r2 + "," + c2;
    }

    // ===================== 混合搜索、PVS等（与前一版相同） =====================
    private Move hybridSearch(Board b, List<Move> injectedCandidates) {
        long startTime = System.currentTimeMillis();
        AtomicReference<Move> alphaBetaBestMove = new AtomicReference<>(null);
        AtomicReference<Integer> alphaBetaBestScore = new AtomicReference<>(-INF);
        AtomicBoolean alphaBetaFinished = new AtomicBoolean(false);
        AtomicReference<Move> greedyBestMove = new AtomicReference<>(null);
        AtomicReference<Integer> greedyBestScore = new AtomicReference<>(-INF);

        Future<?> alphaBetaFuture = executor.submit(() -> {
            try {
                System.out.println("[剪枝搜索] 开始并行计算...");
                for (int depth = 2; depth <= MAX_DEPTH; depth++) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime + estimateSearchTime(depth) + ITERATION_TIME_RESERVE > SEARCH_TIMEOUT_MS) {
                        System.out.println("[剪枝搜索] 预估深度 " + depth + " 将超时，停止迭代。");
                        break;
                    }
                    System.out.println("[剪枝搜索] 深度 " + depth + " 搜索中...");
                    Move currentBest = parallelRootSearch(b, depth, injectedCandidates);
                    if (currentBest != null) {
                        Board tempBoard = b.clone();
                        applyMoveToBoard(tempBoard, currentBest, color);
                        int score = evaluate(tempBoard); // 使用主评估函数获取最终分数
                        alphaBetaBestMove.set(currentBest);
                        alphaBetaBestScore.set(score);
                        System.out.println("[剪枝搜索] 更新深度 " + depth + " 最佳走法: " + moveToString(currentBest) + "，分数: " + score);
                    }
                }
                alphaBetaFinished.set(true);
                System.out.println("[剪枝搜索] 完成。");
            } catch (Exception e) {
                System.out.println("[剪枝搜索] 异常: " + e.getMessage());
            }
        });

        Future<?> greedyFuture = executor.submit(() -> {
            try {
                System.out.println("[贪心算法] 开始并行计算...");
                while (!alphaBetaFinished.get() && !Thread.currentThread().isInterrupted()) {
                    Move currentGreedyMove = parallelGreedySearch(b);
                    if (currentGreedyMove != null) {
                        Board tempBoard = b.clone();
                        applyMoveToBoard(tempBoard, currentGreedyMove, color);
                        int score = evaluate(tempBoard);
                        if (score > greedyBestScore.get()) {
                            greedyBestMove.set(currentGreedyMove);
                            greedyBestScore.set(score);
                        }
                    }
                    Thread.sleep(GREEDY_UPDATE_INTERVAL);
                }
                System.out.println("[贪心算法] 完成。");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[贪心算法] 异常: " + e.getMessage());
            }
        });

        try {
            alphaBetaFuture.get(SEARCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("[混合策略] 剪枝搜索超过时间限制，终止搜索");
            alphaBetaFuture.cancel(true);
        } catch (Exception e) {
            System.out.println("[混合策略] 剪枝搜索异常: " + e.getMessage());
        }

        greedyFuture.cancel(true);
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("[混合策略] 总耗时: " + elapsedTime + "ms");

        // 【新决策逻辑】
        Move alphaBetaMove = alphaBetaBestMove.get();
        int alphaBetaScore = alphaBetaBestScore.get();
        Move greedyMove = greedyBestMove.get();
        int greedyScore = greedyBestScore.get();

        System.out.println("=== 结果比较 ===");
        System.out.println("[剪枝搜索] 最佳走法: " + moveToString(alphaBetaMove) + "，分数: " + alphaBetaScore);
        System.out.println("[贪心算法] 最佳走法: " + moveToString(greedyMove) + "，分数: " + greedyScore);

        Move finalMove;
        if (greedyMove == null && alphaBetaMove == null) {
            System.out.println("[混合策略] 所有算法均未找到走法，使用兜底策略。");
            finalMove = enhancedFallbackMove(b);
        } else if (greedyMove == null) {
            System.out.println("[混合策略] 贪心无结果，采用剪枝搜索结果。");
            finalMove = alphaBetaMove;
        } else if (alphaBetaMove == null) {
            System.out.println("[混合策略] 剪枝无结果，采用贪心结果。");
            finalMove = greedyMove;
        } else {
            // 核心决策逻辑：默认使用贪心，仅在剪枝搜索结果明显更优时覆盖
            finalMove = greedyMove;
            System.out.println("[混合策略] 默认选择【贪心算法】结果。");

            final int SCORE_DIFF_THRESHOLD = 5000; // 定义一个显著的分数差异阈值

            // 条件1：剪枝搜索找到了必胜局
            if (alphaBetaScore >= WIN_SCORE) {
                System.out.println("[混合策略] >>覆盖决策: 剪枝搜索发现必胜走法！");
                finalMove = alphaBetaMove;
            }
            // 条件2：剪枝搜索分数显著高于贪心分数
            else if (alphaBetaScore > greedyScore + SCORE_DIFF_THRESHOLD) {
                System.out.println("[混合策略] >>覆盖决策: 剪枝搜索分数显著更高 (差异 > " + SCORE_DIFF_THRESHOLD + ")。");
                finalMove = alphaBetaMove;
            }
            // 条件3: 贪心结果是一个负分局面，而剪枝搜索找到了一个正分局面
            else if (greedyScore < 0 && alphaBetaScore > 0) {
                System.out.println("[混合策略] >>覆盖决策: 贪心结果为负分，剪枝搜索找到正分局面。");
                finalMove = alphaBetaMove;
            }
        }

        System.out.println("=== 最终决策: " + moveToString(finalMove) + " ===");
        return finalMove;
    }

    private long estimateSearchTime(int depth) {
        if (depth <= 2) return 100;
        if (depth == 3) return 300;
        if (depth == 4) return 1000;
        if (depth == 5) return 3500;
        return 9000;
    }

    /**
     * 【全新】基于精确威胁模式分析，查找必胜或必防的走法。
     * @param b     棋盘
     * @param who   要为哪一方查找
     * @return      如果找到，返回必须执行的Move；否则返回null。
     */
    private Move findImmediateThreatMove(Board b, PieceColor who) {
        List<Threat> threats = collectThreats(b, who);
        if (threats.isEmpty()) {
            return null;
        }

        // 优先级1: "活四" 是必须立即处理的。
        for (Threat threat : threats) {
            // 使用分数来判断棋型，SCORE_SELF[PATTERN_LIVE_FOUR] 是 20000
            if (threat.score >= SCORE_SELF[PATTERN_LIVE_FOUR]) {
                List<Cell> blocks = new ArrayList<>(threat.blockPoints);
                // 活四有两个关键点需要封堵
                if (blocks.size() >= 2) {
                    System.out.println("检测到 " + who + " 的活四威胁，关键点: " + blocks.get(0) + ", " + blocks.get(1));
                    return new Move(blocks.get(0).r, blocks.get(0).c, blocks.get(1).r, blocks.get(1).c);
                }
            }
        }

        // 优先级2: "双活三" 或更多活三也是必须处理的。
        List<Threat> liveThrees = new ArrayList<>();
        for (Threat threat : threats) {
            // SCORE_SELF[PATTERN_LIVE_THREE] 是 600
            if (threat.score >= SCORE_SELF[PATTERN_LIVE_THREE]) {
                liveThrees.add(threat);
            }
        }

        if (liveThrees.size() >= 2) {
            // 选取最紧急的两个活三的关键点进行组合
            Cell block1 = new ArrayList<>(liveThrees.get(0).blockPoints).get(0);
            Cell block2 = new ArrayList<>(liveThrees.get(1).blockPoints).get(0);
            if (!block1.equals(block2)) {
                System.out.println("检测到 " + who + " 的双活三威胁，关键点: " + block1 + ", " + block2);
                return new Move(block1.r, block1.c, block2.r, block2.c);
            }
        }

        return null;
    }

    private Move parallelGreedySearch(Board b) {
        List<ScoredMove> candidates = generateScoredMovesForBoard(b, color);
        if (candidates.isEmpty()) return null;
        int candidateLimit = Math.min(80, candidates.size());
        candidates = new ArrayList<>(candidates.subList(0, candidateLimit));
        CompletionService<ScoredMove> cs = new ExecutorCompletionService<>(executor);
        List<Future<ScoredMove>> futures = new ArrayList<>();
        for (ScoredMove sm : candidates) {
            final Move mv = sm.move;
            futures.add(cs.submit(() -> {
                Board temp = b.clone();
                applyMoveToBoard(temp, mv, color);
                int score = enhancedEvaluate(temp, color);
                return new ScoredMove(mv, score);
            }));
        }
        ScoredMove best = null;
        int completed = 0;
        try {
            while (completed < futures.size()) {
                Future<ScoredMove> f = cs.poll(50, TimeUnit.MILLISECONDS);
                if (f == null) break;
                ScoredMove sm = f.get();
                completed++;
                if (best == null || sm.score > best.score) {
                    best = sm;
                }
            }
        } catch (Exception e) { /* Ignore */ }
        for (Future<ScoredMove> f : futures) {
            f.cancel(true);
        }
        return best != null ? best.move : null;
    }

    private int enhancedEvaluate(Board b, PieceColor player) {
        int standardScore = evaluate(b);
        int threatBonus = 0;
        if (countThreats(b, player) > 0) threatBonus += 5000;
        if (countThreats(b, player.opposite()) > 0) threatBonus -= 10000;
        int pathControlScore = evaluatePathControl(b, player);
        return standardScore + threatBonus + pathControlScore;
    }

    private String moveToString(Move move) {
        if (move == null) return "null";
        if (move.isFirstMove()) return "(" + move.getRow1() + "," + move.getCol1() + ")";
        return "(" + move.getRow1() + "," + move.getCol1() + ") & (" + move.getRow2() + "," + move.getCol2() + ")";
    }

    private Move parallelRootSearch(Board root, int depth, List<Move> injectedCandidates) {
        List<ScoredMove> candidates = generateScoredMovesForBoard(root, color);
        if (injectedCandidates != null && !injectedCandidates.isEmpty()) {
            List<ScoredMove> injectedScoredMoves = new ArrayList<>();
            Set<String> existingMoveKeys = new HashSet<>();
            for (ScoredMove sm : candidates) {
                existingMoveKeys.add(getMovePairKey(sm.move));
            }
            for (Move m : injectedCandidates) {
                String moveKey = getMovePairKey(m);
                if (!existingMoveKeys.contains(moveKey)) {
                    injectedScoredMoves.add(new ScoredMove(m, INF - 1));
                    existingMoveKeys.add(moveKey);
                }
            }
            if (!injectedScoredMoves.isEmpty()) {
                candidates.addAll(0, injectedScoredMoves);
            }
        }
        if (candidates.isEmpty()) return enhancedFallbackMove(root);
        int rootLimit = Math.max(8, Math.min(24 - depth * 2, candidates.size()));
        if (candidates.size() > rootLimit) {
            candidates = candidates.subList(0, rootLimit);
        }
        System.out.println("[剪枝搜索] 根节点分支因子: " + candidates.size());
        CompletionService<RootResult> cs = new ExecutorCompletionService<>(executor);
        List<Future<RootResult>> futures = new ArrayList<>(candidates.size());
        for (ScoredMove sm : candidates) {
            final Move mv = sm.move;
            futures.add(cs.submit(() -> {
                Board temp = root.clone();
                applyMoveToBoard(temp, mv, color);
                int score = -pvs(temp, depth - 1, -INF, INF, color.opposite(), 0);
                return new RootResult(mv, score);
            }));
        }
        Move bestMove = null;
        int bestScore = -INF;
        try {
            for (int i = 0; i < futures.size(); i++) {
                Future<RootResult> f = cs.take();
                if (f != null) {
                    RootResult rr = f.get();
                    if (rr.score > bestScore) {
                        bestScore = rr.score;
                        bestMove = rr.move;
                        if (bestScore >= WIN_SCORE) break;
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("[parallelRootSearch] 搜索任务被主线程中断(超时)，正常退出。");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("[parallelRootSearch] 搜索子任务执行异常: " + e.getCause());
            e.getCause().printStackTrace();
        } finally {
            for (Future<RootResult> fu : futures) {
                fu.cancel(true);
            }
        }
        return bestMove;
    }

    private String getMovePairKey(Move m) {
        if (m.isFirstMove()) return getMovePairKey(new Move(m.getRow1(), m.getCol1()), new Move(-1, -1));
        return getMovePairKey(new Move(m.getRow1(), m.getCol1()), new Move(m.getRow2(), m.getCol2()));
    }

    private int pvs(Board b, int depth, int alpha, int beta, PieceColor player, int threatDepth) {
        nodesExplored++;
        long hash = calculateBoardHash(b);
        CacheEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.depth >= depth) {
            cacheHits++;
            if (entry.flag == 0) return entry.score;
            if (entry.flag == 1 && entry.score >= beta) return entry.score;
            if (entry.flag == 2 && entry.score <= alpha) return entry.score;
        } else {
            cacheMisses++;
        }
        PieceColor winner = checkWinner(b);
        if (winner != null) {
            int val = (winner == color) ? WIN_SCORE : -WIN_SCORE;
            transpositionTable.put(hash, new CacheEntry(val, depth, 0));
            return val;
        }
        if (depth > 0 && threatDepth < 2) {
            boolean hasThreat = (player == color) ? hasUrgentThreatAtDepth(b, color.opposite(), depth) : hasUrgentThreatAtDepth(b, color, depth);
            if (hasThreat) {
                depth += 1;
                threatDepth += 1;
            }
        }
        if (depth == 0) {
            int val = evaluate(b);
            transpositionTable.put(hash, new CacheEntry(val, 0, 0));
            return val;
        }
        List<ScoredMove> moves = generateScoredMovesForBoard(b, player);
        List<ScoredMove> injected = generateDefensiveCandidates(b, player);
        if (!injected.isEmpty()) {
            injected.addAll(moves);
            moves = injected;
        }
        if (moves.isEmpty()) return 0;
        int branchLimit = Math.max(6, Math.min(22 - depth * 3, moves.size()));
        if (moves.size() > branchLimit) {
            moves = moves.subList(0, branchLimit);
        }
        boolean first = true;
        int best = -INF;
        int flag = 2;
        for (ScoredMove sm : moves) {
            Board nb = b.clone();
            applyMoveToBoard(nb, sm.move, player);
            int score;
            if (first) {
                first = false;
                score = -pvs(nb, depth - 1, -beta, -alpha, player.opposite(), threatDepth);
            } else {
                score = -pvs(nb, depth - 1, -alpha - 1, -alpha, player.opposite(), threatDepth);
                if (score > alpha && score < beta) {
                    score = -pvs(nb, depth - 1, -beta, -alpha, player.opposite(), threatDepth);
                }
            }
            if (score > best) best = score;
            if (score > alpha) {
                alpha = score;
                flag = 0;
            }
            if (alpha >= beta) {
                flag = 1;
                transpositionTable.put(hash, new CacheEntry(beta, depth, flag));
                return beta;
            }
        }
        transpositionTable.put(hash, new CacheEntry(best, depth, flag));
        return best;
    }

    private boolean hasUrgentThreatAtDepth(Board b, PieceColor who, int depth) {
        int depthAdjustedThreshold = Math.max(THREAT_IMPORTANT, THREAT_URGENT - depth * 5000);
        return hasThreat(b, who, depthAdjustedThreshold);
    }

    // ===================== 评估函数（与前一版相同） =====================
    private int evaluate(Board b) {
        int myScore = evaluatePlayerWithPatterns(b, color, true);
        int oppScore = evaluatePlayerWithPatterns(b, color.opposite(), false);
        int pathControlScore = evaluatePathControl(b, color);
        return myScore - oppScore + pathControlScore;
    }

    private int evaluatePathControl(Board b, PieceColor player) {
        String boardKey = getBoardKey(b);
        if (pathValueCache.containsKey(boardKey)) return pathValueCache.get(boardKey);
        int score = 0;
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] dir : dirs) {
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    score += evaluateStrategicPath(b, r, c, dir[0], dir[1], player);
                }
            }
        }
        score += evaluateCenterControl(b, player);
        pathValueCache.put(boardKey, score);
        return score;
    }

    private String getBoardKey(Board b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor pc = b.get(i, j);
                sb.append(pc == color ? '1' : (pc == color.opposite() ? '2' : '0'));
            }
        }
        return sb.toString();
    }

    private int evaluateStrategicPath(Board b, int r, int c, int dr, int dc, PieceColor player) {
        int myCount = 0, oppCount = 0, emptyCount = 0;
        for (int i = 0; i < 6; i++) {
            int rr = r + i * dr, cc = c + i * dc;
            if (!b.isValid(rr, cc)) return 0;
            PieceColor pc = b.get(rr, cc);
            if (pc == player) myCount++;
            else if (pc == player.opposite()) oppCount++;
            else emptyCount++;
        }
        if (myCount > 0 && oppCount > 0) return 0;
        if (myCount > 0) return myCount * 5 + emptyCount * 2;
        if (oppCount > 0) return -(oppCount * 6 + emptyCount * 3);
        return 1;
    }

    private int evaluateCenterControl(Board b, PieceColor player) {
        int score = 0;
        int centerStart = 5, centerEnd = 13;
        for (int r = centerStart; r <= centerEnd; r++) {
            for (int c = centerStart; c <= centerEnd; c++) {
                PieceColor pc = b.get(r, c);
                if (pc == player) score += 15;
                else if (pc == player.opposite()) score -= 15;
            }
        }
        return score;
    }

    private int evaluatePlayerWithPatterns(Board b, PieceColor player, boolean isUs) {
        int totalScore = 0, positionScore = 0;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (b.get(i, j) == player) {
                    positionScore += POSITION_SCORE[i][j];
                }
            }
        }
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                for (int[] dir : dirs) {
                    totalScore += evaluateLinePattern(b, r, c, dir[0], dir[1], player, isUs);
                }
            }
        }
        return totalScore + positionScore;
    }

    private int evaluateLinePattern(Board b, int r, int c, int dr, int dc, PieceColor player, boolean isUs) {
        List<Integer> line = new ArrayList<>();
        for (int k = -2; k <= 6; k++) {
            int rr = r + k * dr, cc = c + k * dc;
            if (!b.isValid(rr, cc)) line.add(-1);
            else {
                PieceColor pc = b.get(rr, cc);
                if (pc == player) line.add(1);
                else if (pc == player.opposite()) line.add(2);
                else line.add(0);
            }
        }
        int patternType = analyzePattern(line);
        if (patternType == -1) return 0;
        return isUs ? SCORE_SELF[patternType] : SCORE_OPPONENT[patternType];
    }

    private int analyzePattern(List<Integer> line) {
        int myCount = 0, emptyCount = 0, oppCount = 0, leftEmpty = 0, rightEmpty = 0;
        if (line.size() < 8) return -1;
        for (int i = 2; i < 8; i++) {
            int val = line.get(i);
            if (val == 1) myCount++;
            else if (val == 0) emptyCount++;
            else if (val == 2) oppCount++;
            else return -1;
        }
        if (oppCount > 0) return -1;
        if (line.get(1) == 0) leftEmpty = 1;
        if (line.get(8) == 0) rightEmpty = 1;
        if (myCount >= 6) return PATTERN_SIX;
        if (myCount == 5 && emptyCount == 1) return (leftEmpty == 1 && rightEmpty == 1) ? PATTERN_LIVE_FIVE : PATTERN_SLEEP_FIVE;
        if (myCount == 4 && emptyCount == 2) {
            if (leftEmpty == 1 && rightEmpty == 1) return PATTERN_LIVE_FOUR;
            if (leftEmpty == 1 || rightEmpty == 1) return PATTERN_SLEEP_FOUR;
            return PATTERN_DEAD_FOUR;
        }
        if (myCount == 3 && emptyCount == 3) {
            if (leftEmpty == 1 && rightEmpty == 1) return PATTERN_LIVE_THREE;
            if (leftEmpty == 1 || rightEmpty == 1) return PATTERN_HAZY_THREE;
            return PATTERN_SLEEP_THREE;
        }
        if (myCount == 2 && emptyCount == 4) return (leftEmpty == 1 && rightEmpty == 1) ? PATTERN_LIVE_TWO : PATTERN_SLEEP_TWO;
        return -1;
    }

    // ===================== 候选生成（与前一版相同） =====================
    private List<ScoredMove> generateScoredMovesForBoard(Board b, PieceColor player) {
        List<ScoredMove> scored = new ArrayList<>();
        List<int[]> base = getEmptyPositionsNearPieces(b, 2);
        if (base.size() < 12) {
            Set<String> set = new HashSet<>();
            for (int[] p : base) set.add(p[0] + "," + p[1]);
            for (int[] p : getEmptyPositionsNearPieces(b, 3)) {
                String key = p[0] + "," + p[1];
                if (!set.contains(key)) {
                    base.add(p);
                    set.add(key);
                }
            }
        }
        Set<Cell> myThreats = collectThreatCells(b, player);
        Set<Cell> oppThreats = collectThreatCells(b, player.opposite());
        Map<String, int[]> unique = new LinkedHashMap<>();
        for (int[] p : base) unique.put(p[0] + "," + p[1], p);
        for (Cell c : myThreats) unique.put(c.r + "," + c.c, new int[]{c.r, c.c});
        for (Cell c : oppThreats) unique.put(c.r + "," + c.c, new int[]{c.r, c.c});
        List<int[]> finalCands = new ArrayList<>(unique.values());
        if (finalCands.size() < 2) return Collections.emptyList();
        Set<Cell> urgentOppCells = collectThreatCells(b, player.opposite());
        int cap = Math.min(30, finalCands.size());
        for (int i = 0; i < cap; i++) {
            for (int j = i + 1; j < cap; j++) {
                int[] a = finalCands.get(i), c = finalCands.get(j);
                Move m = new Move(a[0], a[1], c[0], c[1]);
                int score = quickEvaluateMove(b, m, player);
                if (coversAny(m, urgentOppCells)) {
                    score += THREAT_URGENT / 2;
                }
                scored.add(new ScoredMove(m, score));
            }
        }
        scored.sort((x, y) -> Integer.compare(y.score, x.score));
        return scored.subList(0, Math.min(24, scored.size()));
    }

    private List<ScoredMove> generateDefensiveCandidates(Board b, PieceColor player) {
        List<ScoredMove> out = new ArrayList<>();
        Set<Cell> urgent = collectThreatCells(b, player.opposite());
        if (urgent.isEmpty()) return out;
        List<int[]> near = getEmptyPositionsNearPieces(b, 2);
        List<Cell> list = new ArrayList<>(urgent);
        if (list.size() >= 2) {
            Cell u1 = list.get(0), u2 = list.get(1);
            out.add(new ScoredMove(new Move(u1.r, u1.c, u2.r, u2.c), THREAT_URGENT + 5_000));
        }
        int inject = 0;
        for (Cell u : list) {
            for (int[] p : near) {
                if (inject >= 4) break;
                if (p[0] == u.r && p[1] == u.c) continue;
                out.add(new ScoredMove(new Move(u.r, u.c, p[0], p[1]), THREAT_URGENT + 3_000));
                inject++;
            }
            if (inject >= 4) break;
        }
        out.sort((a, b1) -> Integer.compare(b1.score, a.score));
        return out;
    }

    private Move enhancedFallbackMove(Board b) {
        Set<Cell> oppThreats = collectThreatCells(b, color.opposite());
        Set<Cell> myOpportunities = collectThreatCells(b, color);
        List<int[]> nearPositions = getEmptyPositionsNearPieces(b, 2);
        if (nearPositions.isEmpty()) return new Move(Board.SIZE / 2, Board.SIZE / 2);
        if (!oppThreats.isEmpty()) {
            Cell threat = new ArrayList<>(oppThreats).get(0);
            if (!myOpportunities.isEmpty()) {
                Cell opportunity = new ArrayList<>(myOpportunities).get(0);
                if (threat.r != opportunity.r || threat.c != opportunity.c) {
                    return new Move(threat.r, threat.c, opportunity.r, opportunity.c);
                }
            }
            for (int[] p : nearPositions) {
                if (p[0] != threat.r || p[1] != threat.c) {
                    return new Move(threat.r, threat.c, p[0], p[1]);
                }
            }
            return new Move(threat.r, threat.c);
        }
        if (!myOpportunities.isEmpty()) {
            List<Cell> oppList = new ArrayList<>(myOpportunities);
            if (oppList.size() >= 2) {
                return new Move(oppList.get(0).r, oppList.get(0).c, oppList.get(1).r, oppList.get(1).c);
            } else {
                Cell opp = oppList.get(0);
                for (int[] p : nearPositions) {
                    if (p[0] != opp.r || p[1] != opp.c) {
                        return new Move(opp.r, opp.c, p[0], p[1]);
                    }
                }
            }
        }
        if (nearPositions.size() >= 2) {
            nearPositions.sort((a, c) -> {
                int distA = Math.abs(a[0] - Board.SIZE / 2) + Math.abs(a[1] - Board.SIZE / 2);
                int distB = Math.abs(c[0] - Board.SIZE / 2) + Math.abs(c[1] - Board.SIZE / 2);
                return Integer.compare(distA, distB);
            });
            return new Move(nearPositions.get(0)[0], nearPositions.get(0)[1], nearPositions.get(1)[0], nearPositions.get(1)[1]);
        }
        return new Move(Board.SIZE / 2, Board.SIZE / 2);
    }

    // ===================== 关键走法检测（已更新为智能防守） =====================

    private List<Move> findOtherCriticalMoves(Board b) {
        List<Move> candidates = new ArrayList<>();
        // 策略1：【智能版】紧急阻断对手的威胁
        Move urgentBlock = findUrgentBlockingMove(b, color);
        if (urgentBlock != null) {
            System.out.println("检测到建议走法（紧急阻断）: " + moveToString(urgentBlock));
            candidates.add(urgentBlock);
        }
        // 策略2：创建我方的双重威胁
        Move dbl = findDoubleThreatMove(b, color);
        if (dbl != null) {
            System.out.println("检测到建议走法（创建双威胁）: " + moveToString(dbl));
            candidates.add(dbl);
        }
        // 策略3：阻断对手创建双重威胁
        Move blockDbl = blockOpponentDoubleThreat(b);
        if (blockDbl != null) {
            System.out.println("检测到建议走法（阻断对手双威胁）: " + moveToString(blockDbl));
            candidates.add(blockDbl);
        }
        return candidates;
    }

    private Move findWinningMove(Board b, PieceColor who, int limitCap) {
        List<int[]> empties = getEmptyPositionsNearPieces(b, 3);
        int limit = Math.min(empties.size(), Math.max(20, limitCap));
        for (int i = 0; i < limit; i++) {
            for (int j = i + 1; j < limit; j++) {
                int[] p1 = empties.get(i), p2 = empties.get(j);
                Board tb = b.clone();
                tb.makeMove(p1[0], p1[1], who);
                if (tb.checkWin(p1[0], p1[1], who)) return new Move(p1[0], p1[1], p2[0], p2[1]);
                tb.makeMove(p2[0], p2[1], who);
                if (tb.checkWin(p2[0], p2[1], who)) return new Move(p1[0], p1[1], p2[0], p2[1]);
            }
        }
        return null;
    }

    private Move findDoubleThreatMove(Board b, PieceColor who) {
        List<ScoredMove> moves = generateScoredMovesForBoard(b, who);
        for (ScoredMove sm : moves) {
            if (sm.score >= THREAT_URGENT) {
                Board tb = b.clone();
                applyMoveToBoard(tb, sm.move, who);
                if (countThreats(tb, who) >= 2) return sm.move;
            }
        }
        return null;
    }

    /**
     * 【修正版】寻找紧急阻断走法
     * - 利用新的`collectThreats`方法，智能区分不同威胁并做出最优防守。
     */
    private Move findUrgentBlockingMove(Board b, PieceColor defender) {
        List<Threat> threats = collectThreats(b, defender.opposite());
        if (threats.isEmpty()) return null;

        Threat primaryThreat = threats.get(0);
        List<Cell> primaryBlocks = new ArrayList<>(primaryThreat.blockPoints);

        // 情况1: 最主要的威胁需要堵住两个或更多点（例如活四 _OOOO_）。
        if (primaryBlocks.size() >= 2) {
            Cell block1 = primaryBlocks.get(0);
            Cell block2 = primaryBlocks.get(primaryBlocks.size() - 1); // 取第一个和最后一个，通常是两端
            System.out.println("AI-防守修正: 检测到主要威胁（如活四），在两端 " + block1 + " 和 " + block2 + " 进行封堵。");
            return new Move(block1.r, block1.c, block2.r, block2.c);
        }

        // 情况2: 最主要的威胁只需要堵住一个点。
        if (primaryBlocks.size() == 1) {
            Cell block1 = primaryBlocks.get(0);
            // 检查是否存在次要威胁，可以用第二颗棋子来防守。
            if (threats.size() > 1) {
                Threat secondaryThreat = threats.get(1);
                if (!secondaryThreat.blockPoints.isEmpty()) {
                    Cell block2 = new ArrayList<>(secondaryThreat.blockPoints).get(0);
                    if (!block1.equals(block2)) {
                        System.out.println("AI-防守修正: 同时封堵两个独立威胁于 " + block1 + " 和 " + block2);
                        return new Move(block1.r, block1.c, block2.r, block2.c);
                    }
                }
            }
            // 如果没有次要威胁，则将第一颗子用于防守，第二颗子落在附近的好位置。
            System.out.println("AI-防守修正: 封堵主要威胁于 " + block1 + ", 并寻找最佳辅助落点。");
            List<int[]> near = getEmptyPositionsNearPieces(b, 2);
            for (int[] p : near) {
                if (p[0] != block1.r || p[1] != block1.c) {
                    return new Move(block1.r, block1.c, p[0], p[1]);
                }
            }
            return new Move(block1.r, block1.c); // 兜底：只防守一个点
        }

        return null; // 没有可防守的点
    }


    private Move blockOpponentDoubleThreat(Board b) {
        Set<Cell> uc = collectThreatCells(b, color.opposite());
        if (uc.size() >= 2) {
            Iterator<Cell> it = uc.iterator();
            Cell a = it.next(), c = it.next();
            return new Move(a.r, a.c, c.r, c.c);
        }
        return null;
    }

    // ===================== 辅助函数与类 =====================

    private int quickEvaluateMove(Board b, Move m, PieceColor who) {
        Board tb = b.clone();
        applyMoveToBoard(tb, m, who);
        return evaluate(tb);
    }

    private int countThreats(Board b, PieceColor who) {
        return collectThreats(b, who).size();
    }

    private boolean hasThreat(Board b, PieceColor who, int threshold) {
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                for (int[] dir : dirs) {
                    int score = evaluateLinePattern(b, r, c, dir[0], dir[1], who, who == color);
                    if (score >= threshold) return true;
                }
            }
        }
        return false;
    }

    /**
     * 【新】生成规范化的威胁线键，用于避免重复记录同一威胁。
     */
    private String generateThreatKey(int r, int c, int[] dir) {
        int dr = dir[0], dc = dir[1];
        // 为了确保线的唯一性，总是让键从坐标较小的一端开始
        int r2 = r + 5 * dr, c2 = c + 5 * dc;
        if (r < r2 || (r == r2 && c < c2)) {
            return String.format("%d,%d:%d,%d", r, c, dr, dc);
        } else {
            return String.format("%d,%d:%d,%d", r2, c2, -dr, -dc);
        }
    }

    /**
     * 【新】收集所有威胁，并按威胁程度排序。
     *
     * @return 返回一个包含具体威胁信息（分数、防守点）的列表。
     */
    private List<Threat> collectThreats(Board b, PieceColor attacker) {
        Map<String, Threat> threats = new HashMap<>();
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                for (int[] dir : dirs) {
                    int score = evaluateLinePattern(b, r, c, dir[0], dir[1], attacker, attacker == color);
                    if (score >= THREAT_IMPORTANT) {
                        Set<Cell> blockPoints = new LinkedHashSet<>();
                        for (int k = 0; k < 6; k++) {
                            int rr = r + k * dir[0], cc = c + k * dir[1];
                            if (b.isValid(rr, cc) && b.get(rr, cc) == PieceColor.EMPTY) {
                                blockPoints.add(new Cell(rr, cc));
                            }
                        }
                        if (!blockPoints.isEmpty()) {
                            String key = generateThreatKey(r, c, dir);
                            if (!threats.containsKey(key) || threats.get(key).score < score) {
                                threats.put(key, new Threat(score, blockPoints, key));
                            }
                        }
                    }
                }
            }
        }
        List<Threat> result = new ArrayList<>(threats.values());
        result.sort(Threat::compareTo); // 按分数从高到低排序
        return result;
    }

    /**
     * 【兼容性包装】收集威胁位置的老方法，现在通过调用新方法并展平结果来实现。
     */
    private Set<Cell> collectThreatCells(Board b, PieceColor attacker) {
        List<Threat> threats = collectThreats(b, attacker);
        Set<Cell> allBlockPoints = new LinkedHashSet<>();
        for (Threat threat : threats) {
            allBlockPoints.addAll(threat.blockPoints);
        }
        return allBlockPoints;
    }

    private List<int[]> getEmptyPositionsNearPieces(Board b, int dist) {
        List<int[]> res = new ArrayList<>();
        boolean[][] seen = new boolean[Board.SIZE][Board.SIZE];
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (b.get(i, j) == PieceColor.EMPTY && hasNeighbor(b, i, j, dist)) {
                    if (!seen[i][j]) {
                        res.add(new int[]{i, j});
                        seen[i][j] = true;
                    }
                }
            }
        }
        return res;
    }

    private boolean hasNeighbor(Board b, int r, int c, int dist) {
        for (int dr = -dist; dr <= dist; dr++) {
            for (int dc = -dist; dc <= dist; dc++) {
                if (dr == 0 && dc == 0) continue;
                int rr = r + dr, cc = c + dc;
                if (b.isValid(rr, cc) && b.get(rr, cc) != PieceColor.EMPTY) return true;
            }
        }
        return false;
    }

    private void applyMove(Move move) {
        if (move != null) {
            board.makeMove(move.getRow1(), move.getCol1(), color);
            if (!move.isFirstMove()) {
                board.makeMove(move.getRow2(), move.getCol2(), color);
            }
        }
    }

    private void applyMoveToBoard(Board b, Move move, PieceColor who) {
        if (move != null) {
            b.makeMove(move.getRow1(), move.getCol1(), who);
            if (!move.isFirstMove()) {
                b.makeMove(move.getRow2(), move.getCol2(), who);
            }
        }
    }

    private PieceColor checkWinner(Board b) {
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor pc = b.get(i, j);
                if (pc != PieceColor.EMPTY && b.checkWin(i, j, pc)) return pc;
            }
        }
        return null;
    }

    private long calculateBoardHash(Board b) {
        long hash = 0, p = 31, m = 1_000_000_009, pow = 1;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                PieceColor piece = b.get(i, j);
                int v = (piece == PieceColor.BLACK) ? 1 : (piece == PieceColor.WHITE ? 2 : 0);
                hash = (hash + (v + 1L) * pow) % m;
                pow = (pow * p) % m;
            }
        }
        return hash;
    }

    private boolean coversAny(Move m, Set<Cell> cells) {
        if (cells.isEmpty()) return false;
        if (cells.contains(new Cell(m.getRow1(), m.getCol1()))) return true;
        return !m.isFirstMove() && cells.contains(new Cell(m.getRow2(), m.getCol2()));
    }

    // ===================== 辅助类定义 =====================

    private static class RootResult {
        final Move move;
        final int score;
        RootResult(Move m, int s) { move = m; score = s; }
    }

    private static class ScoredMove {
        final Move move;
        final int score;
        ScoredMove(Move m, int s) { move = m; score = s; }
    }

    private static class Cell {
        final int r, c;
        Cell(int r, int c) { this.r = r; this.c = c; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cell)) return false;
            Cell x = (Cell) o;
            return r == x.r && c == x.c;
        }
        @Override
        public int hashCode() { return Objects.hash(r, c); }
        @Override
        public String toString() { return "(" + r + "," + c + ")"; }
    }

    /**
     * 【新】用于封装单个威胁信息的辅助类。
     */
    private static class Threat implements Comparable<Threat> {
        final int score;
        final Set<Cell> blockPoints;
        final String uniqueKey;

        Threat(int score, Set<Cell> blockPoints, String key) {
            this.score = score;
            this.blockPoints = blockPoints;
            this.uniqueKey = key;
        }

        @Override
        public int compareTo(Threat other) {
            return Integer.compare(other.score, this.score); // 按分数降序排列
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Threat threat = (Threat) o;
            return Objects.equals(uniqueKey, threat.uniqueKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uniqueKey);
        }
    }
}