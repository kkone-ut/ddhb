#
# possessed.py
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
from typing import Deque, List

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, GameInfo, GameSetting,
                    IdentContentBuilder, Judge, Role, Species,
                    VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP, JUDGE_EMPTY
from ddhbVillager import ddhbVillager
from RolePredictor import RolePredictor

# 裏切り者
class ddhbPossessed(ddhbVillager):
    """ddhb possessed agent."""

    fake_role: Role # 騙る役職
    """Fake role."""
    co_date: int # COする日にち
    """Scheduled comingout date."""
    has_co: bool # COしたか
    """Whether or not comingout has done."""
    my_judgee_queue: Deque[Judge] # 自身の（占い or 霊媒）結果キュー
    """Queue of fake judgements."""
    not_judged_agents: List[Agent] # 占っていないエージェント
    """Agents that have not been judged."""
    num_wolves: int # 人狼数
    """The number of werewolves."""
    werewolves: List[Agent] # 人狼結果のエージェント
    """Fake werewolves."""

    def __init__(self) -> None:
        """Initialize a new instance of ddhbPossessed."""
        super().__init__()
        self.fake_role = Role.SEER
        self.co_date = 0
        self.has_co = False
        self.my_judgee_queue = deque()
        self.not_judged_agents = []
        self.num_wolves = 0
        self.werewolves = []

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.N = game_setting.player_num

        # 5人なら占い師、15人なら65%占い師、35%霊媒師を騙る
        if self.N == 5:
            self.fake_role = Role.SEER
        elif self.N == 15:
            self.fake_role = Role.SEER if random.random() < 0.65 else Role.MEDIUM

        self.co_date = 1 # 最低でも1日目にCO → 変更する
        self.has_co = False
        self.my_judgee_queue.clear()
        self.not_judged_agents = self.get_others(self.game_info.agent_list)
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()
        self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)

    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        """Generate a fake judgement."""
        target: Agent = AGENT_NONE
        # 対象の決定
        # 占い騙り → ランダムセレクト
        if self.fake_role == Role.SEER:  # Fake seer chooses a target randomly.
            if self.game_info.day != 0:
                target = self.random_select(self.get_alive(self.not_judged_agents))
        # 霊媒騙り → 死者
        elif self.fake_role == Role.MEDIUM:
            target = self.game_info.executed_agent \
                if self.game_info.executed_agent is not None \
                else AGENT_NONE
        if target == AGENT_NONE:
            return JUDGE_EMPTY
        # Determine a fake result.
        # If the number of werewolves found is less than the total number of werewolves,
        # judge as a werewolf with a probability of 0.5.
        # 騙り結果 → 変更する
        # 発見人狼数が人狼総数より少ない and 確率1/2 で黒結果
        result: Species = Species.WEREWOLF \
            if len(self.werewolves) < self.num_wolves and random.random() < 0.5 \
            else Species.HUMAN
        return Judge(self.me, self.game_info.day, target, result)

    def day_start(self) -> None:
        super().day_start()
        # Process the fake judgement.
        # 昼に騙り結果
        judge: Judge = self.get_fake_judge()
        if judge != JUDGE_EMPTY:
            self.my_judgee_queue.append(judge)
            # 占い対象を、占っていないエージェントリストから除く
            if judge.target in self.not_judged_agents:
                self.not_judged_agents.remove(judge.target)
            # 人狼発見 → 人狼結果リストに追加
            if judge.result == Species.WEREWOLF:
                self.werewolves.append(judge.target)

    def vote(self) -> Agent:
        max_score = -1
        agent_vote_for : Agent = AGENT_NONE

        #  狂人の場合：生存するエージェントが3人以下だったら、一番人狼っぽくない人に投票
        if(len(self.get_alive(self.game_info.agent_list)) <= 3) :
            for i in range(self.N):
                if i != self.me and self.is_alive(self.game_info.agent_list[i]) :
                    score = 1 - self.role_predictor.getProb(i, Role.WEREWOLF)
                    if score > max_score :
                        max_score = score
                        agent_vote_for = self.game_info.agent_list[i]
        else :
            # 狂人の場合：5人村だったら、一番人狼っぽくない人に投票
            if(self.N == 5) :
                for i in range(self.N):
                    if i != self.me and self.is_alive(self.game_info.agent_list[i]) :
                        # 元のコードでは、自分の役職を占い師としたときの確率から、狂人としたときの確率を引いている
                        score = 1 - self.role_predictor.getProb(i, Role.WEREWOLF)
                        if score > max_score :
                            max_score = score
                            agent_vote_for = self.game_info.agent_list[i]
                        break
            # 狂人の場合：15人村だったら、一番人狼っぽい人に投票
            else :
                for i in range(self.N):
                    if i != self.me and self.is_alive(self.game_info.agent_list[i]) :
                        score = self.role_predictor.getProb(i, Role.WEREWOLF)
                        if score > max_score :
                            max_score = score
                            agent_vote_for = self.game_info.agent_list[i]

        return agent_vote_for
    
    
    # CO、結果報告
    def talk(self) -> Content:
        # もし占い師を語るならば
        if self.fake_role == Role.SEER:
            # 占い師が何人いるかを数える
            num_seer = 0
            for i in range(self.N):
                if i != self.me and self.is_alive(self.game_info.agent_list[i]) \
                        and self.comingout_map[self.game_info.agent_list[i]] == Role.SEER:
                    num_seer += 1
            # 占い師が既に二人以上いるならば、狩人を騙る
            if num_seer >= 2:
                self.fake_role = Role.BODYGUARD

        # Do comingout if it's on scheduled day or a werewolf is found.
        # CO : 予定の日にち or 人狼発見
        if self.fake_role != Role.VILLAGER and not self.has_co \
                and (self.game_info.day == self.co_date or self.werewolves):
            self.has_co = True
            return Content(ComingoutContentBuilder(self.me, self.fake_role))
        
        # Report the judgement after doing comingout.
        # 結果報告
        if self.has_co and self.my_judgee_queue:
            # popleftで大丈夫だろうか
            judge: Judge = self.my_judgee_queue.popleft()
            # 5人村の時
            if self.N == 5:
                # 対抗がいたら、対抗が黒だという
                for i in range(self.N):
                    if i != self.me and self.is_alive(self.game_info.agent_list[i]):
                        if self.comingout_map[self.game_info.agent_list[i]] == Role.SEER:
                            return Content(DivinedResultContentBuilder(self.my_judgee_queue[0].target, Species.WEREWOLF))
                    
                if self.fake_role == Role.SEER:
                    return Content(DivinedResultContentBuilder(judge.target, judge.result))
                elif self.fake_role == Role.MEDIUM:
                    return Content(IdentContentBuilder(judge.target, judge.result))
            
        # Vote for one of the alive fake werewolves.
        # 投票候補：人狼結果リスト
        candidates: List[Agent] = self.get_alive(self.werewolves)
        # Vote for one of the alive agent that declared itself the same role of Possessed
        # if there are no candidates.
        # 候補なし → 対抗
        if not candidates:
            candidates = self.get_alive([a for a in self.comingout_map
                                         if self.comingout_map[a] == self.fake_role])
        # Vite for one of the alive agents if there are no candidates.
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
