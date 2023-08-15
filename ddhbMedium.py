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

from Util import Util
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
    latest_result: List[Agent] # 前日の霊媒結果
    votefor_executed_agent: List[Agent] # 前日に追放されたエージェントに投票したエージェント
    strategies: List[bool] # 戦略フラグのリスト


    def __init__(self) -> None:
        """Initialize a new instance of ddhbMedium."""
        super().__init__()
        self.co_date = 0
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue = deque()
        
        self.werewolves = []
        self.latest_result = Species.UNC
        self.votefor_executed_agent = []
        self.strategies = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 2
        self.found_wolf = False
        self.has_co = False
        self.my_judge_queue.clear()
        
        self.werewolves.clear()
        self.latest_result = Species.UNC
        self.votefor_executed_agent.clear()
        self.strategies = [True]
        self.strategyA = self.strategies[0] # 戦略A: COする日にちの変更（2日目CO）
        # 戦略A: 2日目CO
        if self.strategyA:
            self.co_date = 2


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        Util.debug_print("alive_comingout_map:\t", self.alive_comingout_map_str)
        
        self.latest_result = Species.UNC
        self.votefor_executed_agent.clear()
        vote_list = self.game_info.vote_list
        # 霊媒結果
        judge: Optional[Judge] = self.game_info.medium_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            self.latest_result = judge.result
            Util.debug_print("latest_result:\t", self.latest_result)
            for vote in vote_list:
                # 前日に追放されたエージェントに投票したエージェントを追加する
                if vote.target == judge.target and vote.agent != self.me:
                    self.votefor_executed_agent.append(vote.agent)
            Util.debug_print("votefor_executed_agent:\t", self.agent_to_index(self.votefor_executed_agent))
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
            Util.debug_print("霊媒CO：予定日")
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # 2: 人狼発見
        if not self.has_co and self.werewolves:
            Util.debug_print("霊媒CO：人狼発見")
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # 3: 他の霊媒がCOしたら(CCO)
        others_medium_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.MEDIUM]
        if not self.has_co and others_medium_co:
            Util.debug_print("霊媒CO：CCO")
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
        # ---------- 結果報告 ----------
        if self.has_co and self.my_judge_queue:
            judge: Judge = self.my_judge_queue.popleft()
            return Content(IdentContentBuilder(judge.target, judge.result))
        # ----- ESTIMATE, VOTE, REQUEST -----
        if 2 <= turn <= 6:
            # 黒結果
            # 前日、黒に投票したエージェントを村人っぽいとESTIMATE
            if self.latest_result == Species.WEREWOLF:
                l = len(self.votefor_executed_agent)
                if l != 0:
                    white_candidate = self.votefor_executed_agent[turn % len(self.votefor_executed_agent)]
                else:
                    white_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, self.get_alive_others(self.game_info.agent_list))
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
        
        return CONTENT_SKIP


    # 投票対象→OK
    def vote(self) -> Agent:
        # ----------  同数投票の処理 ---------- 
        latest_vote_list = self.game_info.latest_vote_list
        if latest_vote_list:
            self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # 投票候補：結果によって変える
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # 白結果：投票したエージェント
        # 修正：人狼が白結果に投票していない場合もあるから危険→全エージェントにする
        # 黒結果：投票したエージェントを除く
        if self.latest_result == Species.WEREWOLF:
            for agent in self.votefor_executed_agent:
                if agent in vote_candidates:
                    vote_candidates.remove(agent)
        # 投票対象の優先順位：偽占い→偽霊媒→人狼っぽいエージェント
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.target == self.me and j.result == Species.WEREWOLF]
        alive_fake_seers: List[Agent] = self.get_alive_others(fake_seers)
        # if a in vote_candidates としているから、aliveは保証されている
        others_medium_co: List[Agent] = [a for a in self.comingout_map if a in vote_candidates and self.comingout_map[a] == Role.MEDIUM]
        if alive_fake_seers:
            Util.debug_print("alive_fake_seers:\t", self.agent_to_index(alive_fake_seers))
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_fake_seers)
        elif others_medium_co:
            Util.debug_print("alive_others_medium_co:\t", self.agent_to_index(others_medium_co))
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, others_medium_co)
        else:
            Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ----- 投票ミスを防ぐ -----
        if self.vote_candidate == AGENT_NONE or self.vote_candidate == self.me:
            Util.debug_print("vote_candidates: AGENT_NONE or self.me")
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
