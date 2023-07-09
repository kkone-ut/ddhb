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
from Util import Util

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
    others_seer_co: List[Agent] # 他の占い師のCOリスト
    found_me: bool # 自分が見つかったかどうか
    agent_seer: Agent # 占い師
    guard_success: bool # 護衛成功したか


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
        self.others_seer_co = []
        self.guard_success = False


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # ---------- 5人村15人村共通 ----------
        self.allies = list(self.game_info.role_map.keys())
        self.humans = [a for a in self.game_info.agent_list if a not in self.allies]
        self.taikou.clear()
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.PP_flag = False
        self.not_judged_humans = self.humans.copy()
        self.others_seer_co.clear()
        self.guard_success = False
        # ---------- 5人村 ----------
        if self.N == 5:
            # # 騙り役職：90%村人、10%占い師
            # self.fake_role = Role.VILLAGER if random.random() < 0.9 else Role.SEER
            self.fake_role = Role.SEER
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
        # 対象候補：生存者
        divine_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # 対象
        target: Agent = AGENT_NONE
        # 結果：基本は白結果
        result: Species = Species.HUMAN
        # ---------- 占い騙り ----------
        if self.fake_role == Role.SEER:
            if self.game_info.day != 0:
                # ----- 5人村 -----
                # 占い対象：村人っぽい人（狂人を対象にしたくないから）
                # 結果：基本は黒結果
                if self.N == 5:
                    target = self.role_predictor.chooseMostLikely(Role.VILLAGER, divine_candidates)
                    result = Species.WEREWOLF
                # ----- 15人村 -----
                elif self.N == 15:
                    if self.kakoi:
                        self.kakoi = False
                        # 占い候補：人狼仲間
                        divine_candidates = self.get_alive_others(self.allies)
                        # todo: HPが少ない方を囲う
                        target = self.random_select(divine_candidates)
                        result = Species.HUMAN
                    else:
                        # 占い候補：占ってない村陣営
                        divine_candidates = self.get_alive(self.not_judged_humans)
                        # 占い対象：村人っぽい人
                        target = self.role_predictor.chooseMostLikely(Role.VILLAGER, divine_candidates)
                        # 結果：発見人狼数が人狼総数より少ない and 30% で黒結果
                        if len(self.werewolves) < self.num_wolves and random.random() < 0.3:
                            result = Species.WEREWOLF
                            self.black_count += 1
        # ---------- 霊媒騙り ----------
        elif self.fake_role == Role.MEDIUM:
            target = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
            # 結果：村陣営 and 発見人狼数が人狼総数より少ない and 20% and 黒判定<2回 で黒結果
            if target in self.humans and len(self.werewolves) < self.num_wolves \
                and random.random() < 0.2 and self.black_count < 2:
                result = Species.WEREWOLF
                self.black_count += 1
        
        if target == AGENT_NONE:
            return JUDGE_EMPTY
        return Judge(self.me, self.game_info.day, target, result)


    # 結果から狂人推定
    # 基本的にScoreMatrixに任せる
    # 狂人＝人狼に白結果、村陣営に黒結果→このつもりだったが、真占いが村人に黒結果を出す場合もあるため不採用
    def estimate_possessed(self) -> None:
        # for judge in self.divination_reports + self.identification_reports:
        #     agent = judge.agent
        #     target = judge.target
        #     result = judge.result
        #     # 人狼仲間の結果は無視
        #     if agent in self.allies:
        #         continue
        #     if target in self.allies and result == Species.HUMAN:
        #         self.agent_possessed = agent
        #     if target in self.humans and result == Species.WEREWOLF:
        #         self.agent_possessed = agent
        self.agent_possessed = self.role_predictor.chooseMostLikely(Role.POSSESSED, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.alive_possessed = False
        if self.agent_possessed != AGENT_NONE:
            self.alive_possessed = self.is_alive(self.agent_possessed)
        Util.debug_print(f"狂人推定：{self.agent_possessed} 生存：{self.alive_possessed}")


    # 結果から真占い推定
    # 基本的にScoreMatrixに任せる
    def estimate_seer(self) -> None:
        self.agent_seer = self.role_predictor.chooseMostLikely(Role.SEER, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.found_me = False
        for judge in self.divination_reports:
            agent = judge.agent
            target = judge.target
            result = judge.result
            if agent == self.agent_seer and target == self.me and result == Species.WEREWOLF:
                self.found_me = True
                break


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
        # 襲撃失敗（護衛成功）
        if self.game_info.attacked_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            self.guard_success = True
        # 襲撃成功（護衛失敗）
        if self.game_info.attacked_agent != None and len(self.game_info.last_dead_agent_list) == 1:
            self.guard_success = False


    # CO、結果報告
    def talk(self) -> Content:
        self.estimate_possessed()
        self.estimate_seer()
        # ---------- PP ----------
        if self.PP_flag:
            self.PP_flag = False
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
        
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        
        # ---------- 5人村 ----------
        if self.N == 5:
            # ----- CO -----
            # 1: 真占いの黒結果
            if not self.has_co and self.found_me:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # 2: 占い2COかつ狂人あり
            if not self.has_co and len(self.others_seer_co) >= 2 and self.alive_possessed:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # 3: 2ターン目以降で占い1CO
            if not self.has_co and self.talk_turn >= 2 and len(self.others_seer_co) == 1:
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- 結果報告 -----
            if self.has_co and self.my_judge_queue:
                judge: Judge = self.my_judge_queue.popleft()
                # 基本は get_fake_judge を利用する
                new_target = self.agent_seer
                if new_target == AGENT_NONE:
                    new_target = self.role_predictor.chooseMostLikely(Role.SEER, self.get_alive_others(self.game_info.agent_list))
                if new_target != AGENT_NONE:
                    new_target = judge.target
                return Content(DivinedResultContentBuilder(new_target, judge.result))
        
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 人狼仲間のCO状況を確認する
            # 仲間が1人以上COしていたら、村人を騙る
            # 発言タイミングがわからないから、基本はランダムにして、仲間とCOが被る場合だけ避ける            
            # todo: 狂人を含めたCO数で判定する→やめる
            # review: max(0, 占いCO数-1) + max(0, 霊能CO数-1) + max(0, 狩人CO数-1) とか使えそう？
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
            if self.is_Low_HP():
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- 結果報告 -----
            if self.has_co and self.has_report:
                self.has_report = True
                guard_agent = self.random_select(self.get_alive(self.allies))
                return Content(GuardedAgentContentBuilder(guard_agent))

        # ---------- 投票宣言 ----------
        # self.vote()の利用
        if self.talk_turn >= 2 and self.vote_candidate == AGENT_NONE:
            self.vote_candidate = self.vote()
            return Content(VoteContentBuilder(self.vote_candidate))
        return CONTENT_SKIP


    # 投票対象
    def vote(self) -> Agent:
        self.estimate_possessed()
        self.estimate_seer()
        vote_candidate: Agent = AGENT_NONE
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
                # 5人村だから占い結果のみ考慮する
                for judge in self.divination_reports:
                    agent = judge.agent
                    target = judge.target
                    result = judge.result
                    # 5人村前提でのコードになっているから、しっかり盤面を追わないと理解できないコードになっている
                    if agent == self.agent_possessed:
                        # 確定狂人がいて、自分への白結果なら、村陣営からランダムセレクト
                        if result == Species.HUMAN:
                            vote_candidate = self.random_select(vote_candidates)
                        # 確定狂人がいて、自分以外への黒結果なら、そのエージェントへ投票
                        elif result == Species.WEREWOLF:
                            vote_candidate = target if self.is_alive(target) else self.random_select(vote_candidates)
                        break
            else:
                vote_candidate = self.random_select(vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            vote_candidate = self.random_select(vote_candidates)
        
        return vote_candidate if vote_candidate != AGENT_NONE else self.me


    # 内通
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
        # 襲撃候補から護衛成功したエージェントを除外
        if self.game_info.attacked_agent in candidates:
            candidates.remove(self.game_info.attacked_agent)
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
