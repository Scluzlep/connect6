package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import core.board.Board;
import core.board.PieceColor;
import core.game.Move;
import player.AlphaBetaAI;
import util.GameLogger;
import java.time.format.DateTimeFormatter;

public class GameFrame extends JFrame {
    private static final int BOARD_SIZE = 19;
    private static final int CELL_SIZE = 30;
    private static final int MARGIN = 40;
    private static final int PANEL_WIDTH = CELL_SIZE * (BOARD_SIZE - 1) + 2 * MARGIN;
    private static final int PANEL_HEIGHT = CELL_SIZE * (BOARD_SIZE - 1) + 2 * MARGIN;

    private Board board;
    private AlphaBetaAI aiPlayer;
    private PieceColor currentPlayer;
    private PieceColor humanColor;
    private boolean gameOver;
    private int firstClickRow = -1;
    private int firstClickCol = -1;
    private int moveNumber = 0;
    private boolean isFirstMoveOfGame = true;  // 游戏的第一步（黑棋只下1子）

    private JLabel statusLabel;
    private JLabel blackTotalTimeLabel;
    private JLabel blackStepTimeLabel;
    private JLabel whiteTotalTimeLabel;
    private JLabel whiteStepTimeLabel;
    private JButton restartButton;
    private JButton viewLogButton;
    private BoardPanel boardPanel;

    private GameLogger logger;

    // 思考时间计时器和累计时间
    private Timer thinkingTimer;
    private long blackTotalTime = 0;
    private long whiteTotalTime = 0;
    private long currentStepStartTime = 0;

    public GameFrame(boolean humanFirst) {
        setTitle("六子棋 - 人机对战");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        this.humanColor = humanFirst ? PieceColor.BLACK : PieceColor.WHITE;
        initGame();
        initComponents();

        // 如果AI先手（黑棋），让AI先走第一步
        if (!humanFirst) {
            currentPlayer = PieceColor.BLACK;
            statusLabel.setText("<html><center>AI思考中...<br/>正在下第一子</center></html>");
            // 使用 SwingWorker 在后台线程执行AI思考
            new SwingWorker<Move, Void>() {
                @Override
                protected Move doInBackground() {
                    return aiPlayer.findMove(null);
                }

                @Override
                protected void done() {
                    try {
                        Move aiMove = get();
                        finishAIMove(aiMove, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    private void initGame() {
        board = new Board();
        aiPlayer = new AlphaBetaAI("AI Bot");
        PieceColor aiColor = (humanColor == PieceColor.BLACK) ? PieceColor.WHITE : PieceColor.BLACK;
        aiPlayer.setColor(aiColor);
        aiPlayer.setBoard(board);
        currentPlayer = PieceColor.BLACK;  // 黑棋总是先行
        gameOver = false;
        moveNumber = 0;
        isFirstMoveOfGame = true;
        logger = new GameLogger();
        blackTotalTime = 0;
        whiteTotalTime = 0;
        currentStepStartTime = System.currentTimeMillis();

        initThinkingTimer();
    }

    private void initThinkingTimer() {
        thinkingTimer = new Timer(100, e -> {
            if (!gameOver) {
                long currentStepTime = System.currentTimeMillis() - currentStepStartTime;
                updateThinkingTimeDisplay(currentPlayer, currentStepTime);
            }
        });
        thinkingTimer.start();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(260, PANEL_HEIGHT));
        rightPanel.setBackground(new Color(240, 230, 210));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(240, 230, 210));
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 1),
                "对局信息",
                0, 0,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        String humanColorStr = (humanColor == PieceColor.BLACK) ? "黑棋" : "白棋";
        String aiColorStr = (humanColor == PieceColor.BLACK) ? "白棋" : "黑棋";

        JLabel humanLabel = new JLabel("玩家：" + humanColorStr);
        humanLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        JLabel aiLabel = new JLabel("电脑：" + aiColorStr);
        aiLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        infoPanel.add(humanLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(aiLabel);

        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        timePanel.setBackground(new Color(240, 230, 210));
        timePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 1),
                "思考时间",
                0, 0,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        JPanel blackTimePanel = new JPanel();
        blackTimePanel.setLayout(new BoxLayout(blackTimePanel, BoxLayout.Y_AXIS));
        blackTimePanel.setBackground(new Color(240, 230, 210));
        blackTimePanel.setBorder(BorderFactory.createTitledBorder("黑方"));

        blackTotalTimeLabel = new JLabel("局时：0.0秒");
        blackTotalTimeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        blackStepTimeLabel = new JLabel("步时：0.0秒");
        blackStepTimeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        blackTimePanel.add(blackTotalTimeLabel);
        blackTimePanel.add(Box.createVerticalStrut(3));
        blackTimePanel.add(blackStepTimeLabel);

        JPanel whiteTimePanel = new JPanel();
        whiteTimePanel.setLayout(new BoxLayout(whiteTimePanel, BoxLayout.Y_AXIS));
        whiteTimePanel.setBackground(new Color(240, 230, 210));
        whiteTimePanel.setBorder(BorderFactory.createTitledBorder("白方"));

        whiteTotalTimeLabel = new JLabel("局时：0.0秒");
        whiteTotalTimeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        whiteStepTimeLabel = new JLabel("步时：0.0秒");
        whiteStepTimeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        whiteTimePanel.add(whiteTotalTimeLabel);
        whiteTimePanel.add(Box.createVerticalStrut(3));
        whiteTimePanel.add(whiteStepTimeLabel);

        timePanel.add(blackTimePanel);
        timePanel.add(Box.createVerticalStrut(5));
        timePanel.add(whiteTimePanel);

        statusLabel = new JLabel("<html><center>黑棋先行<br/>请在中央落第一子</center></html>");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 1),
                "当前状态",
                0, 0,
                new Font("微软雅黑", Font.BOLD, 12)
        ));
        statusLabel.setPreferredSize(new Dimension(240, 80));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(new Color(240, 230, 210));

        restartButton = new JButton("重新开始");
        restartButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        restartButton.setMaximumSize(new Dimension(200, 30));
        restartButton.addActionListener(e -> restartGame());

        viewLogButton = new JButton("查看对局日志");
        viewLogButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        viewLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewLogButton.setMaximumSize(new Dimension(200, 30));
        viewLogButton.addActionListener(e -> showLogDialog());

        buttonPanel.add(restartButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(viewLogButton);

        JTextArea rulesArea = new JTextArea(
                "规则说明：\n" +
                        "1. 黑棋先行，第一步只下一子\n" +
                        "2. 之后双方轮流，每次下两子\n" +
                        "3. 先形成六连者获胜\n\n" +
                        "时间说明：\n" +
                        "局时 = 本局累计思考时间\n" +
                        "步时 = 本步实时思考时间"
        );
        rulesArea.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        rulesArea.setBackground(new Color(240, 230, 210));
        rulesArea.setEditable(false);
        rulesArea.setFocusable(false);
        rulesArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 1),
                "游戏说明",
                0, 0,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        rightPanel.add(infoPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(timePanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(statusLabel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(buttonPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(rulesArea);
        rightPanel.add(Box.createVerticalGlue());

        add(rightPanel, BorderLayout.EAST);

        pack();
    }

    private void updateThinkingTimeDisplay(PieceColor color, long currentStepTime) {
        String stepTimeStr = formatTime(currentStepTime);
        long totalTime = (color == PieceColor.BLACK) ? blackTotalTime : whiteTotalTime;
        String totalTimeStr = formatTime(totalTime + currentStepTime);  // 显示实时累计

        if (color == PieceColor.BLACK) {
            blackStepTimeLabel.setText("步时：" + stepTimeStr);
            blackTotalTimeLabel.setText("局时：" + totalTimeStr);
        } else {
            whiteStepTimeLabel.setText("步时：" + stepTimeStr);
            whiteTotalTimeLabel.setText("局时：" + totalTimeStr);
        }
    }

    private void updateTotalTime(PieceColor color, long stepTime) {
        if (color == PieceColor.BLACK) {
            blackTotalTime += stepTime;
            blackTotalTimeLabel.setText("局时：" + formatTime(blackTotalTime));
            blackStepTimeLabel.setText("步时：" + formatTime(stepTime));
        } else {
            whiteTotalTime += stepTime;
            whiteTotalTimeLabel.setText("局时：" + formatTime(whiteTotalTime));
            whiteStepTimeLabel.setText("步时：" + formatTime(stepTime));
        }
    }

    private String formatTime(long milliseconds) {
        double seconds = milliseconds / 1000.0;
        if (seconds < 60) {
            return String.format("%.1f秒", seconds);
        } else {
            int minutes = (int) (seconds / 60);
            double remainSeconds = seconds % 60;
            return String.format("%d分%.1f秒", minutes, remainSeconds);
        }
    }

    class BoardPanel extends JPanel {
        public BoardPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_WIDTH));
            setBackground(new Color(220, 179, 92));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (gameOver) return;
                    if (currentPlayer != humanColor) return;
                    handleClick(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < BOARD_SIZE; i++) {
                g2d.drawLine(MARGIN, MARGIN + i * CELL_SIZE,
                        MARGIN + (BOARD_SIZE - 1) * CELL_SIZE, MARGIN + i * CELL_SIZE);
                g2d.drawLine(MARGIN + i * CELL_SIZE, MARGIN,
                        MARGIN + i * CELL_SIZE, MARGIN + (BOARD_SIZE - 1) * CELL_SIZE);
            }

            int[] stars = {3, 9, 15};
            for (int i : stars) {
                for (int j : stars) {
                    g2d.fillOval(MARGIN + j * CELL_SIZE - 4, MARGIN + i * CELL_SIZE - 4, 8, 8);
                }
            }

            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    PieceColor color = board.get(i, j);
                    if (color != PieceColor.EMPTY) {
                        drawPiece(g2d, j, i, color);
                    }
                }
            }

            if (firstClickRow != -1 && firstClickCol != -1) {
                g2d.setColor(new Color(255, 140, 0));
                g2d.setStroke(new BasicStroke(2));
                int x = MARGIN + firstClickCol * CELL_SIZE;
                int y = MARGIN + firstClickRow * CELL_SIZE;
                g2d.drawRect(x - 14, y - 14, 28, 28);
            }
        }

        private void drawPiece(Graphics2D g2d, int col, int row, PieceColor color) {
            int x = MARGIN + col * CELL_SIZE;
            int y = MARGIN + row * CELL_SIZE;
            int radius = 12;

            if (color == PieceColor.BLACK) {
                GradientPaint gradient = new GradientPaint(x - radius/2, y - radius/2,
                        new Color(80, 80, 80), x + radius/2, y + radius/2, Color.BLACK);
                g2d.setPaint(gradient);
                g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            } else {
                GradientPaint gradient = new GradientPaint(x - radius/2, y - radius/2,
                        Color.WHITE, x + radius/2, y + radius/2, new Color(230, 230, 230));
                g2d.setPaint(gradient);
                g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        }
    }

    private void handleClick(int x, int y) {
        int col = Math.round((float)(x - MARGIN) / CELL_SIZE);
        int row = Math.round((float)(y - MARGIN) / CELL_SIZE);

        if (col < 0 || col >= BOARD_SIZE || row < 0 || row >= BOARD_SIZE) {
            return;
        }

        if (board.get(row, col) != PieceColor.EMPTY) {
            statusLabel.setText("<html><center>该位置已有棋子！<br/>请选择空位</center></html>");
            return;
        }

        // 游戏第一步：黑棋只下一子
        if (isFirstMoveOfGame) {
            if (currentPlayer != PieceColor.BLACK) {
                statusLabel.setText("<html><center>错误：第一步必须是黑棋</center></html>");
                return;
            }

            if (Math.abs(col - 9) <= 2 && Math.abs(row - 9) <= 2) {
                long stepTime = System.currentTimeMillis() - currentStepStartTime;
                board.makeMove(row, col, PieceColor.BLACK);
                moveNumber++;

                logger.logMove(moveNumber, PieceColor.BLACK, row, col, -1, -1, stepTime, true);
                updateTotalTime(PieceColor.BLACK, stepTime);

                // 检查胜利（虽然第一步不可能赢）
                if (board.checkWin(row, col, PieceColor.BLACK)) {
                    endGame(PieceColor.BLACK, "黑棋六连获胜");
                    return;
                }

                boardPanel.repaint();
                isFirstMoveOfGame = false;
                currentPlayer = PieceColor.WHITE;  // 切换到白棋
                currentStepStartTime = System.currentTimeMillis();

                if (humanColor == PieceColor.BLACK) {
                    // 人类是黑棋，AI是白棋，AI下2子
                    statusLabel.setText("<html><center>AI思考中...<br/>正在下两子</center></html>");
                    startAIMove(null);
                } else {
                    // 人类是白棋，轮到人类下2子
                    statusLabel.setText("<html><center>轮到你了（白棋）<br/>请落两子</center></html>");
                }
            } else {
                statusLabel.setText("<html><center>第一步请在<br/>棋盘中央附近!</center></html>");
            }
        } else {
            // 后续每步下两个子
            if (firstClickRow == -1) {
                // 第一个子
                firstClickRow = row;
                firstClickCol = col;
                board.makeMove(row, col, currentPlayer);

                // 立即检查胜利
                if (board.checkWin(row, col, currentPlayer)) {
                    long stepTime = System.currentTimeMillis() - currentStepStartTime;
                    moveNumber++;
                    logger.logMove(moveNumber, currentPlayer, row, col, -1, -1, stepTime, false);
                    updateTotalTime(currentPlayer, stepTime);

                    String colorStr = (currentPlayer == PieceColor.BLACK) ? "黑棋" : "白棋";
                    endGame(currentPlayer, colorStr + "六连获胜");
                    return;
                }

                String colorStr = (currentPlayer == PieceColor.BLACK) ? "黑棋" : "白棋";
                statusLabel.setText("<html><center>" + colorStr + "已落第一子<br/>请落第二子</center></html>");
                boardPanel.repaint();
            } else {
                // 第二个子
                if (row == firstClickRow && col == firstClickCol) {
                    statusLabel.setText("<html><center>请选择不同<br/>的位置！</center></html>");
                    return;
                }

                long stepTime = System.currentTimeMillis() - currentStepStartTime;
                board.makeMove(row, col, currentPlayer);
                moveNumber++;

                logger.logMove(moveNumber, currentPlayer, firstClickRow, firstClickCol,
                        row, col, stepTime, false);
                updateTotalTime(currentPlayer, stepTime);

                // 检查胜利
                if (board.checkWin(firstClickRow, firstClickCol, currentPlayer) ||
                        board.checkWin(row, col, currentPlayer)) {
                    String colorStr = (currentPlayer == PieceColor.BLACK) ? "黑棋" : "白棋";
                    endGame(currentPlayer, colorStr + "六连获胜");
                    return;
                }

                Move playerMove = new Move(firstClickRow, firstClickCol, row, col);
                firstClickRow = -1;
                firstClickCol = -1;
                boardPanel.repaint();

                // 切换到对方
                currentPlayer = currentPlayer.opposite();
                currentStepStartTime = System.currentTimeMillis();

                if (currentPlayer != humanColor) {
                    statusLabel.setText("<html><center>AI思考中...<br/>请稍候</center></html>");
                    startAIMove(playerMove);
                } else {
                    String colorStr = (humanColor == PieceColor.BLACK) ? "黑棋" : "白棋";
                    statusLabel.setText("<html><center>轮到你了（" + colorStr + "）<br/>请落两子</center></html>");
                }
            }
        }
    }

    private void startAIMove(Move opponentMove) {
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                // 直接计算，不休眠
                return aiPlayer.findMove(opponentMove);
            }

            @Override
            protected void done() {
                try {
                    Move aiMove = get();
                    finishAIMove(aiMove, isFirstMoveOfGame);
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("<html><center>AI出错！<br/>游戏中断</center></html>");
                }
            }
        }.execute();
    }

    private void finishAIMove(Move aiMove, boolean isFirst) {
        if (aiMove == null) {
            statusLabel.setText("<html><center>AI无法移动<br/>游戏结束</center></html>");
            return;
        }

        long stepTime = System.currentTimeMillis() - currentStepStartTime;
        PieceColor aiColor = aiPlayer.getColor();

        // 下第一个子
        board.makeMove(aiMove.getRow1(), aiMove.getCol1(), aiColor);

        // 立即检查胜利
        if (board.checkWin(aiMove.getRow1(), aiMove.getCol1(), aiColor)) {
            moveNumber++;
            if (aiMove.isFirstMove()) {
                logger.logMove(moveNumber, aiColor, aiMove.getRow1(), aiMove.getCol1(), -1, -1, stepTime, true);
            } else {
                logger.logMove(moveNumber, aiColor, aiMove.getRow1(), aiMove.getCol1(), -1, -1, stepTime, false);
            }
            updateTotalTime(aiColor, stepTime);
            boardPanel.repaint();

            String colorStr = (aiColor == PieceColor.BLACK) ? "黑棋" : "白棋";
            endGame(aiColor, "AI(" + colorStr + ")六连获胜");
            return;
        }

        // 如果是第一步（黑棋），只下一子
        if (aiMove.isFirstMove()) {
            moveNumber++;
            logger.logMove(moveNumber, aiColor, aiMove.getRow1(), aiMove.getCol1(), -1, -1, stepTime, true);
            updateTotalTime(aiColor, stepTime);
            boardPanel.repaint();

            isFirstMoveOfGame = false;
            currentPlayer = PieceColor.WHITE;
            currentStepStartTime = System.currentTimeMillis();

            String colorStr = (humanColor == PieceColor.BLACK) ? "黑棋" : "白棋";
            statusLabel.setText("<html><center>轮到你了（" + colorStr + "）<br/>请落两子</center></html>");
        } else {
            // 下第二个子
            board.makeMove(aiMove.getRow2(), aiMove.getCol2(), aiColor);
            moveNumber++;

            logger.logMove(moveNumber, aiColor, aiMove.getRow1(), aiMove.getCol1(),
                    aiMove.getRow2(), aiMove.getCol2(), stepTime, false);
            updateTotalTime(aiColor, stepTime);

            // 检查胜利
            if (board.checkWin(aiMove.getRow1(), aiMove.getCol1(), aiColor) ||
                    board.checkWin(aiMove.getRow2(), aiMove.getCol2(), aiColor)) {
                boardPanel.repaint();
                String colorStr = (aiColor == PieceColor.BLACK) ? "黑棋" : "白棋";
                endGame(aiColor, "AI(" + colorStr + ")六连获胜");
                return;
            }

            boardPanel.repaint();
            currentPlayer = aiColor.opposite();
            currentStepStartTime = System.currentTimeMillis();

            String colorStr = (humanColor == PieceColor.BLACK) ? "黑棋" : "白棋";
            statusLabel.setText("<html><center>轮到你了（" + colorStr + "）<br/>请落两子</center></html>");
        }
    }

    private void endGame(PieceColor winner, String reason) {
        gameOver = true;
        thinkingTimer.stop();
        logger.logGameEnd(winner, reason);

        statusLabel.setText("<html><center>游戏结束！<br/>" + reason + "</center></html>");

        String blackTimeStr = formatTime(blackTotalTime);
        String whiteTimeStr = formatTime(whiteTotalTime);
        String timeInfo = String.format("\n\n黑方总用时：%s\n白方总用时：%s", blackTimeStr, whiteTimeStr);
        String message = reason + timeInfo + "\n\n日志已保存到：" + logger.getFileName();

        int choice = JOptionPane.showConfirmDialog(this,
                message + "\n\n是否再来一局？",
                "游戏结束",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        }
    }

    private void restartGame() {
        thinkingTimer.stop();

        int choice = JOptionPane.showConfirmDialog(this,
                "选择先后手：\n是：你先手（黑棋）\n否：AI先手（黑棋）",
                "重新开始",
                JOptionPane.YES_NO_OPTION);

        dispose();
        SwingUtilities.invokeLater(() -> {
            GameFrame newFrame = new GameFrame(choice == JOptionPane.YES_OPTION);
            newFrame.setVisible(true);
        });
    }

    private void showLogDialog() {
        JDialog logDialog = new JDialog(this, "对局日志 - " + logger.getFileName(), true);
        logDialog.setSize(800, 550);
        logDialog.setLocationRelativeTo(this);

        String[] columnNames = {"步数", "棋手", "第一子", "第二子", "时间", "步时", "局时"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        long blackTotal = 0, whiteTotal = 0;

        for (GameLogger.LogEntry entry : logger.getAllEntries()) {
            String player = (entry.player == PieceColor.BLACK) ? "黑棋" : "白棋";
            String pos1 = String.format("(%d,%d)", entry.col1, entry.row1);
            String pos2 = entry.isFirstMove ? "-" : String.format("(%d,%d)", entry.col2, entry.row2);
            String time = entry.timestamp.format(formatter);
            String stepTime = formatTime(entry.thinkingTimeMs);

            if (entry.player == PieceColor.BLACK) {
                blackTotal += entry.thinkingTimeMs;
            } else {
                whiteTotal += entry.thinkingTimeMs;
            }
            String totalTime = formatTime(entry.player == PieceColor.BLACK ? blackTotal : whiteTotal);

            model.addRow(new Object[]{entry.moveNumber, player, pos1, pos2, time, stepTime, totalTime});
        }

        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 11));
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel infoLabel = new JLabel("坐标系统：以棋盘中央为(0,0)  |  步时=本步思考时间  |  局时=累计思考时间");
        infoLabel.setFont(new Font("微软雅黑", Font.ITALIC, 11));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.SOUTH);

        JButton closeButton = new JButton("关闭");
        closeButton.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        closeButton.addActionListener(e -> logDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        logDialog.add(panel);
        logDialog.setVisible(true);
    }
}