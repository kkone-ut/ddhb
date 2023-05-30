from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import numpy as np
import time
from collections import defaultdict

from Util import Util
from Assignment import Assignment
from ScoreMatrix import ScoreMatrix


class RolePredictor:

    # 保持しておく役職の割り当ての数
    # これを超えたら評価の低いものから削除する
    # 制限時間的に最大500個
    ASSIGNMENT_NUM = 50

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _score_matrix: ScoreMatrix) -> None:
        self.geme_setting = game_setting
        self.game_info = game_info
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        self.player = _player
        self.me = _player.me
        self.assignments = []
        self.score_matrix = _score_matrix
        self.fixed_positions = [self.me.agent_idx-1]

        # 役職の割り当ての初期値を設定する
        # 5人村なら [Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF] のような感じ
        assignment = np.array([], dtype=Role)
        for role, num in game_setting.role_num_map.items():
            # self.assignment に num 個だけ role を追加する
            assignment = np.append(assignment, np.full(num, role))

        # プレイヤーの位置を固定する
        idx = np.where(assignment == game_info.my_role)[0][0]
        assignment[self.me.agent_idx-1], assignment[idx] = assignment[idx], assignment[self.me.agent_idx-1]

        # assignment のすべての並び替えを列挙する
        # 5人村はすべて列挙する
        # 15人村では重すぎるので、ランダムに ASSIGNMENT_NUM 個だけ列挙し、少しずつ追加・削除を行う
        if self.N == 5:
            time_start = time.time()
            for p in Util.unique_permutations(assignment):
                self.assignments.append(Assignment(game_info, game_setting, _player, np.copy(p)))
            time_end = time.time()
            print('time: ', time_end - time_start)
            print(len(self.assignments))
        else:
            for _ in range(self.ASSIGNMENT_NUM):
                a = Assignment(game_info, game_setting, _player, np.copy(assignment))
                a.shuffle(fixed_positions=self.fixed_positions)
                self.assignments.append(a)
                print(a.assignment)
        
        print("my role:", game_info.my_role)
        print("my idx:", self.me.agent_idx-1)
    
    # すべての割り当ての評価値を計算する
    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        self.game_info = game_info

        time_start = time.time()

        # assignments の評価値を更新しつつ、評価値が -inf のものを削除する
        for assignment in self.assignments[:]:
            if assignment.evaluate(self.score_matrix) == -float('inf'):
                self.assignments.remove(assignment)

        # 新しい割り当てを追加する
        for _ in range(10):
            self.addAssignments(game_info, game_setting)
        
        # 評価値の高い順にソートして、上位 ASSIGNMENT_NUM 個だけ残す
        self.assignments = sorted(self.assignments, key=lambda x: x.score, reverse=True)[:self.ASSIGNMENT_NUM]

        time_end = time.time()
        if time_end - time_start > 0.1:
            Util.debug_print("len:", len(self.assignments))
            Util.debug_print("time:", time_end - time_start)
            Util.debug_print("avg:", (time_end - time_start) / len(self.assignments))

        self.getProbAll()
    
    # 今ある割り当てを少しだけ変更して追加する
    def addAssignments(self, game_info: GameInfo, game_setting: GameSetting, fixed_positions=[]) -> None:
        base = np.random.choice(self.assignments).assignment
        assignment = Assignment(game_info, game_setting, self.player, np.copy(base))
        times = int(abs(np.random.normal(scale=0.2) * self.N)) + 1 # 基本的に1~3程度の小さな値 (正規分布を使用)
        assignment.shuffle(times, self.fixed_positions)
        self.assignments.append(assignment)

    # 各プレイヤーの役職の確率を表す二次元配列を返す
    # (実際には defaultdict[Role, float] の配列)
    # p[i][r] は i 番目のプレイヤーが役職 r である確率 (i: int, r: Role)
    def getProbAll(self) -> np.ndarray:

        # 各割り当ての相対確率を計算する
        relative_prob = np.zeros(len(self.assignments))
        sum_relative_prob = 0
        for i, assignment in enumerate(self.assignments):
            # スコアは対数尤度なので、exp して相対確率に変換する
            relative_prob[i] = np.exp(assignment.score)
            sum_relative_prob += relative_prob[i]
        
        # 各割り当ての相対確率を確率に変換する
        assignment_prob = np.zeros(len(self.assignments))
        for i in range(len(assignment_prob)):
            assignment_prob[i] = relative_prob[i] / sum_relative_prob

        # 各プレイヤーの役職の確率を計算する
        # ndarray だと添字に Role を使えないので、defaultdict[Role, float] の配列を使う
        probs = np.array([defaultdict[Role, float](float) for _ in range(self.N)])

        for i, assignment in enumerate(self.assignments):
            for j in range(self.N):
                probs[j][assignment[j]] += assignment_prob[i]
        
        return probs
    
    # i 番目のプレイヤーが役職 role である確率を返す
    # 複数回呼び出す場合は getProbAll() を呼んだほうが効率的
    def getProb(self, i: int, role: Role) -> float:
        p = self.getProbAll()
        return p[i][role]
    
    # 指定された役職である確率が最も高いプレイヤーの番号を返す
    def chooseMostLikely(self, role: Role) -> Agent:
        p = self.getProbAll()
        idx = 0
        for i in range(self.N):
            if p[i][role] > p[idx][role]:
                idx = i
        return self.game_info.agent_list[idx]