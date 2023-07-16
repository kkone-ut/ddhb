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

from Util import Util


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
    latest_result: List[Agent] # 昨夜の霊媒結果
    whites: List[Agent] # 前日に黒結果に投票したエージェント（白っぽいエージェント）
    blacks: List[Agent] # 前日に白結果に投票したエージェント（黒っぽいエージェント）
    votefor_executed_agent: List[Agent] # 前日に追放されたエージェントに投票したエージェント


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
        self.latest_result = Species.UNC
        self.whites = []
        self.blacks = []
        self.votefor_executed_agent = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 3 # 最低でも3日目にCO
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue.clear()
        
        self.werewolves.clear()
        self.strategies = [True, False, False]
        self.strategyA = self.strategies[0] # 戦略A: COする日にちの変更（2日目CO）
        self.strategyB = self.strategies[1] # 戦略B: COする日にちの変更（1日目CO）
        self.others_medium_co.clear()
        self.latest_result = Species.UNC
        self.whites.clear()
        self.blacks.clear()
        self.votefor_executed_agent.clear()
        
        # 戦略A: 2日目CO
        if self.strategyA:
            self.co_date = 2 # 79%, 71%
        if self.strategyB:
            self.co_date = 1 # 73%, 70%


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        self.votefor_executed_agent.clear()
        vote_list = self.game_info.vote_list
        alive_comingout_map = {a.agent_idx: r.value for a, r in self.comingout_map.items() if self.is_alive(a)}
        Util.debug_print("alive_comingout_map:\t", alive_comingout_map)
        # Queue the medium result.
        # 霊結果
        judge: Optional[Judge] = self.game_info.medium_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            self.latest_result = judge.result
            Util.debug_print("latest_result:\t", self.latest_result)
            # Util.debug_print("vote_list:\t", len(vote_list))
            for vote in vote_list:
                if vote.target == judge.target:
                    self.votefor_executed_agent.append(vote.agent)
            votefor_executed_agent_no = [a.agent_idx for a in self.votefor_executed_agent]
            Util.debug_print("votefor_executed_agent:\t", len(self.votefor_executed_agent), votefor_executed_agent_no)
            # 黒結果
            if judge.result == Species.WEREWOLF:
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
        if 2 <= turn <= 6:
            # 黒結果
            if self.latest_result == Species.WEREWOLF:
                white_candidate = self.random_select(self.votefor_executed_agent)
                return Content(EstimateContentBuilder(white_candidate, Role.VILLAGER))
            # 白結果 or 結果なし
            else:
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
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.target == self.me and j.result == Species.WEREWOLF]
        # 白結果：投票したエージェント
        if self.latest_result == Species.HUMAN:
            vote_candidates = self.votefor_executed_agent
        # 黒結果：投票したエージェントを除く
        elif self.latest_result == Species.WEREWOLF:
            for agent in self.votefor_executed_agent:
                if agent in vote_candidates:
                    vote_candidates.remove(agent)
        # 投票候補の優先順位
        # 偽占い→対抗霊媒→霊結果から推定
        if fake_seers:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, fake_seers)
        elif self.others_medium_co:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.others_medium_co)
        else:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # # 候補なし → 偽占い以外からの黒結果
        # if not vote_candidates:
        #     reported_wolves: List[Agent] = [j.target for j in self.divination_reports if j.agent not in fake_seers and j.result == Species.WEREWOLF]
        #     vote_candidates = self.get_alive_others(reported_wolves)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
