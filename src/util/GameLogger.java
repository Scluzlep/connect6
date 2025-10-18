package util;

import core.board.PieceColor;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GameLogger {
    private String fileName;
    private List<LogEntry> logEntries;
    private LocalDateTime gameStartTime;
    private BufferedWriter writer;
    private long blackTotalTime = 0;
    private long whiteTotalTime = 0;

    public static class LogEntry {
        public int moveNumber;
        public PieceColor player;
        public int row1, col1;  // 以(0,0)为中心的坐标
        public int row2, col2;  // 第二个子的坐标，如果只下一子则为-100
        public LocalDateTime timestamp;
        public long thinkingTimeMs;
        public long totalTimeMs;  // 累计时间
        public boolean isFirstMove;

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            String playerStr = (player == PieceColor.BLACK) ? "黑棋" : "白棋";
            String timeStr = timestamp.format(formatter);
            String stepTimeStr = formatTime(thinkingTimeMs);
            String totalTimeStr = formatTime(totalTimeMs);

            if (isFirstMove) {
                return String.format("第%2d手 [%s] 位置:(%+3d,%+3d)          | 落子时间:%s | 步时:%8s | 局时:%8s",
                        moveNumber, playerStr, col1, row1, timeStr, stepTimeStr, totalTimeStr);
            } else {
                return String.format("第%2d手 [%s] 位置:(%+3d,%+3d),(%+3d,%+3d) | 落子时间:%s | 步时:%8s | 局时:%8s",
                        moveNumber, playerStr, col1, row1, col2, row2, timeStr, stepTimeStr, totalTimeStr);
            }
        }

        private String formatTime(long ms) {
            double seconds = ms / 1000.0;
            if (seconds < 60) {
                return String.format("%.3fs", seconds);
            } else {
                int minutes = (int) (seconds / 60);
                double remainSeconds = seconds % 60;
                return String.format("%dm%.1fs", minutes, remainSeconds);
            }
        }
    }

    public GameLogger() {
        logEntries = new ArrayList<>();
        gameStartTime = LocalDateTime.now();

        // 以当前时间为文件名
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        fileName = "Connect6_" + gameStartTime.format(formatter) + ".log";

        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            writeHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeader() throws IOException {
        writer.write("╔══════════════════════════════════════════════════════════╗\n");
        writer.write("║                 六子棋 (Connect6) 对局记录                  ║\n");
        writer.write("╠══════════════════════════════════════════════════════════╣\n");
        writer.write(String.format("║  对局开始时间: %-100s ║\n",
                gameStartTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss (E)", java.util.Locale.CHINA))));
        writer.write(String.format("║  日志文件名称: %-100s ║\n", fileName));
        writer.write("║  坐标系统说明: 以棋盘中央为原点(0,0)，横向为x轴(列)，纵向为y轴(行) ║\n");
        writer.write("╚══════════════════════════════════════════════════════════╝\n\n");
        writer.flush();
    }

    // 转换棋盘坐标(0-18)到中心坐标系(-9到9)
    private int boardToCenter(int coord) {
        return coord - 9;
    }

    public void logMove(int moveNumber, PieceColor player, int row1, int col1,
                        int row2, int col2, long thinkingTimeMs, boolean isFirstMove) {
        // 更新累计时间
        if (player == PieceColor.BLACK) {
            blackTotalTime += thinkingTimeMs;
        } else {
            whiteTotalTime += thinkingTimeMs;
        }

        LogEntry entry = new LogEntry();
        entry.moveNumber = moveNumber;
        entry.player = player;
        entry.row1 = boardToCenter(row1);
        entry.col1 = boardToCenter(col1);
        entry.row2 = (row2 == -1) ? -100 : boardToCenter(row2);
        entry.col2 = (col2 == -1) ? -100 : boardToCenter(col2);
        entry.timestamp = LocalDateTime.now();
        entry.thinkingTimeMs = thinkingTimeMs;
        entry.totalTimeMs = (player == PieceColor.BLACK) ? blackTotalTime : whiteTotalTime;
        entry.isFirstMove = isFirstMove;

        logEntries.add(entry);

        try {
            writer.write(entry.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logGameEnd(PieceColor winner, String reason) {
        try {
            writer.write("\n");
            writer.write("╔════════════════════════════════════════════════╗\n");
            writer.write("║                                               对局结束                                                            ║\n");
            writer.write("╠════════════════════════════════════════════════╣\n");

            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(gameStartTime, endTime);

            writer.write(String.format("║  结束时间: %-104s ║\n",
                    endTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss (E)", java.util.Locale.CHINA))));

            if (winner != null) {
                String winnerStr = (winner == PieceColor.BLACK) ? "黑棋" : "白棋";
                writer.write(String.format("║  获胜方  : %-104s ║\n", winnerStr + " 获胜！"));
            } else {
                writer.write(String.format("║  对局结果: %-104s ║\n", "平局"));
            }

            writer.write(String.format("║  结束原因: %-104s ║\n", reason));
            writer.write(String.format("║  总步数  : %-104d ║\n", logEntries.size()));
            writer.write("║                                                                                                                    ║\n");

            // 对局统计
            writer.write("║  ┌─ 对局统计 ─────────────────────────────────────────────────────────────────────────────────────────────┐  ║\n");
            writer.write(String.format("║  │  黑方累计思考时间: %-82s  │  ║\n", formatTimeLong(blackTotalTime)));
            writer.write(String.format("║  │  白方累计思考时间: %-82s  │  ║\n", formatTimeLong(whiteTotalTime)));
            writer.write(String.format("║  │  对局总用时      : %-82s  │  ║\n",
                    String.format("%d分%d秒", duration.toMinutes(), duration.getSeconds() % 60)));

            // 平均步时
            if (logEntries.size() > 0) {
                long blackCount = logEntries.stream().filter(e -> e.player == PieceColor.BLACK).count();
                long whiteCount = logEntries.stream().filter(e -> e.player == PieceColor.WHITE).count();

                String blackAvg = blackCount > 0 ? formatTimeLong(blackTotalTime / blackCount) : "N/A";
                String whiteAvg = whiteCount > 0 ? formatTimeLong(whiteTotalTime / whiteCount) : "N/A";

                writer.write(String.format("║  │  黑方平均步时    : %-82s  │  ║\n", blackAvg));
                writer.write(String.format("║  │  白方平均步时    : %-82s  │  ║\n", whiteAvg));
            }

            writer.write("║  └──────────────────────────────────────────────────────────────────────────────────────────────────────┘  ║\n");
            writer.write("╚════════════════════════════════════════════════╝\n");

            writer.write("\n");
            writer.write("═══════════════════════════════════════════════════\n");
            writer.write("                                   感谢使用六子棋对战系统！期待下次对局！\n");
            writer.write("═══════════════════════════════════════════════════\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTimeLong(long ms) {
        double seconds = ms / 1000.0;
        if (seconds < 60) {
            return String.format("%.3f秒", seconds);
        } else if (seconds < 3600) {
            int minutes = (int) (seconds / 60);
            double remainSeconds = seconds % 60;
            return String.format("%d分%.3f秒", minutes, remainSeconds);
        } else {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            double remainSeconds = seconds % 60;
            return String.format("%d小时%d分%.1f秒", hours, minutes, remainSeconds);
        }
    }

    public List<LogEntry> getAllEntries() {
        return new ArrayList<>(logEntries);
    }

    public String getFileName() {
        return fileName;
    }

    public long getBlackTotalTime() {
        return blackTotalTime;
    }

    public long getWhiteTotalTime() {
        return whiteTotalTime;
    }
}