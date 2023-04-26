from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import numpy as np
import itertools
import time

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

        # 役職の割り当ての初期値を設定する
        # 5人村なら [Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF] のような感じ
        assignment = np.array([], dtype=Role)
        for role, num in game_setting.role_num_map.items():
            # self.assignment に num 個だけ role を追加する
            assignment = np.append(assignment, np.full(num, role))

        # assignment のすべての並び替えを列挙する
        # 15人村では重すぎるので、5人村のときだけ実行する
        if self.N == 5:
            time_start = time.time()
            for p in Util.unique_permutations(assignment):
                self.assignments.append(Assignment(game_info, game_setting, _player, p))
            time_end = time.time()
            print('time: ', time_end - time_start)
            print(len(self.assignments))

    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        # assignments の評価値を更新しつつ、評価値が -inf のものを削除する
        time_start = time.time()
        for assignment in self.assignments[:]:
            if assignment.evaluate(self.score_matrix) == -float('inf'):
                self.assignments.remove(assignment)
        time_end = time.time()
        print('time: ', time_end - time_start)

        print(len(self.assignments))

        pass