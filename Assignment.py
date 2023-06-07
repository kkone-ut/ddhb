from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import ScoreMatrix
import numpy as np
from Util import Util

class Assignment:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _assignment) -> None:
        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)
        self.player = _player
        self.me = _player.me
        self.score = 0
        self.assignment = _assignment
    
    def __str__(self) -> str:
        m = ""
        for r in self.assignment:
            m += r.name[0] + ", "
        return m
    
    def __eq__(self, o: object) -> bool:
        return np.array_equal(self.assignment, o.assignment)

    # 外部クラスから assignment.assignment[i] ではなく assignment[i] でアクセスできるようにする
    def __getitem__(self, agent) -> Role:
        if type(agent) is Agent:
            return self.assignment[agent.agent_idx-1]
        elif type(agent) is int:
            return self.assignment[agent]
        else:
            if Util.debug_mode:
                raise TypeError
            else:
                return self.assignment[0]
        
    # 役職の割り当ての評価値を計算する
    def evaluate(self, score_matrix: ScoreMatrix, debug = False) -> float:
        self.score = 0
        for i in range(self.N):
            for j in range(self.N):
                self.score += score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])
                if debug and abs(score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])) >= 0.45:
                    Util.debug_print("score_matrix.get_score(", i+1, self.assignment[i], j+1, self.assignment[j], ")\t", "= ",round(score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])*10, 2))
        
        return self.score
    
    # エージェント i とエージェント j の役職を入れ替える
    def swap(self, i: int, j: int) -> None:
        self.assignment[i], self.assignment[j] = self.assignment[j], self.assignment[i]

    # リストをシャッフルする
    # fixed_positions で指定した位置はシャッフルしない
    def shuffle(self, times=-1, fixed_positions=[]):
        times = times if times != -1 else self.N

        a = np.arange(self.N)
        a = np.setdiff1d(a, np.array(fixed_positions))

        for _ in range(times):
            i = np.random.randint(len(a))
            j = np.random.randint(len(a))
            i = a[i]
            j = a[j]
            self.assignment[i], self.assignment[j] = self.assignment[j], self.assignment[i]