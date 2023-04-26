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
from aiwolf.constant import AGENT_NONE

from ddhbVillager import ddhbVillager

# 狩人
class ddhbBodyguard(ddhbVillager):
    """ddhb bodyguard agent."""

    
    to_be_guarded: Agent # 護衛先
    """Target of guard."""

    def __init__(self) -> None:
        """Initialize a new instance of ddhbBodyguard."""
        super().__init__()
        self.to_be_guarded = AGENT_NONE

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.to_be_guarded = AGENT_NONE

    def day_start(self) -> None:
        super().day_start()

        print("guarded: ", self.game_info.guarded_agent)

        # 護衛が成功した場合
        if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            print("護衛成功: エージェント" + str(self.game_info.guarded_agent.agent_idx) + "を護衛しました")
            self.score_matrix.my_guarded(self.game_info, self.game_setting, self.game_info.guarded_agent)
        elif self.game_info.guarded_agent != None:
            print("護衛失敗: エージェント" + str(self.game_info.last_dead_agent_list[0].agent_idx) + "が死亡しました")

    # 護衛先選び → 変更する
    def guard(self) -> Agent:
        # Guard one of the alive non-fake seers.
        # 候補：白結果あり
        candidates: List[Agent] = self.get_alive([j.agent for j in self.divination_reports
                                                  if j.result != Species.WEREWOLF or j.target != self.me])
        # Guard one of the alive mediums if there are no candidates.
        # 候補なし → 生存する霊媒COの人
        if not candidates:
            candidates = [a for a in self.comingout_map if self.is_alive(a)
                          and self.comingout_map[a] == Role.MEDIUM]
        # Guard one of the alive sagents if there are no candidates.
        # 候補なし → 生存者
        if not candidates:
            candidates = self.get_alive_others(self.game_info.agent_list)
        # Update a guard candidate if the candidate is changed.
        # 護衛先 → 候補からランダムセレクト
        if self.to_be_guarded == AGENT_NONE or self.to_be_guarded not in candidates:
            self.to_be_guarded = self.random_select(candidates)

        self.to_be_guarded = self.random_select(self.get_alive_others(self.game_info.agent_list))
        return self.to_be_guarded if self.to_be_guarded != AGENT_NONE else self.me
    
    # talk追加はありかも→白圧迫
