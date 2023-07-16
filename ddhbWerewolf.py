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
                    DivinedResultContentBuilder, IdentContentBuilder, GuardedAgentContentBuilder, VoteContentBuilder,
                    RequestContentBuilder, EstimateContentBuilder)
from aiwolf.constant import AGENT_NONE, AGENT_ANY

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
    has_PP: bool # PP宣言したか
    kakoi: bool # 囲いフラグ
    not_judged_humans: List[Agent] # 占っていない村陣営
    others_seer_co: List[Agent] # 他の占い師のCOリスト
    found_me: bool # 自分が見つかったかどうか
    agent_seer: Agent # 占い師
    alive_seer: bool # 確定占い師の生存フラグ
    guard_success: bool # 護衛成功したか
    new_target: Agent # 偽の占い対象
    new_result: Species # 偽の占い結果
    
    whisper_turn: int = 0 # 内通ターン


    def __init__(self) -> None:
        """Initialize a new instance of ddhbWerewolf."""
        super().__init__()
        self.allies = []
        self.humans = []
        self.attack_vote_candidate = AGENT_NONE
        
        self.taikou = []
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.agent_seer = AGENT_NONE
        self.alive_seer = False
        self.has_co = False
        self.PP_flag = False
        self.has_PP = False
        self.kakoi = False
        self.not_judged_humans = []
        self.others_seer_co = []
        self.guard_success = False


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # ---------- 5人村15人村共通 ----------
        self.allies = list(self.game_info.role_map.keys())
        self.humans = [a for a in self.game_info.agent_list if a not in self.allies]
        allies_no = [a.agent_idx for a in self.allies]
        humans_no = [a.agent_idx for a in self.humans]
        Util.debug_print("仲間:\t", allies_no)
        Util.debug_print("村陣営:\t", humans_no)
        self.taikou.clear()
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.agent_seer = AGENT_NONE
        self.alive_seer = False
        
        self.has_co = False
        self.my_judge_queue.clear()
        self.not_judged_agents.clear()
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()
        self.has_report = False
        self.black_count = 0
        self.PP_flag = False
        self.has_PP = False
        self.not_judged_humans = self.humans.copy()
        self.others_seer_co.clear()
        self.guard_success = False
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        self.whisper_turn = 0
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
            # 騙り役職：55%村人、35%占い師、10%霊媒師、0%狩人→割合調整する
            fake_roles = [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
            weights = [0.2, 0.6, 0.1, 0.1]
            self.fake_role = np.random.choice(fake_roles, p=weights)
            Util.debug_print(f"騙り役職:\t{self.fake_role}")
            # COする日にち：1~2日目
            # self.co_date = random.randint(1, 2)
            self.co_date = 1
            self.kakoi = True
        
        self.strategies = [False, False, True, False, False]
        self.strategyA = self.strategies[0] # 戦略A: 占い重視
        self.strategyB = self.strategies[1] # 戦略B: 霊媒重視
        self.strategyC = self.strategies[2] # 戦略C: 狩人重視


    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        """Generate a fake judgement."""
        # Determine the target of the fake judgement.
        # 対象候補：生存村人
        judge_candidates: List[Agent] = self.get_alive_others(self.not_judged_humans)
        # 対象：最も村人っぽいエージェント
        # judge_candidate: Agent = self.role_predictor.chooseMostLikely(Role.VILLAGER, judge_candidates)
        # 対象：勝率の高いエージェント
        judge_candidate: Agent = Util.get_strong_agent(judge_candidates)
        result: Species = Species.HUMAN
        
        # ---------- 5人村 ----------
        if self.N == 5:
            # 基本は黒結果
            result = Species.WEREWOLF
        # ---------- 15人村 ----------
        elif self.N == 15:
            # ----- 占い騙り -----
            if self.fake_role == Role.SEER:
                if self.kakoi:
                    self.kakoi = False
                    # 占い候補：人狼仲間
                    judge_candidates = self.get_alive_others(self.allies)
                    # 囲い先：勝率の高い方
                    judge_candidate = Util.get_strong_agent(judge_candidates)
                    # judge_candidate = self.random_select(judge_candidates)
                    result = Species.HUMAN
                else:
                    # 結果：発見人狼数が人狼総数より少ない and 30% で黒結果 and 黒判定<3回 で黒結果
                    # todo: 勝率の高いエージェントに白結果、勝率の低いエージェントに黒結果を出すのはありかも
                    if len(self.werewolves) < self.num_wolves and random.random() < 0.3 and self.black_count < 3:
                        judge_candidate = Util.get_weak_agent(judge_candidates)
                        result = Species.WEREWOLF
                        self.black_count += 1
                    if judge_candidate in self.not_judged_agents:
                        self.not_judged_agents.remove(judge_candidate)
            # ----- 霊媒騙り -----
            elif self.fake_role == Role.MEDIUM:
                judge_candidate = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
                # 結果：村陣営 and 発見人狼数が人狼総数より少ない and 20% and 黒判定<2回 で黒結果
                if judge_candidate in self.humans and len(self.werewolves) < self.num_wolves \
                    and random.random() < 0.2 and self.black_count < 2:
                    result = Species.WEREWOLF
                    self.black_count += 1
        if judge_candidate == AGENT_NONE:
            return JUDGE_EMPTY
        return Judge(self.me, self.game_info.day, judge_candidate, result)


    # 結果から狂人推定
    # 基本的にScoreMatrixに任せる
    # 狂人＝人狼に白結果、村陣営に黒結果→このつもりだったが、真占いが村人に黒結果を出す場合もあるため不採用
    def estimate_possessed(self) -> None:
        self.agent_possessed = self.role_predictor.chooseMostLikely(Role.POSSESSED, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.alive_possessed = False
        if self.agent_possessed != AGENT_NONE:
            self.alive_possessed = self.is_alive(self.agent_possessed)
        # PP：3人以下かつ確定狂人生存
        self.PP_flag = False
        alive_cnt: int = len(self.get_alive(self.game_info.agent_list))
        if alive_cnt <= 3 and self.alive_possessed:
            self.PP_flag = True
        if self.alive_possessed and self.talk_turn >= 12:
            Util.debug_print(f"狂人推定:\t{self.agent_possessed}\t 生存:\t{self.alive_possessed}")


    # 結果から真占い推定
    # 基本的にScoreMatrixに任せる
    # todo: self.meだけではなくて、仲間の人狼に黒結果を出している場合も考慮する
    def estimate_seer(self) -> None:
        # self.agent_seer = self.role_predictor.chooseMostLikely(Role.SEER, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.agent_seer = AGENT_NONE
        self.found_me = False
        for judge in self.divination_reports:
            agent = judge.agent
            target = judge.target
            result = judge.result
            # 狂人の誤爆は考えないことにする
            # if agent == self.agent_seer and target == self.me and result == Species.WEREWOLF:
            # if target == self.me and result == Species.WEREWOLF:
            if target in self.allies and result == Species.WEREWOLF:
                self.agent_seer = agent
                if target == self.me:
                    self.found_me = True
                break
        if self.agent_seer != AGENT_NONE:
            self.alive_seer = self.is_alive(self.agent_seer)
        if self.alive_seer and self.talk_turn >= 12:
            Util.debug_print(f"真占い推定:\t{self.agent_seer}\t 生存:\t{self.alive_seer}")


    # 昼スタート
    def day_start(self) -> None:
        super().day_start()
        day: int = self.game_info.day
        self.attack_vote_candidate = AGENT_NONE
        self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, self.get_alive_others(self.game_info.agent_list))
        self.new_result = Species.WEREWOLF
        self.whisper_turn = 0
        self.estimate_possessed()
        self.estimate_seer()
        # 騙り結果
        if day >= 1:
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
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.estimate_possessed()
        self.estimate_seer()
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        others_seer_co_num = len(self.others_seer_co)
        self.vote_candidate = self.vote()
        # ---------- PP ----------
        if self.PP_flag and not self.has_PP:
            self.has_PP = True
            Util.debug_print(f"狂人推定:\t{self.agent_possessed}\t 生存:\t{self.alive_possessed}")
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
        # ---------- 5人村 ----------
        if self.N == 5:
            if day == 1:
                # ----- CO -----
                # 1: 真占いの黒結果
                if not self.has_co and self.found_me:
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # 2: 占い2COかつ狂人あり
                if not self.has_co and (others_seer_co_num >= 2 and self.alive_possessed):
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # 3: 3ターン目以降かつ占い1CO
                if not self.has_co and (turn >= 3 and others_seer_co_num == 1):
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # ----- 結果報告 -----
                # todo: 一時的にturn<=5にしている
                if self.has_co and self.my_judge_queue and turn <= 5:
                    judge: Judge = self.my_judge_queue.popleft()
                    # 基本は get_fake_judge を利用する
                    # 最も占いっぽいエージェントに黒結果
                    if self.alive_seer:
                        self.new_target = self.agent_seer
                        self.new_result = judge.result
                    if self.new_target == AGENT_NONE:
                        self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, self.get_alive_others(self.game_info.agent_list))
                    if self.new_target == AGENT_NONE:
                        self.new_target = judge.target
                    return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
            elif day == 2:
                # PP盤面でない場合、適当に白結果を出して、占いっぽく見せる
                # if self.has_co and turn == 1:
                if turn == 1:
                    # 勝率の高いエージェントに白結果
                    self.new_target = Util.get_strong_agent(self.get_alive_others(self.game_info.agent_list))
                    self.new_result = Species.HUMAN
                    return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
            # ----- VOTE and REQUEST -----
            if 2<= turn <= 9:
                if turn % 2 == 0:
                    return Content(VoteContentBuilder(self.new_target))
                else:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 人狼仲間のCO状況を確認して、COするかを決める
            allies_co: List[Agent] = [a for a in self.comingout_map if a in self.allies]
            # Util.debug_print("仲間CO数:\t", len(allies_co))
            if len(allies_co) >= 1 and not self.has_co:
                self.fake_role = Role.VILLAGER
            # ---------- 占い騙り ----------
            if self.fake_role == Role.SEER:
                # ----- CO -----
                # 1: 予定の日にち or 2: 人狼発見
                if not self.has_co and (day == self.co_date or self.werewolves):
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
                # 1: 予定の日にち or 2: 人狼発見
                if not self.has_co and (day == self.co_date or self.werewolves):
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
                # 処刑されそうになったらCO
                if not self.has_co and self.is_Low_HP():
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, self.fake_role))
                # ----- 結果報告 -----
                # 人狼仲間を護衛
                if self.has_co and not self.has_report and self.guard_success:
                    self.has_report = True
                    guard_agent = self.random_select(self.get_alive_others(self.allies))
                    return Content(GuardedAgentContentBuilder(guard_agent))
            # ---------- 村人騙り ----------
            else:
                # ----- CO -----
                # 処刑されそうになったら占いCO
                if not self.has_co and self.is_Low_HP():
                    self.has_co = True
                    self.fake_role = Role.BODYGUARD
                    return Content(ComingoutContentBuilder(self.me, self.fake_role))
            # ----- ESTIMATE, VOTE, REQUEST -----
            if 2 <= turn <= 6:
                rnd = random.randint(0, 2)
                if rnd == 0:
                    return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
                elif rnd == 1:
                    return Content(VoteContentBuilder(self.vote_candidate))
                else:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
            else:
                return CONTENT_SKIP
        return CONTENT_SKIP


    # 投票対象
    def vote(self) -> Agent:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.estimate_possessed()
        self.estimate_seer()
        vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        # 確定狂人がいたら除外
        if self.agent_possessed in vote_candidates:
            vote_candidates.remove(self.agent_possessed)
        vote_candidates_no = [a.agent_idx for a in vote_candidates]
        # Util.debug_print(f"投票候補:\t{vote_candidates_no}")
        # ---------- 5人村15人村共通 ----------
        if self.PP_flag:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # ---------- 5人村 ----------
        if self.N == 5:
            latest_vote_list = self.game_info.latest_vote_list
            if day == 1 and latest_vote_list:
                self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF, mostlikely=False)
                return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
            # 確定狂人がいる場合→狂人の結果に合わせる
            if self.alive_possessed:
                # breakしないことで、最新の狂人の結果を反映する
                for judge in self.divination_reports:
                    agent = judge.agent
                    target = judge.target
                    result = judge.result
                    if agent == self.agent_possessed:
                        # 自分への白結果の場合：自分の黒先→村人っぽいエージェント
                        if result == Species.HUMAN:
                            if self.new_target != AGENT_NONE:
                                self.vote_candidate = self.new_target
                            else:
                                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
                        # 自分以外への黒結果の場合：狂人の黒先
                        elif result == Species.WEREWOLF:
                            if self.is_alive(target):
                                self.vote_candidate = target
            else:
                # 自分の黒先→村人っぽいエージェント
                # todo: 最も処刑されそうなエージェント：自分が死ぬよりはマシ
                if self.new_target != AGENT_NONE:
                    self.vote_candidate = self.new_target
                else:
                    self.vote_candidate = self.chooseMostlikelyExecuted()
                    # vote_candidates = self.get_alive_others(self.others_seer_co)
                    # if not vote_candidates:
                    #     vote_candidates = self.get_alive_others(self.humans)
                    # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 投票候補の優先順位
            # 仲間の投票先→自分の黒先→占い→処刑されそうなエージェント
            allies_will_vote_reports = [target for agent, target in self.will_vote_reports.items() if agent in self.allies]
            allies_will_vote_reports_num = [target.agent_idx for agent, target in self.will_vote_reports.items() if agent in self.allies]
            
            humans_seer_co = [a for a in self.comingout_map if a in vote_candidates and  self.comingout_map[a] == Role.SEER]
            if allies_will_vote_reports:
                self.vote_candidate = self.chooseMostlikelyExecuted_2(include_list=allies_will_vote_reports)
                if turn >= 12:
                    Util.debug_print("仲間の投票先:\t", allies_will_vote_reports_num)
                    Util.debug_print("仲間の投票先投票:\t", self.vote_candidate.agent_idx)
            elif self.get_alive_others(self.werewolves):
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, self.get_alive_others(self.werewolves))
            # 初日に真偽がわからない占いに投票するのはおかしいから、2日目以降にする
            elif humans_seer_co and day >= 2:
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, humans_seer_co)
            else:
                self.vote_candidate = self.chooseMostlikelyExecuted(exclude_list=self.allies)
                if turn >= 12:
                    Util.debug_print("処刑されそうなエージェント投票:\t", self.vote_candidate.agent_idx)
        if self.vote_candidate == AGENT_NONE:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me


    # 内通
    # 注意：15人村かつ人狼が複数生きている場合のみ呼ばれる
    def whisper(self) -> Content:
        self.whisper_turn += 1
        day: int = self.game_info.day
        turn: int = self.whisper_turn
        self.estimate_possessed()
        self.estimate_seer()
        # ----- 襲撃対象 -----
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        attack_vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        # 確定狂人は除外
        if self.agent_possessed in attack_vote_candidates:
            attack_vote_candidates.remove(self.agent_possessed)
        # 襲撃候補から護衛成功したエージェントを除外
        if self.game_info.attacked_agent in attack_vote_candidates:
            attack_vote_candidates.remove(self.game_info.attacked_agent)
        
        if day == 0:
            # ----- 騙り役職宣言 -----
            if turn == 1:
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            elif turn == 2:
                self.attack_vote_candidate = Util.get_strong_agent(attack_vote_candidates)
                Util.debug_print(f"whisper襲撃対象:\t{self.attack_vote_candidate}")
                return Content(AttackContentBuilder(self.attack_vote_candidate))
            else:
                return CONTENT_SKIP
        
        # 襲撃候補の優先順位
        # 狩人→占い→霊媒→護衛スコア
        others_bodygurad_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.BODYGUARD]
        others_medium_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.MEDIUM]
        if others_bodygurad_co:
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, others_bodygurad_co)
        # 確定占い師がいて、護衛成功していない場合
        elif self.alive_seer and not self.guard_success:
            self.attack_vote_candidate = self.agent_seer
        elif others_medium_co and not self.guard_success:
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, others_medium_co)
        
        # # 戦略A: 占い重視（占い師っぽい方）
        # if self.strategyA:
        #     attack_vote_candidates = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.SEER]
        #     self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, attack_vote_candidates)
        # # 戦略B: 霊媒重視（霊媒っぽい方）
        # if self.strategyB:
        #     attack_vote_candidates = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.MEDIUM]
        #     self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, attack_vote_candidates)
        # # 戦略C: 狩人重視（狩人っぽい方）
        # if self.strategyC:
        #     candidates = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.BODYGUARD]
        #     self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, candidates)
        
        # 候補なし → 襲撃スコア = スコア + 勝率
        if self.attack_vote_candidate == AGENT_NONE:
            p = self.role_predictor.prob_all
            mx_score = 0
            for agent in attack_vote_candidates:
                score = p[agent][Role.VILLAGER] + p[agent][Role.SEER]*4 + p[agent][Role.MEDIUM]*3 + p[agent][Role.BODYGUARD]*2
                score += 3 * Util.win_rate[agent]
                if score > mx_score:
                    mx_score = score
                    self.attack_vote_candidate = agent
        if turn <= 2 and self.attack_vote_candidate != AGENT_NONE:
            return Content(AttackContentBuilder(self.attack_vote_candidate))
        elif turn == 3 and self.alive_possessed:
            return Content(EstimateContentBuilder(self.agent_possessed, Role.POSSESSED))
        elif turn == 4 and self.alive_seer:
            return Content(EstimateContentBuilder(self.agent_seer, Role.SEER))
        return CONTENT_SKIP


    # 襲撃→OK
    def attack(self) -> Agent:
        self.estimate_possessed()
        self.estimate_seer()
        alive_comingout_map = {a.agent_idx: r.value for a, r in self.comingout_map.items() if self.is_alive(a)}
        Util.debug_print("alive_comingout_map:\t", alive_comingout_map)
        # ----- 襲撃対象 -----
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        attack_vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        # 確定狂人は除外
        if self.agent_possessed in attack_vote_candidates:
            attack_vote_candidates.remove(self.agent_possessed)
        # ---------- 5人村 ----------
        # 注意：5人村ではwhisperが呼ばれないので、attack関数で襲撃対象を決める
        if self.N == 5:
            # 襲撃候補：占いCOしていないエージェント
            for seer_candidate in self.others_seer_co:
                if seer_candidate in attack_vote_candidates:
                    attack_vote_candidates.remove(seer_candidate)
            # 候補なし → 生存村人
            if not attack_vote_candidates:
                attack_vote_candidates = self.get_alive_others(self.humans)
            # 対象：最も村人っぽいエージェント＋勝率を考慮する
            self.attack_vote_candidate = self.role_predictor.chooseStrongLikely(Role.VILLAGER, attack_vote_candidates, coef=0.5)
            # self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, attack_vote_candidates)
        # ---------- 15人村 ----------
        # 注意：15人村でも人狼が1人になったらwhisperが呼ばれないので、attack関数で襲撃対象を決める：whisperのコピペ
        elif self.N == 15:
            # 襲撃候補の優先順位
            # 狩人→占い→霊媒→護衛スコア
            others_bodygurad_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.BODYGUARD]
            others_medium_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.MEDIUM]
            role_attack = False
            if others_bodygurad_co:
                Util.debug_print('狩人襲撃')
                role_attack = True
                self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, others_bodygurad_co)
            # 確定占い師がいて、護衛成功していない場合
            elif self.alive_seer and not self.guard_success:
                Util.debug_print('占い襲撃')
                role_attack = True
                self.attack_vote_candidate = self.agent_seer
            elif others_medium_co and not self.guard_success:
                Util.debug_print('霊媒襲撃')
                role_attack = True
                self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, others_medium_co)
            # 候補なし → 襲撃スコア = スコア + 勝率
            # if self.attack_vote_candidate == AGENT_NONE:
            if not role_attack:
                p = self.role_predictor.prob_all
                mx_score = 0
                for agent in attack_vote_candidates:
                    score = p[agent][Role.VILLAGER] + p[agent][Role.SEER]*4 + p[agent][Role.MEDIUM]*3 + p[agent][Role.BODYGUARD]*2
                    score += 3 * Util.win_rate[agent]
                    if score > mx_score:
                        mx_score = score
                        self.attack_vote_candidate = agent
                attack_vote_candidates_no = [a.agent_idx for a in attack_vote_candidates]
                Util.debug_print("襲撃スコア:\t:", attack_vote_candidates_no, self.attack_vote_candidate.agent_idx, mx_score)
        Util.debug_print("襲撃候補:\t", [a.agent_idx for a in attack_vote_candidates])
        Util.debug_print(f"襲撃対象:\t{self.attack_vote_candidate}")
        return self.attack_vote_candidate if self.attack_vote_candidate != AGENT_NONE else self.me
