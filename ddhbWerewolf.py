#
# werewolf.py
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
from typing import List, Deque

from aiwolf import (Agent, AttackContentBuilder, ComingoutContentBuilder,
                    Content, GameInfo, GameSetting, Judge, Role, Species,
                    DivinedResultContentBuilder, IdentContentBuilder, GuardedAgentContentBuilder, VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP, JUDGE_EMPTY
from ddhbPossessed import ddhbPossessed

import numpy as np


# 人狼
# todo: Basketを参考にする
class ddhbWerewolf(ddhbPossessed):
    """ddhb werewolf agent."""

    allies: List[Agent] # 仲間の人狼
    """Allies."""
    humans: List[Agent] # 村陣営
    """Humans."""
    attack_vote_candidate: Agent # 襲撃対象
    """The candidate for the attack voting."""
    
    taikou: List[Agent] # 対抗リスト
    agent_possessed: Agent # 狂人
    alive_possessed: bool # 確定狂人の生存フラグ
    PP_flag: bool # PPフラグ
    kakoi: bool # 囲いフラグ
    not_judged_humans: List[Agent] # 占っていない村陣営


    def __init__(self) -> None:
        """Initialize a new instance of ddhbWerewolf."""
        super().__init__()
        self.allies = []
        self.humans = []
        self.attack_vote_candidate = AGENT_NONE
        
        self.taikou = []
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.PP_flag = False
        self.kakoi = False
        self.not_judged_humans = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # ---------- 5人村15人村共通 ----------
        self.humans = [a for a in self.game_info.agent_list if a not in self.allies]
        self.allies = list(self.game_info.role_map.keys())
        self.taikou.clear()
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.PP_flag = False
        self.not_judged_humans = self.humans.copy()
        # ---------- 5人村 ----------
        if self.N == 5:
            # 騙り役職：90%村人、10%占い師
            self.fake_role = Role.VILLAGER if random.random() < 0.9 else Role.SEER
            # 初日CO
            self.co_date = 1
            self.kakoi = False
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 騙り役職：55%村人、35%占い師、10%霊媒師、0%狩人
            fake_roles = [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
            weights = [0.55, 0.35, 0.10, 0.0]
            self.fake_role = np.random.choice(fake_roles, p=weights)
            # COする日にち：1~3日目
            self.co_date = random.randint(1, 3)
            self.kakoi = True
        
        self.strategies = [True, False, False, False, False]
        self.strategyA = self.strategies[0] # 戦略A: 占い重視
        self.strategyB = self.strategies[1] # 戦略B: 霊媒重視
        self.strategyC = self.strategies[2] # 戦略C: 狩人重視


    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        """Generate a fake judgement."""
        # Determine the target of the fake judgement.
        # 対象
        target: Agent = AGENT_NONE
        # 結果：基本は白結果
        result: Species = Species.HUMAN
        # ---------- 占い騙り ----------
        if self.fake_role == Role.SEER:
            if self.game_info.day != 0:
                if self.kakoi:
                    self.kakoi = False
                    # 占い候補：人狼仲間
                    divine_candidates: List[Agent] = self.get_alive_others(self.allies)
                    target = self.random_select(divine_candidates)
                else:
                    # 占い候補：占ってない村陣営
                    divine_candidates: List[Agent] = self.get_alive(self.not_judged_humans)
                    # 占い対象：todo：工夫する
                    target = self.random_select(divine_candidates)
                    # ----- 5人村 -----
                    # 結果：基本は黒結果
                    if self.N == 5:
                        result = Species.WEREWOLF
                    # ----- 15人村 -----
                    # 結果：発見人狼数が人狼総数より少ない and 30% で黒結果
                    elif self.N == 15:
                        if len(self.werewolves) < self.num_wolves and random.random() < 0.3:
                            result = Species.WEREWOLF
                            self.black_count += 1
        # ---------- 霊媒騙り ----------
        elif self.fake_role == Role.MEDIUM:
            target = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
            # 結果：村陣営 and 発見人狼数が人狼総数より少ない and 10% and 黒判定<2回 で黒結果
            if target in self.humans and len(self.werewolves) < self.num_wolves \
                and random.random() < 0.1 and self.black_count < 2:
                result = Species.WEREWOLF
                self.black_count += 1
        
        if target == AGENT_NONE:
            return JUDGE_EMPTY
        return Judge(self.me, self.game_info.day, target, result)


    # 結果から狂人推定
    # todo: どのタイミングで判定するべき？→投票前？だけでいい？
    # 狂人＝人狼に白結果、村陣営に結果
    def estimate_possessed(self) -> None:
        for judge in self.divination_reports + self.identification_reports:
            agent = judge.agent
            target = judge.target
            result = judge.result
            # 人狼仲間の結果は無視
            if agent in self.allies:
                continue
            if target in self.allies and result == Species.HUMAN:
                self.agent_possessed = agent
            if target in self.humans and result == Species.WEREWOLF:
                self.agent_possessed = agent
        self.alive_possessed = False
        if self.agent_possessed != AGENT_NONE:
            self.alive_possessed = self.is_alive(self.agent_possessed)


    # 昼スタート
    def day_start(self) -> None:
        super().day_start()
        self.attack_vote_candidate = AGENT_NONE
        self.estimate_possessed()
        # PP：3人以下かつ確定狂人生存
        alive_cnt: int = len(self.get_alive(self.game_info.agent_list))
        if alive_cnt <= 3 and self.alive_possessed:
            self.PP_flag = True
        # 騙り結果
        judge: Judge = self.get_fake_judge()
        if judge != JUDGE_EMPTY:
            self.my_judge_queue.append(judge)
            if judge.target in self.not_judged_agents:
                self.not_judged_agents.remove(judge.target)
            if judge.target in self.not_judged_humans:
                self.not_judged_humans.remove(judge.target)
            if judge.result == Species.WEREWOLF:
                self.werewolves.append(judge.target)


    # CO、結果報告
    def talk(self) -> Content:
        # 人狼仲間のCO状況を確認する
        # 仲間が1人以上COしていたら、村人を騙る
        allies_co: List[Agent] = [a for a in self.comingout_map if a in self.allies]
        if len(allies_co) >= 1:
            self.fake_role = Role.VILLAGER
        
        # ---------- 占い騙り ----------
        if self.fake_role == Role.SEER:
            # ----- CO -----
            # 1: 予定の日にち
            if not self.has_co and self.game_info.day == self.co_date:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # 2: 人狼発見
            if not self.has_co and self.werewolves:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- 結果報告 -----
            if self.has_co and self.my_judge_queue:
                judge: Judge = self.my_judge_queue.popleft()
                # 基本は get_fake_judge を利用する
                # ----- 5人村 -----
                if self.N == 5:
                    others_seer_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
                    candidates: List[Agent] = self.get_alive(others_seer_co)
                    # 対抗がいたら、対抗に黒結果
                    if candidates:
                        judge.target = self.random_select(others_seer_co)
                        judge.result = Species.WEREWOLF # なくてもOK←5人村では基本は黒結果だから
                    return Content(DivinedResultContentBuilder(judge.target, judge.result))
                # ----- 15人村 -----
                elif self.N == 15:
                    return Content(DivinedResultContentBuilder(judge.target, judge.result))
        # ---------- 霊媒騙り ----------
        elif self.fake_role == Role.MEDIUM:
            # ----- CO -----
            # 1: 予定の日にち
            if not self.has_co and self.game_info.day == self.co_date:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # 2: 人狼発見
            if not self.has_co and self.werewolves:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- 結果報告 -----
            if self.has_co and self.my_judge_queue:
                judge: Judge = self.my_judge_queue.popleft()
                # 基本は get_fake_judge を利用する
                return Content(IdentContentBuilder(judge.target, judge.result))
        # ---------- 狩人騙り ----------
        elif self.fake_role == Role.BODYGUARD:
            # ----- CO -----
            # 前日投票の25%以上が自分に入っていたら
            vote_num = 0
            latest_vote_list = self.game_info.latest_vote_list
            for vote in latest_vote_list:
                if vote.target == self.me:
                    vote_num += 1
            if not self.has_co and len(latest_vote_list) != 0 and vote_num/len(latest_vote_list) >= 0.25:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- 結果報告 -----
            if self.has_co and self.has_report:
                self.has_report = True
                guard_agent = self.random_select(self.get_alive(self.allies))
                return Content(GuardedAgentContentBuilder(guard_agent))
        # ---------- PP ----------
        if self.PP_flag:
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))    
        
        return CONTENT_SKIP


    # 投票対象
    def vote(self) -> Agent:
        self.estimate_possessed()
        # 投票候補：人狼結果
        vote_candidates: List[Agent] = self.get_alive(self.werewolves)
        # 候補なし → 村陣営
        # 基本的にはライン切りしない
        if not vote_candidates:
            vote_candidates = self.get_alive(self.humans)
        # 確定狂人がいたら除外
        if self.alive_possessed and self.agent_possessed in vote_candidates:
            vote_candidates.remove(self.agent_possessed)
        
        # ---------- 5人村15人村共通 ----------
        if self.PP_flag:
            vote_candidates = self.get_alive(self.humans)
            vote_candidates.remove(self.agent_possessed)
            vote_candidate = self.random_select(vote_candidates)
            return vote_candidate
        # ---------- 5人村 ----------
        if self.N == 5:
            # 確定狂人がいる場合
            if self.alive_possessed:
                # 狂人が確定するのは、占い結果のみ
                for judge in self.divination_reports:
                    agent = judge.agent
                    target = judge.target
                    result = judge.result
                    if agent == self.agent_possessed:
                        if result == Species.HUMAN:
                            vote_candidate = self.random_select(vote_candidates)
                        elif result == Species.WEREWOLF:
                            vote_candidate = target
                        break
            else:
                vote_candidate = self.random_select(vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            vote_candidate = self.random_select(vote_candidates)
        
        return vote_candidate if vote_candidate != AGENT_NONE else self.me


    # 内通
    # todo: 護衛成功で噛み先を変える
    def whisper(self) -> Content:
        # ---------- 騙り役職宣言 ----------
        # Declare the fake role on the 1st day,
        # and declare the target of attack vote after that.
        # 初日に宣言
        if self.game_info.day == 0:
            return Content(ComingoutContentBuilder(self.me, self.fake_role))
        
        # ---------- 襲撃対象 ----------
        # Choose the target of attack vote.
        # Vote for one of the agent that did comingout.
        # 襲撃候補
        candidates: List[Agent] = self.get_alive(self.humans)
        # 戦略A: 占い重視（占い師っぽい方）
        if self.strategyA:
            attack_vote_candidates = [a for a in self.comingout_map if a in candidates and self.comingout_map[a] == Role.SEER]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, attack_vote_candidates)
        # 戦略B: 霊媒重視（霊媒っぽい方）
        if self.strategyB:
            attack_vote_candidates = [a for a in self.comingout_map if a in candidates and self.comingout_map[a] == Role.MEDIUM]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, attack_vote_candidates)
        # 戦略C: 狩人重視（狩人っぽい方）
        if self.strategyC:
            attack_vote_candidates = [a for a in self.comingout_map if self.is_alive(a)
                            and a in self.humans and self.comingout_map[a] == Role.BODYGUARD]
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, attack_vote_candidates)
        # 候補なし → 襲撃スコア
        if self.attack_vote_candidate == AGENT_NONE:
            p = self.role_predictor.prob_all
            mx_score = 0
            for agent in candidates:
                score = p[agent][Role.VILLAGER] + p[agent][Role.SEER]*4 + p[agent][Role.MEDIUM]*3 + p[agent][Role.BODYGUARD]*2
                if score > mx_score:
                    mx_score = score
                    self.attack_vote_candidate = agent
        # 候補なし → 生存村人
        if self.attack_vote_candidate == AGENT_NONE:
            self.attack_vote_candidate = self.random_select(candidates)
        if self.attack_vote_candidate != AGENT_NONE:
            return Content(AttackContentBuilder(self.attack_vote_candidate))
        
        return CONTENT_SKIP


    # 襲撃→OK
    def attack(self) -> Agent:
        return self.attack_vote_candidate if self.attack_vote_candidate != AGENT_NONE else self.me
