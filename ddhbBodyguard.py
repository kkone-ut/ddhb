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

from typing import List

from aiwolf import Agent, GameInfo, GameSetting, Role, Species
from aiwolf import ComingoutContentBuilder, Content, GuardedAgentContentBuilder # 追加：狩もCOする
from aiwolf.constant import AGENT_NONE

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
    guard_success: bool # 護衛成功したか
    has_report: bool # 報告したかどうか
    strategies: List[bool] # 戦略フラグのリスト


    def __init__(self) -> None:
        """Initialize a new instance of ddhbBodyguard."""
        super().__init__()
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 0
        self.has_co = False
        self.guard_success = False
        self.has_report = False
        self.strategies = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 4
        self.has_co = False
        self.guard_success = False
        self.has_report = False
        self.strategies = [True, False, False, False, False]
        self.strategyA = self.strategies[0] # 戦略A: 護衛スコア
        self.strategyB = self.strategies[1] # 戦略B: 占い重視
        self.strategyC = self.strategies[2] # 戦略C: 候補者から選ぶ
        self.strategyD = self.strategies[3] # 戦略D: COする日にちの変更
        self.strategyE = self.strategies[4] # 戦略E: (CO予定日-1)日目からの護衛成功でCO


    # 昼スタート→OK
    def day_start(self) -> None:
        super().day_start()
        
        self.guard_success = False
        self.has_report = False
        
        Util.debug_print("guarded: ", self.game_info.guarded_agent)
        # 護衛が成功した場合
        if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            self.guard_success = True
            Util.debug_print("護衛成功:\tエージェント" + str(self.game_info.guarded_agent.agent_idx) + "を護衛しました")
            self.score_matrix.my_guarded(self.game_info, self.game_setting, self.game_info.guarded_agent)
        elif self.game_info.guarded_agent != None:
            Util.debug_print("護衛失敗:\tエージェント" + str(self.game_info.last_dead_agent_list[0].agent_idx) + "が死亡しました")


    # CO、報告→OK
    def talk(self) -> Content:
        # ---------- CO ----------
        # 戦略D: 3日目CO
        if self.strategyD:
            self.strategyD = False
            self.co_date = 3
        # 戦略E: (CO予定日-1)目からの護衛成功でCO
        if self.strategyE:
            self.strategyE = False
            if not self.has_co and (self.game_info.day >= self.co_date - 1 and self.guard_success):
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        # 絶対にCOする→1,2
        # 1: 予定の日にち
        if not self.has_co and (self.game_info.day == self.co_date):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        # 2: 前日投票の25%以上が自分に入っていたら
        vote_num = 0
        latest_vote_list = self.game_info.latest_vote_list
        for vote in latest_vote_list:
            if vote.target == self.me:
                vote_num += 1
        if not self.has_co and len(latest_vote_list) != 0 and vote_num/len(latest_vote_list) >= 0.25:
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        
        # ---------- 護衛報告 ----------
        # COしてて、報告してないなら
        if self.has_co and not self.has_report:
            self.has_report = True
            # review: GuardedAgentContentBuilder の引数に自分のエージェントは要らないので削除した
            return Content(GuardedAgentContentBuilder(self.game_info.guarded_agent))
        
        return CONTENT_SKIP


    # 護衛先選び→OK
    def guard(self) -> Agent:
        # 護衛先候補：生存者
        candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # 戦略A：護衛スコア重視
        # 護衛スコア＝村人スコア＋占い師スコア*3＋霊媒師スコア
        # todo：勝率で補正する
        if self.strategyA:
            guard_candidates: List[Agent] = candidates
            p = self.role_predictor.getProbAll()
            mx_score = 0
            for agent in guard_candidates:
                score = p[agent][Role.VILLAGER] + p[agent][Role.SEER] * 3 + p[agent][Role.MEDIUM]
                if score > mx_score:
                    mx_score = score
                    self.to_be_guarded = agent
        # 戦略B：占い重視（複数なら占い師っぽい方）
        if self.strategyB:
            # 護衛先候補：占いCOかつ生存者
            guard_candidates: List[Agent] = [a for a in self.comingout_map if self.is_alive(a)
                            and self.comingout_map[a] == Role.SEER]
            if not guard_candidates:
                guard_candidates = candidates
            # 護衛先：占い師っぽいエージェント
            self.to_be_guarded = self.role_predictor.chooseMostLikely(Role.SEER, guard_candidates)
        # 戦略C：候補者から選ぶ
        if self.strategyC:        
            # Guard one of the alive non-fake seers.
            # 護衛先候補：白結果あり
            guard_candidates: List[Agent] = self.get_alive([j.agent for j in self.divination_reports
                                                    if j.result != Species.WEREWOLF or j.target != self.me])
            # Guard one of the alive mediums if there are no candidates.
            # 候補なし → 霊媒COかつ生存者
            if not guard_candidates:
                guard_candidates = [a for a in self.comingout_map if self.is_alive(a)
                            and self.comingout_map[a] == Role.MEDIUM]
            # Guard one of the alive sagents if there are no candidates.
            # 候補なし → 生存者
            if not guard_candidates:
                guard_candidates = candidates            
            # Update a guard candidate if the candidate is changed.
            # 護衛先：候補からランダムセレクト
            if self.to_be_guarded == AGENT_NONE or self.to_be_guarded not in guard_candidates:
                self.to_be_guarded = self.random_select(guard_candidates)
        
        return self.to_be_guarded if self.to_be_guarded != AGENT_NONE else self.me
