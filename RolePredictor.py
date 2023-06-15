from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role
import numpy as np
import time
from collections import defaultdict
from typing import List, Dict

from Util import Util
from Assignment import Assignment
from ScoreMatrix import ScoreMatrix
from aiwolf.constant import AGENT_NONE

import library.timeout_decorator as timeout_decorator
from library.SortedSet import SortedSet

class RolePredictor:

    # 保持しておく役職の割り当ての数
    # これを超えたら評価の低いものから削除する
    # 制限時間的に最大500個
    ASSIGNMENT_NUM = 100
    ADDITIONAL_ASSIGNMENT_NUM = 100

    def get_initail_assignment(self) -> np.ndarray:
        # 役職の割り当ての初期値を設定する
        # 5人村なら [Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF] のような感じ
        assignment = np.array([], dtype=Role)
        for role, num in self.game_setting.role_num_map.items():
            # self.assignment に num 個だけ role を追加する
            assignment = np.append(assignment, np.full(num, role))

        # プレイヤーの位置を固定する
        idx = np.where(assignment == self.game_info.my_role)[0][0]
        assignment[self.me.agent_idx-1], assignment[idx] = assignment[idx], assignment[self.me.agent_idx-1]

        return assignment

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player, _score_matrix: ScoreMatrix) -> None:
        self.game_setting = game_setting
        self.game_info = game_info
        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)
        self.player = _player
        self.me = _player.me
        # assignments は現在保持している Assignment の SortedSet (C++ の std::set みたいなもの)
        # assignments_set はこれまでに作成した Assignment の集合 (リストから外れても保持しておく)
        self.assignments: SortedSet = SortedSet()
        self.assignments_set = set()
        self.score_matrix = _score_matrix
        self.fixed_positions = [self.me.agent_idx-1]

        assignment = self.get_initail_assignment()

        # assignment のすべての並び替えを列挙する
        # 5人村はすべて列挙する
        # 15人村では重すぎるので、ランダムに数個だけ列挙し、少しずつ追加・削除を行う
        if self.N == 5:
            for p in Util.unique_permutations(assignment):
                self.assignments.add(Assignment(game_info, game_setting, _player, np.copy(p)))
        else:
            try: 
                self.addAssignments(game_info, game_setting, self.ADDITIONAL_ASSIGNMENT_NUM // 5)
            except timeout_decorator.TimeoutError:
                Util.error_print("TimeoutError:\t", "RolePredictor.__init__")
    
    # すべての割り当ての評価値を計算する
    @timeout_decorator.timeout(0.04)
    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        self.game_info = game_info

        Util.start_timer("RolePredictor.update")
        Util.debug_print("len(self.assignments)1:", len(self.assignments))

        # assignments の評価値を更新
        for assignment in self.assignments:
            # assignment を変更するので一度削除して、評価した後に再度追加する
            res = self.assignments.discard(assignment)
            assignment.evaluate(self.score_matrix)
            if assignment.score != -float('inf'):
                self.assignments.add(assignment)

        Util.debug_print("len(self.assignments)2:", len(self.assignments))
        Util.end_timer("RolePredictor.update", 50)

        # todo: ここで確率の更新をしてキャッシュする
        # self.getProbAll()

    @timeout_decorator.timeout(0.04)
    def addAssignments(self, game_info: GameInfo, game_setting: GameSetting, num: int = -1) -> None:
        if num == -1:
            num = self.ADDITIONAL_ASSIGNMENT_NUM
        
        if self.N == 5: # 5人村ならすべて列挙しているので、追加する必要はない
            return
        
        Util.start_timer("RolePredictor.addAssignments")

        # 新しい割り当てを追加する
        for _ in range(num):
            self.addAssignment(game_info, game_setting)

        Util.end_timer("RolePredictor.addAssignments", 50)

    def addAssignment(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        if len(self.assignments) < self.ASSIGNMENT_NUM or np.random.rand() < 0.1:
            # 割り当てが少ない場合は初期割り当てをシャッフルして追加する (多様性確保のため)
            # そうでなくても、10%の確率で初期割り当てをシャッフルして追加する (遺伝的アルゴリズムでいう突然変異)
            base = self.get_initail_assignment()
            times = self.N
        else:
            # 既にある割り当てからランダムに1つ選んで少しだけシャッフルして追加する
            assignment_idx = np.random.randint(len(self.assignments))
            base = self.assignments[assignment_idx].assignment
            times = int(abs(np.random.normal(scale=0.2) * self.N)) + 1 # 基本的に1~3程度の小さな値 (正規分布を使用)
        
        # 指定回数シャッフルする
        assignment = Assignment(game_info, game_setting, self.player, np.copy(base))
        assignment.shuffle(times, self.fixed_positions)

        # すでにある割り当てと同じなら追加しない
        if assignment in self.assignments_set:
            return
        
        # 評価値を計算
        assignment.evaluate(self.score_matrix)

        # 割り当て数が超過していたら、スコアの低いものから削除する
        while len(self.assignments) > self.ASSIGNMENT_NUM:
            self.assignments.pop(0)
        
        # 割り当て数が足りなかったら追加する
        # 丁度だった場合は、すでにある割り当てよりもスコアが高ければ追加する
        if len(self.assignments) < self.ASSIGNMENT_NUM:
            self.assignments.add(assignment)
        elif assignment.score > self.assignments[0].score:
            self.assignments.discard(self.assignments[0])
            self.assignments.add(assignment)

        # 割り当て重複チェック用のセットに追加
        self.assignments_set.add(assignment)

    # 各プレイヤーの役職の確率を表す二次元配列を返す
    # (実際には defaultdict[Agent, defaultdict[Role, float]])
    # p[a][r] はエージェント a が役職 r である確率 (a: Agent, r: Role)
    def getProbAll(self) -> "defaultdict[Agent, defaultdict[Role, float]]":

        # 各割り当ての相対確率を計算する
        relative_prob = np.zeros(len(self.assignments))
        sum_relative_prob = 0
        for i, assignment in enumerate(self.assignments):
            # スコアは対数尤度なので、exp して相対確率に変換する
            try:
                relative_prob[i] = np.exp(assignment.score / 10) # スコアが大きいとオーバーフローするので10で割る
            except RuntimeWarning:
                Util.error_print("OverflowError", assignment.score)
            sum_relative_prob += relative_prob[i]
        
        # 各割り当ての相対確率を確率に変換する
        assignment_prob = np.zeros(len(self.assignments))
        for i in range(len(assignment_prob)):
            assignment_prob[i] = relative_prob[i] / sum_relative_prob

        # 各プレイヤーの役職の確率を計算する
        # ndarray だと添字に Role を使えないので、defaultdict[Role, float] の配列を使う
        probs = defaultdict(lambda: defaultdict(float))

        for i, assignment in enumerate(self.assignments):
            for a in self.game_info.agent_list:
                probs[a][assignment[a]] += assignment_prob[i]
        
        return probs
    
    # i 番目のプレイヤーが役職 role である確率を返す
    # 複数回呼び出す場合は getProbAll() を呼んだほうが効率的
    def getProb(self, agent, role: Role) -> float:
        if type(agent) == int:
            agent = self.game_info.agent_list[agent]
        p = self.getProbAll()
        return p[agent][role]
    
    # 指定された役職である確率が最も高いプレイヤーの番号を返す
    def chooseMostLikely(self, role: Role, agent_list: List[Agent] = None) -> Agent:
        if agent_list is None:
            agent_list = self.game_info.agent_list
        if len(agent_list) == 0:
            return AGENT_NONE
        
        p = self.getProbAll()
        ret_agent = agent_list[0]
        for a in agent_list:
            if p[a][role] > p[ret_agent][role]:
                ret_agent = a
                
        return ret_agent

    def getMostLikelyRole(self, agent: Agent) -> Role:
        p = self.getProbAll()
        ret_role = Role.VILLAGER
        for r in Role:
            if p[agent][r] > p[agent][ret_role]:
                ret_role = r
        return ret_role