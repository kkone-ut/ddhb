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

import random
from collections import deque
from typing import Deque, List, Optional

from aiwolf import (Agent, ComingoutContentBuilder, Content, GameInfo,
                    GameSetting, IdentContentBuilder, Judge, Role, Species,
                    VoteContentBuilder, EstimateContentBuilder, RequestContentBuilder)
from aiwolf.constant import AGENT_NONE, AGENT_ANY

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
    strategies: List[bool] # 戦略フラグのリスト
    others_medium_co: List[Agent] # 他の霊媒のCOリスト


    def __init__(self) -> None:
        """Initialize a new instance of ddhbMedium."""
        super().__init__()
        self.co_date = 0
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue = deque()
        
        self.werewolves = []
        self.strategies = []
        self.others_medium_co = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 3 # 最低でも3日目にCO
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue.clear()
        
        self.werewolves.clear()
        self.strategies = [True, False, False]
        self.strategyA = self.strategies[0] # 戦略A: COする日にちの変更（2日目CO）
        self.strategyB = self.strategies[1]
        self.others_medium_co.clear()
        
        # 戦略A: 2日目CO
        if self.strategyA:
            self.co_date = 2


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        # Queue the medium result.
        # 霊結果
        judge: Optional[Judge] = self.game_info.medium_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            if judge.result == Species.WEREWOLF: # 黒結果
                self.found_wolf = True
                self.werewolves.append(judge.target) # 人狼リストに追加
            # スコアの更新
            self.score_matrix.my_identified(self.game_info, self.game_setting, judge.target, judge.result)


    # CO、結果報告、投票宣言→OK
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.vote_candidate = self.vote()
        # ---------- CO ----------        
        # 絶対にCOする→1,2,3
        # 1: 予定の日にち
        if not self.has_co and day == self.co_date:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # 2: 人狼発見
        if not self.has_co and self.werewolves:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # 3: 他の霊媒がCOしたら(CCO)
        self.others_medium_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.MEDIUM]
        if not self.has_co and self.others_medium_co:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # ---------- 結果報告 ----------
        if self.has_co and self.my_judge_queue:
            judge: Judge = self.my_judge_queue.popleft()
            return Content(IdentContentBuilder(judge.target, judge.result))
        # ---------- 投票宣言 ----------
        # ----- ESTIMATE, VOTE, REQUEST -----
        if turn >= 2 and turn <= 7:
            rnd = random.randint(0, 2)
            if rnd == 0:
                return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
            elif rnd == 1:
                return Content(VoteContentBuilder(self.vote_candidate))
            else:
                return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
        else:
            return CONTENT_SKIP


    # 投票対象→OK
    def vote(self) -> Agent:
        self.others_medium_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.MEDIUM]
        # 投票宣言候補：偽霊媒かつ生存者
        vote_candidates: List[Agent] = self.get_alive(self.others_medium_co)
        # 偽占い＝自分に黒結果を出している占い
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.target == self.me and j.result == Species.WEREWOLF]
        # 候補なし → 偽占い以外からの黒結果
        if not vote_candidates:
            reported_wolves: List[Agent] = [j.target for j in self.divination_reports if j.agent not in fake_seers and j.result == Species.WEREWOLF]
            vote_candidates = self.get_alive_others(reported_wolves)
        # 候補なし → 偽占いかつ生存者
        if not vote_candidates:
            vote_candidates = self.get_alive(fake_seers)
        # 候補なし → 生存者
        if not vote_candidates:
            vote_candidates = self.get_alive_others(self.game_info.agent_list)
        # 投票宣言対象：候補の中で人狼っぽいエージェント
        vote_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return vote_candidate if vote_candidate != AGENT_NONE else self.me
