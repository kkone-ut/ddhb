#
# seer.py
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

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, GameInfo, GameSetting, Judge,
                    Role, Species, VoteContentBuilder,
                    RequestContentBuilder, EstimateContentBuilder)
from aiwolf.constant import AGENT_NONE, AGENT_ANY

from const import CONTENT_SKIP
from ddhbVillager import ddhbVillager

from Util import Util


# 占い
class ddhbSeer(ddhbVillager):
    """ddhb seer agent."""

    co_date: int # COする日にち
    """Scheduled comingout date."""
    has_co: bool # COしたか
    """Whether or not comingout has done."""
    my_judge_queue: Deque[Judge] # 自身の占い結果キュー
    """Queue of divination results."""
    not_divined_agents: List[Agent] # 占っていないエージェント
    """Agents that have not been divined."""
    werewolves: List[Agent] # 人狼結果のエージェント
    """Found werewolves."""
    
    strategies: List[bool] # 戦略フラグのリスト
    others_seer_co: List[Agent] # 他の占い師のCOリスト
    new_target: Agent # 偽の占い対象
    new_result: Species # 偽の占い結果


    def __init__(self) -> None:
        """Initialize a new instance of ddhbSeer."""
        super().__init__()
        self.co_date = 0
        self.has_co = False
        self.my_judge_queue = deque()
        self.not_divined_agents = []
        self.werewolves = []
        
        self.strategies = []
        self.others_seer_co = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 3 # 最低でも3日目にCO
        self.has_co = False
        self.my_judge_queue.clear()
        self.not_divined_agents = self.get_others(self.game_info.agent_list)
        self.werewolves.clear()
        
        self.strategies = [True, False, False, False, False]
        self.strategyA = self.strategies[0] # 戦略A: COする日にちの変更（初日CO）
        self.strategyB = self.strategies[1] # 戦略B:
        
        self.others_seer_co.clear()
        
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        
        # 戦略A: 初日CO
        if self.strategyA:
            self.co_date = 1
        # ---------- 5人村 ----------
        if self.N == 5:
            # 初日CO
            self.co_date = 1


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        # Process a divination result.
        # 占い結果
        judge: Optional[Judge] = self.game_info.divine_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            # 占い対象を、占っていないエージェントリストから除く
            if judge.target in self.not_divined_agents:
                self.not_divined_agents.remove(judge.target)
            if judge.result == Species.WEREWOLF: # 黒結果
                self.werewolves.append(judge.target) # 人狼リストに追加
            # スコアの更新
            self.score_matrix.my_divined(self.game_info, self.game_setting, judge.target, judge.result)


    # CO、結果報告、投票宣言→OK
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        others_co_num: int = len(self.others_seer_co)
        self.vote_candidate = self.vote()
        # ---------- 5人村 ----------
        if self.N == 5:
            if day == 1:
                # ----- CO -----
                if turn == 1:
                    if not self.has_co:
                        self.has_co = True
                        return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # ----- 結果報告 -----
                elif turn == 2:
                    if self.has_co and self.my_judge_queue:
                        judge: Judge = self.my_judge_queue.popleft()
                        self.new_target = judge.target
                        self.new_result = judge.result
                        # 黒結果→そのまま報告
                        if judge.result == Species.WEREWOLF:
                            return Content(DivinedResultContentBuilder(judge.target, judge.result))
                        # 白結果→状況に応じて報告
                        elif judge.result == Species.HUMAN:
                            self.new_result = Species.WEREWOLF
                            if others_co_num == 0:
                                # todo: 勝率を反映する
                                self.role_predictor.chooseStrongLikely(Role.WEREWOLF, self.get_alive_others(self.not_divined_agents), coef=0.5)
                                # self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.not_divined_agents)
                            else:
                                self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.others_seer_co)
                            if self.new_target == AGENT_NONE:
                                self.new_target = judge.target
                                self.new_result = judge.result
                            return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # ----- VOTE and REQUEST -----
                elif 3 <= turn <= 9:
                    if turn % 2 == 0:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                    else:
                        return Content(VoteContentBuilder(self.new_target))
                else:
                    return CONTENT_SKIP
            elif day >= 2:
                # ----- 結果報告 -----
                if turn == 1:
                    if self.has_co and self.my_judge_queue:
                        judge: Judge = self.my_judge_queue.popleft()
                        self.new_target = judge.target
                        self.new_result = judge.result
                        # 黒結果→そのまま報告
                        if judge.result == Species.WEREWOLF:
                            return Content(DivinedResultContentBuilder(judge.target, judge.result))
                        # 白結果→生存者3人だから、残りの1人に黒結果（結果としては等価）
                        # 注意：占い先が噛まれた場合は等価ではない→人狼っぽい方に黒結果
                        elif judge.result == Species.HUMAN:
                            # self.new_target = self.random_select(self.get_alive_others(self.not_divined_agents))
                            self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.get_alive_others(self.not_divined_agents))
                            self.new_result = Species.WEREWOLF
                            return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # 狂人が生きている場合→人狼CO
                elif turn == 2 and self.role_predictor.estimate_alive_possessed(threshold=0.5):
                    return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
                # ----- VOTE and REQUEST -----
                elif 2 <= turn <= 9:
                    if turn % 2 == 0:
                        return Content(VoteContentBuilder(self.new_target))
                    else:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                else:
                    return CONTENT_SKIP
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            # ---------- CO ----------
            # 絶対にCOする→1,2,3
            # 1: 予定の日にち
            if not self.has_co and day == self.co_date:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER))
            # 2: 人狼発見
            if not self.has_co and self.werewolves:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER))
            # 3: 他の占い師がCOしたら(CCO)
            if not self.has_co and self.others_seer_co:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER)) 
            # ---------- 結果報告 ----------
            # Report the divination result after doing comingout.
            if self.has_co and self.my_judge_queue:
                judge: Judge = self.my_judge_queue.popleft()
                # 正しい結果を報告する
                return Content(DivinedResultContentBuilder(judge.target, judge.result))
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
        else:
            return CONTENT_SKIP


    # 投票対象→OK
    def vote(self) -> Agent:
        day: int = self.game_info.day
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        # ---------- 5人村 ----------
        if self.N == 5:
            latest_vote_list = self.game_info.latest_vote_list
            if day == 1 and latest_vote_list:
                self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF)
                return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
            # 投票対象：自分の黒先→人狼っぽいエージェント
            if self.get_alive_others(self.werewolves):
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.get_alive_others(self.werewolves))
            elif self.new_target != AGENT_NONE:
                self.vote_candidate = self.new_target
            else:
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            latest_vote_list = self.game_info.latest_vote_list
            if latest_vote_list:
                self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF)
            # 投票候補：人狼結果リスト
            vote_candidates = self.get_alive_others(self.werewolves)
            # 候補なし → 偽占い
            if not vote_candidates:
                vote_candidates = self.get_alive_others(self.others_seer_co)
            # 候補なし → 生存者
            if not vote_candidates:
                vote_candidates = self.get_alive_others(self.game_info.agent_list)
            # 投票対象：人狼っぽいエージェント
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me


    # 占い対象→OK
    def divine(self) -> Agent:
        day: int = self.game_info.day
        divine_candidate: Agent = AGENT_NONE
        # 占い候補：占っていないエージェント
        divine_candidates: List[Agent] = self.get_alive_others(self.not_divined_agents)
        # todo: 人狼確率と勝率を組み合わせる
        divine_candidate = self.role_predictor.chooseStrongLikely(Role.WEREWOLF, divine_candidates, coef=0.5)
        # divine_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, divine_candidates)
        # ---------- 5人村15人村共通 ----------
        if day == 0:
            divine_candidate = Util.get_strong_agent(divine_candidates)
        return divine_candidate if divine_candidate != AGENT_NONE else self.me
