from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import ScoreMatrix

class Assignment:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _assignment) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        self.player = _player
        self.me = _player.me
        self.score = 0
        self.assignment = _assignment
        
    def evaluate(self, score_matrix: ScoreMatrix) -> float:
        # 役職の割り当ての評価値を計算する
        self.score = 0
        for i in range(self.N):
            for j in range(self.N):
                self.score += score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])
        
        return self.score
    
    # エージェントiとエージェントjの役職を入れ替える
    # 全通りの割当を考える場合は不要
    def swap(self, i: int, j: int) -> None:
        pass