#
# werewolf.py
#
# Copyright 2022 OTSUKI Takashi
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import random
from collections import deque
from typing import List, Deque

from aiwolf import (Agent, AttackContentBuilder, ComingoutContentBuilder,
                    Content, GameInfo, GameSetting, Judge, Role, Species)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP, JUDGE_EMPTY
from ddhbPossessed import ddhbPossessed

# 人狼
class ddhbWerewolf(ddhbPossessed):
    """ddhb werewolf agent."""

    allies: List[Agent] # 仲間の人狼
    """Allies."""
    humans: List[Agent] # 村陣営
    """Humans."""
    attack_vote_candidate: Agent # 襲撃対象
    """The candidate for the attack voting."""
    
    N: int # エージェント数
    M: int # 役職数
    co_date: int # COする日にち
    fake_role: Role # 騙る役職
    taikou: List[Agent] # 対抗リスト
    fake_judge_queue: Deque[Judge] # 偽結果キュー
    PP_flag: bool # PPフラグ


    def __init__(self) -> None:
        """Initialize a new instance of ddhbWerewolf."""
        super().__init__()
        self.allies = []
        self.humans = []
        self.attack_vote_candidate = AGENT_NONE

        self.co_date = 0
        self.fake_role = Role.UNC
        self.taikou = []
        self.fake_judge_queue = deque()
        self.PP_flag = False


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.allies = list(self.game_info.role_map.keys())
        self.humans = [a for a in self.game_info.agent_list if a not in self.allies]
        # Do comingout on the day that randomly selected from the 1st, 2nd and 3rd day.
        self.co_date = random.randint(1, 3) # COする日にち
        # Choose fake role randomly.
        # 騙りはランダムに
        self.fake_role = random.choice([r for r in [Role.VILLAGER, Role.SEER, Role.MEDIUM]
                                        if r in self.game_info.existing_role_list])
        self.taikou.clear()
        self.fake_judge_queue.clear()
        self.PP_flag = False
        
        self.strategies = [True, False, False]
        self.strategyA = self.strategies[0]
        self.strategyB = self.strategies[1]
        self.strategyC = self.strategies[2]


    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        """Generate a fake judgement."""
        # Determine the target of the fake judgement.
        # 対象の決定
        target: Agent = AGENT_NONE
        # 占い騙り → ランダムセレクト
        if self.fake_role == Role.SEER:  # Fake seer chooses a target randomly.
            if self.game_info.day != 0:
                target = self.random_select(self.get_alive(self.not_judged_agents))
        # 霊媒騙り → 死者
        elif self.fake_role == Role.MEDIUM:
            target = self.game_info.executed_agent if self.game_info.executed_agent is not None \
                else AGENT_NONE
        if target == AGENT_NONE:
            return JUDGE_EMPTY
        # Determine a fake result.
        # If the target is a human
        # and the number of werewolves found is less than the total number of werewolves,
        # judge as a werewolf with a probability of 0.3.
        # 騙り結果 → 変更する
        # 発見人狼数が人狼総数より少ない and 確率0.3 で黒結果
        result: Species = Species.WEREWOLF if target in self.humans \
            and len(self.werewolves) < self.num_wolves and random.random() < 0.3 \
            else Species.HUMAN
        return Judge(self.me, self.game_info.day, target, result)


    # 昼スタート
    def day_start(self) -> None:
        super().day_start()
        self.attack_vote_candidate = AGENT_NONE
        
        


    # 内通
    def whisper(self) -> Content:
        # Declare the fake role on the 1st day,
        # and declare the target of attack vote after that.
        
        # 初日は騙る役職を宣言
        if self.game_info.day == 0:
            return Content(ComingoutContentBuilder(self.me, self.fake_role))
        # Choose the target of attack vote.
        # Vote for one of the agent that did comingout.
        
        # 襲撃対象を選ぶ
        # 襲撃対象
        candidates: List[Agent] = []
        
        # 戦略A: 占いCOで占い師っぽい方（人狼仲間は除く）
        if self.strategyA:
            candidates = [a for a in self.comingout_map if self.is_alive(a) 
                            and a in self.humans and self.comingout_map[a] == Role.SEER]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, candidates)
        
        # 戦略B: 霊媒COで霊媒師っぽい方（人狼仲間は除く）
        if self.strategyB:
            candidates = [a for a in self.comingout_map if self.is_alive(a)
                            and a in self.humans and self.comingout_map[a] == Role.MEDIUM]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, candidates)
        
        # 戦略C: 狩人COで狩人っぽい方（人狼仲間は除く）
        if self.strategyC:
            candidates = [a for a in self.comingout_map if self.is_alive(a)
                            and a in self.humans and self.comingout_map[a] == Role.BODYGUARD]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, candidates)
        
        # 候補なし→襲撃スコア
        if self.attack_vote_candidate == AGENT_NONE:
            candidates = self.get_alive_others(self.humans)
            p = self.role_predictor.getProbAll()
            mx_score = 0
            for agent in candidates:
                score = p[agent][Role.VILLAGER] + p[agent][Role.SEER]*4 + p[agent][Role.MEDIUM]*3 + p[agent][Role.BODYGUARD]*2
                if score > mx_score:
                    mx_score = score
                    self.attack_vote_candidate = agent
        
        if self.attack_vote_candidate == AGENT_NONE:
            self.attack_vote_candidate = self.random_select(self.get_alive_others(self.humans))
            if self.attack_vote_candidate != AGENT_NONE:
                return Content(AttackContentBuilder(self.attack_vote_candidate))
        
        # # 襲撃対象：COしているエージェント
        # candidates: List[Agent] = [a for a in self.get_alive(self.humans) if a in self.comingout_map]
        # # Vote for one of the alive human agents if there are no candidates.
        # # 候補なし→生存村人
        # if not candidates:
        #     candidates = self.get_alive(self.humans)
        # # Declare which to vote for if not declare yet or the candidate is changed.
        # if self.attack_vote_candidate == AGENT_NONE or self.attack_vote_candidate not in candidates:
        #     self.attack_vote_candidate = self.random_select(candidates)
        #     if self.attack_vote_candidate != AGENT_NONE:
        #         return Content(AttackContentBuilder(self.attack_vote_candidate))
        return CONTENT_SKIP


    # 襲撃→OK
    def attack(self) -> Agent:
        return self.attack_vote_candidate if self.attack_vote_candidate != AGENT_NONE else self.me
