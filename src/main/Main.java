package main;

import core.game.GameController;
import player.AIPlayer;
import player.AlphaBetaAI;

public class Main {
    public static void main(String[] args) {
        System.out.println("========== 六子棋 AI 对战 ==========");
        
        // 创建两个 AI 玩家
        AIPlayer player1 = new AlphaBetaAI("AlphaBot-1");
        AIPlayer player2 = new AlphaBetaAI("AlphaBot-2");
        
        // 创建游戏控制器
        GameController game = new GameController(player1, player2);
        
        // 开始游戏
        game.playGame();
        
        System.out.println("========== 游戏结束 ==========");
    }
}