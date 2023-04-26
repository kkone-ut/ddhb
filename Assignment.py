from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import numpy as np
import ScoreMatrix

class Assignment:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        self.player = _player
        self.me = _player.me
        self.score = 0
        
        # 役職の割り当てを表す配列
        self.assignment = np.array([], dtype=Role)
        
        # 役職の割り当ての初期値を設定する
        # 5人村なら [Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF] のような感じ
        for role, num in game_setting.role_num_map.items():
            # self.assignment に num 個だけ role を追加する
            self.assignment = np.append(self.assignment, np.full(num, role))

    def evaluate(self, score_matrix: ScoreMatrix) -> float:
        # 役職の割り当ての評価値を計算する
        self.score = 0
        for i in range(self.N):
            for j in range(self.M):
                self.score += score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])
        
        return self.score
    
    # エージェントiとエージェントjの役職を入れ替える
    # 全通りの割当を考える場合は不要
    def swap(self, i: int, j: int) -> None:
        pass