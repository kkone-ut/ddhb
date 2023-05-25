from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import numpy as np
import itertools
import time
import copy

from Util import Util
from Assignment import Assignment
from ScoreMatrix import ScoreMatrix


class RolePredictor:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _score_matrix: ScoreMatrix) -> None:
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
        # 15人村では重すぎるので、ランダムに500個だけ列挙し、少しずつ追加・削除を行う
        if self.N == 5:
            time_start = time.time()
            for p in Util.unique_permutations(assignment):
                self.assignments.append(Assignment(game_info, game_setting, _player, p))
            time_end = time.time()
            print('time: ', time_end - time_start)
            print(len(self.assignments))
        else:
            for _ in range(500):
                a = Assignment(game_info, game_setting, _player, np.copy(assignment))
                a.shuffle(fixed_positions=self.fixed_positions)
                self.assignments.append(a)
                print(a.assignment)
        
        print("my role:", game_info.my_role)
        print("my idx:", self.me.agent_idx-1)
            
    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        # assignments の評価値を更新しつつ、評価値が -inf のものを削除する
        time_start = time.time()

        for assignment in self.assignments[:]:
            if assignment.evaluate(self.score_matrix) == -float('inf'):
                self.assignments.remove(assignment)

        for _ in range(10):
            self.addAssignments(game_info, game_setting)
        self.assignments = sorted(self.assignments, key=lambda x: x.score, reverse=True)[:500]

        time_end = time.time()
        Util.debug_print("len:", len(self.assignments))
        Util.debug_print("time:", time_end - time_start)
        Util.debug_print("avg:", (time_end - time_start) / len(self.assignments))
    
    # 今ある割り当てを少しだけ変更して追加する
    def addAssignments(self, game_info: GameInfo, game_setting: GameSetting, fixed_positions=[]) -> None:
        base = np.random.choice(self.assignments).assignment
        assignment = Assignment(game_info, game_setting, self.player, np.copy(base))
        times = int(abs(np.random.normal(scale=0.2) * self.N)) + 1 # 基本的に1~3程度の小さな値
        assignment.shuffle(times, self.fixed_positions)
        self.assignments.append(assignment)

    def getProbAll(self) -> np.ndarray:
        prob = np.zeros(self.N, self.M)
        return prob
        # for assignment in self.assignments:
        #     prob += assignment.getProb()
        # return prob / len(self.assignments)
    
    def getProb(self, i: int, role: Role) -> float:
        prob = 0
        return prob
        # for assignment in self.assignments:
        #     prob += assignment.getProb(i, role)
        # return prob / len(self.assignments)