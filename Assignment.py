from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import ScoreMatrix
import numpy as np

class Assignment:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _assignment) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
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
    def __getitem__(self, i: int) -> Role:
        return self.assignment[i]
        
    # 役職の割り当ての評価値を計算する
    def evaluate(self, score_matrix: ScoreMatrix) -> float:
        self.score = 0
        for i in range(self.N):
            for j in range(self.N):
                self.score += score_matrix.get_score(i, self.assignment[i], j, self.assignment[j])
        
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