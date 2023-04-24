#
# possessed.py
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
from typing import Deque, List

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, GameInfo, GameSetting,
                    IdentContentBuilder, Judge, Role, Species,
                    VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP, JUDGE_EMPTY
from ddhbVillager import ddhbVillager

# 裏切り者
class ddhbPossessed(ddhbVillager):
    """ddhb possessed agent."""

    fake_role: Role # 騙る役職
    """Fake role."""
    co_date: int # COする日にち
    """Scheduled comingout date."""
    has_co: bool # COしたか
    """Whether or not comingout has done."""
    my_judgee_queue: Deque[Judge] # 自身の（占い or 霊媒）結果キュー
    """Queue of fake judgements."""
    not_judged_agents: List[Agent] # 占っていないエージェント
    """Agents that have not been judged."""
    num_wolves: int # 人狼数
    """The number of werewolves."""
    werewolves: List[Agent] # 人狼結果のエージェント
    """Fake werewolves."""

    def __init__(self) -> None:
        """Initialize a new instance of ddhbPossessed."""
        super().__init__()
        self.fake_role = Role.SEER
        self.co_date = 0
        self.has_co = False
        self.my_judgee_queue = deque()
        self.not_judged_agents = []
        self.num_wolves = 0
        self.werewolves = []

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.fake_role = Role.SEER # 占いのみ → 変更する
        self.co_date = 1 # 最低でも1日目にCO → 変更する
        self.has_co = False
        self.my_judgee_queue.clear()
        self.not_judged_agents = self.get_others(self.game_info.agent_list)
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()

    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        """Generate a fake judgement."""
        target: Agent = AGENT_NONE
        # 対象の決定
        # 占い騙り → ランダムセレクト
        if self.fake_role == Role.SEER:  # Fake seer chooses a target randomly.
            if self.game_info.day != 0:
                target = self.random_select(self.get_alive(self.not_judged_agents))
        # 霊媒騙り → 死者
        elif self.fake_role == Role.MEDIUM:
            target = self.game_info.executed_agent \
                if self.game_info.executed_agent is not None \
                else AGENT_NONE
        if target == AGENT_NONE:
            return JUDGE_EMPTY
        # Determine a fake result.
        # If the number of werewolves found is less than the total number of werewolves,
        # judge as a werewolf with a probability of 0.5.
        # 騙り結果 → 変更する
        # 発見人狼数が人狼総数より少ない and 確率1/2 で黒結果
        result: Species = Species.WEREWOLF \
            if len(self.werewolves) < self.num_wolves and random.random() < 0.5 \
            else Species.HUMAN
        return Judge(self.me, self.game_info.day, target, result)

    def day_start(self) -> None:
        super().day_start()
        # Process the fake judgement.
        # 昼に騙り結果
        judge: Judge = self.get_fake_judge()
        if judge != JUDGE_EMPTY:
            self.my_judgee_queue.append(judge)
            # 占い対象を、占っていないエージェントリストから除く
            if judge.target in self.not_judged_agents:
                self.not_judged_agents.remove(judge.target)
            # 人狼発見 → 人狼結果リストに追加
            if judge.result == Species.WEREWOLF:
                self.werewolves.append(judge.target)

    # CO、結果報告
    def talk(self) -> Content:
        # Do comingout if it's on scheduled day or a werewolf is found.
        # CO : 予定の日にち or 人狼発見
        if self.fake_role != Role.VILLAGER and not self.has_co \
                and (self.game_info.day == self.co_date or self.werewolves):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, self.fake_role))
        # Report the judgement after doing comingout.
        # 結果報告
        if self.has_co and self.my_judgee_queue:
            judge: Judge = self.my_judgee_queue.popleft()
            if self.fake_role == Role.SEER:
                return Content(DivinedResultContentBuilder(judge.target, judge.result))
            elif self.fake_role == Role.MEDIUM:
                return Content(IdentContentBuilder(judge.target, judge.result))
        # Vote for one of the alive fake werewolves.
        # 投票候補：人狼結果リスト
        candidates: List[Agent] = self.get_alive(self.werewolves)
        # Vote for one of the alive agent that declared itself the same role of Possessed
        # if there are no candidates.
        # 候補なし → 対抗
        if not candidates:
            candidates = self.get_alive([a for a in self.comingout_map
                                         if self.comingout_map[a] == self.fake_role])
        # Vite for one of the alive agents if there are no candidates.
        # 生存者
        if not candidates:
            candidates = self.get_alive_others(self.game_info.agent_list)
        # Declare which to vote for if not declare yet or the candidate is changed.
        # 候補からランダムセレクト
        if self.vote_candidate == AGENT_NONE or self.vote_candidate not in candidates:
            self.vote_candidate = self.random_select(candidates)
            if self.vote_candidate != AGENT_NONE:
                return Content(VoteContentBuilder(self.vote_candidate))
        return CONTENT_SKIP
