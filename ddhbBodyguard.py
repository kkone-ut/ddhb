#
# bodyguard.py
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
from typing import List

from aiwolf import Agent, GameInfo, GameSetting, Role, Species
from aiwolf import (ComingoutContentBuilder, Content, GuardedAgentContentBuilder,
                    EstimateContentBuilder, VoteContentBuilder, RequestContentBuilder)
from aiwolf.constant import AGENT_NONE, AGENT_ANY

from Util import Util
from const import CONTENT_SKIP
from ddhbVillager import ddhbVillager


# 狩人
class ddhbBodyguard(ddhbVillager):
    """ddhb bodyguard agent."""

    to_be_guarded: Agent # 護衛先
    """Target of guard."""
    
    co_date: int # COする日にち
    has_co: bool # COしたか
    gj_cnt: int # 護衛成功(GJ)回数
    guard_success: bool # 護衛成功したか
    guard_success_agent: Agent # 護衛成功したエージェント
    guard_success_agents: List[Agent] # 護衛成功したエージェントのリスト
    has_report: bool # 報告したかどうか
    strategies: List[bool] # 戦略フラグのリスト


    def __init__(self) -> None:
        """Initialize a new instance of ddhbBodyguard."""
        super().__init__()
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 0
        self.has_co = False
        self.guard_success = False
        self.guard_success_agents = []
        self.has_report = False
        self.strategies = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 4
        self.has_co = False
        self.gj_cnt = 0
        self.guard_success = False
        self.guard_success_agent = AGENT_NONE
        self.guard_success_agents.clear()
        self.has_report = False
        self.strategies = [False, False, False, False, False, True]
        self.strategyA = self.strategies[0] # 戦略A: 護衛スコア
        self.strategyB = self.strategies[1] # 戦略B: 占い重視
        self.strategyC = self.strategies[2] # 戦略C: 候補者から選ぶ
        self.strategyD = self.strategies[3] # 戦略D: COする日にちの変更
        self.strategyE = self.strategies[4] # 戦略E: (CO予定日-1)日目からの護衛成功でCO
        self.strategyF = self.strategies[5] # 戦略F: (CO予定日-1)日目からの2GJ成功でCO
        
        # 戦略D: 3日目CO
        if self.strategyD:
            self.co_date = 3


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        
        self.guard_success = False
        self.has_report = False
        # 処刑で死亡している場合
        if self.guard_success_agent != AGENT_NONE and not self.is_alive(self.guard_success_agent):
            self.guard_success_agent = AGENT_NONE
        
        # Util.debug_print("guarded: ", self.game_info.guarded_agent)
        # 護衛が成功した場合
        if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            self.gj_cnt += 1
            self.guard_success = True
            self.guard_success_agents.append(self.game_info.guarded_agent)
            Util.debug_print("護衛成功:\tエージェント" + str(self.game_info.guarded_agent.agent_idx) + "を護衛しました")
            self.score_matrix.my_guarded(self.game_info, self.game_setting, self.game_info.guarded_agent)
        elif self.game_info.guarded_agent != None:
            self.guard_success_agent = AGENT_NONE
            Util.debug_print("護衛失敗:\tエージェント" + str(self.game_info.last_dead_agent_list[0].agent_idx) + "が死亡しました")


    # CO、報告→OK
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.vote_candidate = self.vote()
        # ---------- CO ----------
        # 戦略E: (CO予定日-1)目からの護衛成功でCO
        if self.strategyE:
            if not self.has_co and (day >= self.co_date - 1 and self.guard_success):
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        # 戦略F: (CO予定日-1)目からの2GJ成功でCO
        if self.strategyF:
            if not self.has_co and (day >= self.co_date - 1 and self.gj_cnt >= 2):
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        # 絶対にCOする→1,2
        # 1: 予定の日にち or 2: 自分が処刑対象になりそうだったら
        if not self.has_co and (day == self.co_date or self.is_Low_HP()):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        # ---------- 護衛報告 ----------
        # COしてて、報告してないなら
        if self.has_co and not self.has_report and day >= 2:
            self.has_report = True
            return Content(GuardedAgentContentBuilder(self.game_info.guarded_agent))
        # ---------- 投票宣言 ----------
        # ----- ESTIMATE, VOTE, REQUEST -----
        if 2 <= turn <= 7:
            rnd = random.randint(0, 2)
            if rnd == 0:
                return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
            elif rnd == 1:
                return Content(VoteContentBuilder(self.vote_candidate))
            else:
                return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
        else:
            return CONTENT_SKIP


    # 投票対象
    def vote(self) -> Agent:
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # 護衛成功したエージェントは除外
        for guard_success_agent in self.guard_success_agents:
            if guard_success_agent in vote_candidates:
                vote_candidates.remove(guard_success_agent)
        # 投票候補：偽占い
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.agent in vote_candidates and j.target == self.me and j.result == Species.WEREWOLF]
        if fake_seers:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, fake_seers)
        else:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me


    # 護衛スコアの高いエージェント
    # 護衛スコア＝村人スコア＋占い師スコア*3＋霊媒師スコア＋勝率
    def get_guard_agent(self, agent_list: List[Agent]) -> Agent:
        p = self.role_predictor.prob_all
        mx_score = 0
        ret_agent = AGENT_NONE
        for agent in agent_list:
            score = p[agent][Role.VILLAGER] + 3 * p[agent][Role.SEER] + p[agent][Role.MEDIUM]
            score += Util.win_rate[agent]
            if score > mx_score:
                mx_score = score
                ret_agent = agent
        return ret_agent


    # 護衛先選び→OK
    def guard(self) -> Agent:
        day: int = self.game_info.day
        # 基本的には連続護衛
        if self.guard_success_agent != AGENT_NONE:
            self.to_be_guarded = self.guard_success_agent
        else:
            # 護衛先候補
            guard_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
            fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.target == self.me and j.result == Species.WEREWOLF]
            # 偽占いは除外
            for fake_seer in fake_seers:
                if fake_seer in guard_candidates:
                    guard_candidates.remove(fake_seer)
            others_seer_co: List[Agent] = [a for a in self.comingout_map if a in guard_candidates and self.comingout_map[a] == Role.SEER]
            others_medium_co: List[Agent] = [a for a in self.comingout_map if a in guard_candidates and self.comingout_map[a] == Role.MEDIUM]
            seer_co_cnt: int = len(others_seer_co)
            medium_co_cnt: int = len(others_medium_co)
            # 占いCO：0人
            if seer_co_cnt == 0:
                if medium_co_cnt >= 1:
                    self.to_be_guarded = self.role_predictor.chooseMostLikely(Role.MEDIUM, others_medium_co)
                else:
                    self.to_be_guarded = self.get_guard_agent(guard_candidates)
            # 占いCO：1人
            elif seer_co_cnt == 1:
                self.to_be_guarded = others_seer_co[0]
                if self.role_predictor.getMostLikelyRole(self.to_be_guarded) == Role.WEREWOLF:
                    if medium_co_cnt == 1:
                        self.to_be_guarded = others_medium_co[0]
                    else:
                        self.to_be_guarded = self.get_guard_agent(guard_candidates)
                    Util.debug_print("護衛先変更:", others_seer_co[0], "→", self.to_be_guarded)
            # 占いCO：2人以上
            # todo: (2,1)盤面でも占い護衛の方がいいかも
            else:
                if day == 1:
                    self.to_be_guarded = self.role_predictor.chooseMostLikely(Role.SEER, others_seer_co)
                else:
                    if medium_co_cnt == 1:
                        self.to_be_guarded = others_medium_co[0]
                    else:
                        self.to_be_guarded = self.get_guard_agent(guard_candidates)

            alive_comingout_map = {a.agent_idx: r.value for a, r in self.comingout_map.items() if self.is_alive(a)}
            Util.debug_print("alive_comingout_map:\t", alive_comingout_map)
            Util.debug_print(f"seer:{seer_co_cnt}, medium:{medium_co_cnt}")
        Util.debug_print("to_be_guarded:\t", self.to_be_guarded)
        
        self.guard_success_agent = self.to_be_guarded
        return self.to_be_guarded if self.to_be_guarded != AGENT_NONE else self.me
