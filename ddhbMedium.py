#
# medium.py
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

from collections import deque
from typing import Deque, List, Optional

from aiwolf import (Agent, ComingoutContentBuilder, Content, GameInfo,
                    GameSetting, IdentContentBuilder, Judge, Role, Species,
                    VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP
from ddhbVillager import ddhbVillager

# 霊媒
class ddhbMedium(ddhbVillager):
    """ ddhb medium agent. """

    co_date: int # COする日にち
    """Scheduled comingout date."""
    found_wolf: bool # 人狼を見つけたか
    """Whether or not a werewolf is found."""
    has_co: bool # COしたか
    """Whether or not comingout has done."""
    my_judge_queue: Deque[Judge] # 自身の霊媒結果キュー
    """Queue of medium results."""
    
    werewolves: List[Agent] # 人狼結果のエージェント


    def __init__(self) -> None:
        """Initialize a new instance of ddhbMedium."""
        super().__init__()
        self.co_date = 0
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue = deque()
        
        self.werewolves = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 3 # 最低でも3日目にCO → 変更する
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue.clear()
        
        self.werewolves.clear()


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        # Queue the medium result.
        # 昼に霊結果
        judge: Optional[Judge] = self.game_info.medium_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            if judge.result == Species.WEREWOLF: # 黒結果
                self.found_wolf = True
                self.werewolves.append(judge.target) # 人狼リストに追加
            # スコアの更新
            self.score_matrix.my_identified(self.game_info, self.game_setting, judge.target, judge.result)


    # CO、結果報告→OK
    def talk(self) -> Content:
        # Do comingout if it's on scheduled day or a werewolf is found.
        # CO : 予定の日にち or 人狼発見 → 追加：対抗がCOしたら
        if not self.has_co and (self.game_info.day == self.co_date or self.found_wolf):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # 追加：他の霊媒がCOしたら(CCO)
        others_medium_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.MEDIUM]
        if not self.has_co and others_medium_co:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        
        # Report the medium result after doing comingout.
        # 結果報告
        if self.has_co and self.my_judge_queue:
            judge: Judge = self.my_judge_queue.popleft()
            return Content(IdentContentBuilder(judge.target, judge.result))
        # Fake seers.
        # 偽占い＝自分に黒結果を出している占い
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports
                                   if j.target == self.me and j.result == Species.WEREWOLF]
        # Vote for one of the alive fake mediums.
        # 投票候補：偽霊媒
        candidates: List[Agent] = [a for a in self.comingout_map
                                   if self.is_alive(a) and self.comingout_map[a] == Role.MEDIUM]
        # Vote for one of the alive agents that were judged as werewolves by non-fake seers
        # if there are no candidates.
        # 候補なし → 偽占い以外からの黒結果
        if not candidates:
            reported_wolves: List[Agent] = [j.target for j in self.divination_reports
                                            if j.agent not in fake_seers and j.result == Species.WEREWOLF]
            candidates = self.get_alive_others(reported_wolves)
        # Vote for one of the alive fake seers if there are no candidates.
        # 生存する偽占い
        if not candidates:
            candidates = self.get_alive(fake_seers)
        # Vote for one of the alive agents if there are no candidates.
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
    
    
    # 投票対象→OK
    # 占い師と同じ方針
    def vote(self) -> Agent:
        # 投票候補：人狼結果リスト
        vote_candidates: List[Agent] = self.get_alive(self.werewolves)
        target: Agent = self.random_select(vote_candidates)
        
        # 偽占いに投票しても投票が集まらないため、最も人狼っぽいエージェントに投票
        if not vote_candidates:
            # 生存者
            vote_candidates = self.get_alive_others(self.game_info.agent_list)
            target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        
        return target if target != AGENT_NONE else self.me
