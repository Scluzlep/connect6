package ui;

import javax.swing.*;
import java.awt.*;

public class WelcomeFrame extends JFrame {
    public WelcomeFrame() {
        setTitle("六子棋游戏");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 230, 210));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // 标题
        JLabel titleLabel = new JLabel("六子棋游戏");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        titleLabel.setForeground(new Color(139, 69, 19));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 副标题
        JLabel subtitleLabel = new JLabel("Connect6 - 人机对战");
        subtitleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(101, 67, 33));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 规则说明
        JTextArea rulesArea = new JTextArea(
            "游戏规则：\n\n" +
            "1. 黑棋先行，第一步只下一子（必须在中央附近）\n\n" +
            "2. 之后双方轮流，每回合各下两子\n\n" +
            "3. 率先在横、竖、斜任意方向形成六连者获胜\n\n" +
            "4. 游戏会自动记录落子日志和思考时间"
        );
        rulesArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        rulesArea.setBackground(new Color(240, 230, 210));
        rulesArea.setEditable(false);
        rulesArea.setFocusable(false);
        rulesArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        rulesArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(new Color(240, 230, 210));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton blackFirstButton = createStyledButton("我先手（黑棋）");
        blackFirstButton.addActionListener(e -> startGame(true));

        JButton whiteFirstButton = createStyledButton("AI先手（我执白）");
        whiteFirstButton.addActionListener(e -> startGame(false));

        buttonPanel.add(blackFirstButton);
        buttonPanel.add(whiteFirstButton);

        // 添加组件
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(rulesArea);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(160, 40));
        button.setBackground(new Color(139, 69, 19));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }

    private void startGame(boolean humanFirst) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            GameFrame gameFrame = new GameFrame(humanFirst);
            gameFrame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WelcomeFrame frame = new WelcomeFrame();
            frame.setVisible(true);
        });
    }
}