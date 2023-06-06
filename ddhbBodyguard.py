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
    
    # 追加
    co_date: int # COする日にち
    has_co: bool # COしたか
    guard_success: bool # 護衛成功したか
    has_report: bool # 報告したかどうか
    

    def __init__(self) -> None:
        """Initialize a new instance of ddhbBodyguard."""
        super().__init__()
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 0
        self.has_co = False
        self.guard_success = False
        self.has_report = False

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.to_be_guarded = AGENT_NONE
        
        self.co_date = 4
        self.has_co = False
        self.guard_success = False
        self.has_co_date = False

    def day_start(self) -> None:
        super().day_start()

        Util.debug_print("guarded: ", self.game_info.guarded_agent)

        # 護衛が成功した場合
        if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            print("護衛成功: エージェント" + str(self.game_info.guarded_agent.agent_idx) + "を護衛しました")
            self.score_matrix.my_guarded(self.game_info, self.game_setting, self.game_info.guarded_agent)
        elif self.game_info.guarded_agent != None:
            print("護衛失敗: エージェント" + str(self.game_info.last_dead_agent_list[0].agent_idx) + "が死亡しました")

    # 護衛先選び → 変更する
    def guard(self) -> Agent:
        # 護衛スコアを参考に、護衛先を選ぶ
        p = self.role_predictor.getProbAll()
        mx_score = 0
        guard_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        for agent in guard_candidates:
            score = p[agent][Role.VILLAGER] + p[agent][Role.SEER] * 3 + p[agent][Role.MEDIUM]
            if score > mx_score:
                mx_score = score
                self.to_be_guarded = agent
        
        # # Guard one of the alive non-fake seers.
        # # 候補：白結果あり
        # candidates: List[Agent] = self.get_alive([j.agent for j in self.divination_reports
        #                                           if j.result != Species.WEREWOLF or j.target != self.me])
        # # Guard one of the alive mediums if there are no candidates.
        # # 候補なし → 生存する霊媒COの人
        # if not candidates:
        #     candidates = [a for a in self.comingout_map if self.is_alive(a)
        #                   and self.comingout_map[a] == Role.MEDIUM]
        # # Guard one of the alive sagents if there are no candidates.
        # # 候補なし → 生存者
        # if not candidates:
        #     candidates = self.get_alive_others(self.game_info.agent_list)
        
        # Update a guard candidate if the candidate is changed.
        # 護衛先 → 候補からランダムセレクト
        if self.to_be_guarded == AGENT_NONE or self.to_be_guarded not in guard_candidates:
            self.to_be_guarded = self.random_select(guard_candidates)

        return self.to_be_guarded if self.to_be_guarded != AGENT_NONE else self.me
    
    # talk追加はありかも→白圧迫
    # CO、結果報告
    def talk(self) -> Content:
        # CO : 予定の日にちになったら
        # 後で追加：疑われ出したらCOする
        if not self.has_co and (self.game_info.day == self.co_date):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        
        # 護衛成功報告
        # 予定の日にち-1 以降の日にちなら、まずCOして、次のターンに報告
        if self.game_info.day == self.co_date - 1:
            if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
                self.guard_success = True
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.BODYGUARD))
        
        # COしてて、護衛成功してて、報告してないなら
        if self.has_co and self.guard_success and not self.has_report:
            self.has_report = True
            return Content(GuardedAgentContentBuilder(self.me, self.game_info.guarded_agent))
        
            
        return CONTENT_SKIP
