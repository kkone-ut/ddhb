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

from collections import deque
from typing import Deque, List, Optional

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, GameInfo, GameSetting, Judge,
                    Role, Species, VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

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


    def __init__(self) -> None:
        """Initialize a new instance of ddhbSeer."""
        super().__init__()
        self.co_date = 0
        self.has_co = False
        self.my_judge_queue = deque()
        self.not_divined_agents = []
        self.werewolves = []
        
        self.strategies = []


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
        
        # ---------- 5人村 ----------
        if self.N == 5:
            # 初日CO
            self.co_date = 1


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
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
        # ---------- CO ----------
        # Do comingout if it's on scheduled day or a werewolf is found.
        # 戦略A: 初日CO
        # review: initialize で行う
        if self.strategyA:
            self.strategyA = False
            self.co_date = 1
        # 絶対にCOする→1,2,3
        # 1: 予定の日にち
        if not self.has_co and self.game_info.day == self.co_date:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.SEER))
        # 2: 人狼発見
        if not self.has_co and self.werewolves:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.SEER))
        # 3: 他の占い師がCOしたら(CCO)
        # 注意：comingout_mapには、自分は含まれていない
        others_seer_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        if not self.has_co and others_seer_co:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.SEER))
        
        # ---------- 結果報告 ----------
        # Report the divination result after doing comingout.
        if self.has_co and self.my_judge_queue:
            judge: Judge = self.my_judge_queue.popleft()
            return Content(DivinedResultContentBuilder(judge.target, judge.result))
        
        # ---------- 投票宣言 ----------
        # Vote for one of the alive werewolves.
        # 投票候補：人狼結果リストかつ生存者
        candidates: List[Agent] = self.get_alive(self.werewolves)
        # Vote for one of the alive fake seers if there are no candidates.
        # 候補なし → 偽占い
        if not candidates:
            candidates = self.get_alive(others_seer_co)
        # Vote for one of the alive agents if there are no candidates.
        # 候補なし → 生存者
        if not candidates:
            candidates = self.get_alive_others(self.game_info.agent_list)
        # Declare which to vote for if not declare yet or the candidate is changed.
        # 投票宣言対象：候補からランダムセレクト
        # todo: 人狼っぽいエージェントを選ぶ→計算時間は大丈夫か？
        if self.vote_candidate == AGENT_NONE or self.vote_candidate not in candidates:
            self.vote_candidate = self.random_select(candidates)
            if self.vote_candidate != AGENT_NONE:
                Util.debug_print('vote_candidate: ' + str(self.vote_candidate))
                return Content(VoteContentBuilder(self.vote_candidate))
        return CONTENT_SKIP


    # 投票対象→OK
    def vote(self) -> Agent:
        # 投票候補：人狼結果リストかつ生存者
        vote_candidates: List[Agent] = self.get_alive(self.werewolves)
        # 候補なし → 生存者
        # 偽占いとしないのは、偽占いに投票しても投票が集まらないと思われるため
        if not vote_candidates:
            vote_candidates = self.get_alive_others(self.game_info.agent_list)
        # 投票対象：人狼っぽいエージェント
        target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return target if target != AGENT_NONE else self.me


    # 占い対象→OK
    def divine(self) -> Agent:
        # Divine a agent randomly chosen from undivined agents.
        target: Agent = self.random_select(self.not_divined_agents)
        # 占い候補：占っていないかつ生存者
        divine_candidates: List[Agent] = self.get_alive(self.not_divined_agents)
        # 占い対象：最も人狼っぽいエージェント
        target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, divine_candidates)
        return target if target != AGENT_NONE else self.me
