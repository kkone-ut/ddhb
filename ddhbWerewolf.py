import random
from typing import List, Optional

import numpy as np
from const import CONTENT_SKIP, JUDGE_EMPTY
from Util import Util

from aiwolf import (Agent, AttackContentBuilder, ComingoutContentBuilder,
                    Content, DivinedResultContentBuilder,
                    EstimateContentBuilder, GameInfo, GameSetting,
                    GuardedAgentContentBuilder, IdentContentBuilder, Judge,
                    RequestContentBuilder, Role, Species, VoteContentBuilder)
from aiwolf.constant import AGENT_ANY, AGENT_NONE
from ddhbPossessed import ddhbPossessed


# 人狼
class ddhbWerewolf(ddhbPossessed):
    """ddhb werewolf agent."""

    allies: List[Agent] # 仲間の人狼
    """Allies."""
    humans: List[Agent] # 村陣営
    """Humans."""
    attack_vote_candidate: Agent # 襲撃対象
    """The candidate for the attack voting."""
    agent_possessed: Agent # 確定狂人
    alive_possessed: bool # 確定狂人の生存フラグ
    agent_seer: Agent # 確定占い師
    alive_seer: bool # 確定占い師の生存フラグ
    found_me: bool # 自分が見つかったかどうか
    whisper_turn: int = 0 # 内通ターン
    threat: List[Agent] # 脅威となるエージェント
    # ----- 占い騙り -----
    kakoi: bool # 囲いフラグ
    not_judged_humans: List[Agent] # 占っていない村陣営
    # ----- 狩人騙り -----
    guard_success: bool # 護衛成功したか
    guard_success_agent: Agent # 護衛成功したエージェント

    def __init__(self) -> None:
        """Initialize a new instance of ddhbWerewolf."""
        super().__init__()
        self.allies = []
        self.humans = []
        self.attack_vote_candidate = AGENT_NONE

        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.agent_seer = AGENT_NONE
        self.alive_seer = False
        self.found_me = False
        self.whisper_turn = 0
        self.threat = []
        self.kakoi = False
        self.not_judged_humans = []
        self.guard_success = False
        self.guard_success_agent = AGENT_NONE

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # ---------- 5人村15人村共通 ----------
        self.allies = list(self.game_info.role_map.keys())
        self.humans = [a for a in self.game_info.agent_list if a not in self.allies]
        allies_no = [a.agent_idx for a in self.allies]
        Util.debug_print("仲間:\t", allies_no)
        self.attack_vote_candidate = AGENT_NONE
        self.agent_possessed = AGENT_NONE
        self.alive_possessed = False
        self.agent_seer = AGENT_NONE
        self.alive_seer = False
        self.found_me = False
        self.whisper_turn = 0
        self.threat = []
        self.not_judged_humans = self.humans.copy()
        self.guard_success = False
        self.guard_success_agent = AGENT_NONE
        # ---------- 5人村 ----------
        if self.N == 5:
            self.fake_role = Role.SEER
            # 初日CO
            self.co_date = 1
            self.kakoi = False
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 騙り役職：割合調整
            fake_roles = [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
            weights = [0.6, 0.4, 0.0, 0.0]
            self.fake_role = np.random.choice(fake_roles, p=weights)
            Util.debug_print("騙り役職:\t", self.fake_role)
            # COする日にち：1日目
            self.co_date = 1
            self.kakoi = True

        self.strategies = [False, False, False, False, False]
        self.strategyA = self.strategies[0] # 戦略A: 占い重視
        self.strategyB = self.strategies[1] # 戦略B: 霊媒重視
        self.strategyC = self.strategies[2] # 戦略C: 狩人重視

    # 偽結果生成
    def get_fake_judge(self) -> Judge:
        # 対象候補：生存村人
        judge_candidates: List[Agent] = self.get_alive_others(self.not_judged_humans)
        # 対象：勝率の高いエージェント
        judge_candidate: Agent = Util.get_strong_agent(judge_candidates)
        result: Species = Species.HUMAN
        # ---------- 5人村 ----------
        if self.N == 5:
            judge_candidate = Util.get_strong_agent(judge_candidates)
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
                    result = Species.HUMAN
                else:
                    # 結果：発見人狼数が人狼総数より少ない and 30% で黒結果 で黒結果
                    # 勝率の高いエージェントに白結果、勝率の低いエージェントに黒結果を出す
                    if len(self.werewolves) < self.num_wolves and random.random() < 0.3:
                        judge_candidate = Util.get_weak_agent(judge_candidates)
                        result = Species.WEREWOLF
                    if judge_candidate in self.not_judged_agents:
                        self.not_judged_agents.remove(judge_candidate)
            # ----- 霊媒騙り -----
            elif self.fake_role == Role.MEDIUM:
                judge_candidate = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
                # 結果：村陣営 and 発見人狼数が人狼総数-1より少ない and 30% で黒結果
                if judge_candidate in self.humans and len(self.werewolves) < self.num_wolves and random.random() < 0.3:
                    result = Species.WEREWOLF
        if judge_candidate == AGENT_NONE:
            return JUDGE_EMPTY
        return Judge(self.me, self.game_info.day, judge_candidate, result)

    # 結果から狂人推定
    # 「狂人＝人狼に白結果、村陣営に黒結果」のつもりだったが、真占いが村人に黒結果を出す場合もあるため不採用
    # ScoreMatrixに任せる
    def estimate_possessed(self) -> None:
        th: float = 0.9
        game: int = Util.game_count
        if self.N == 5:
            if game < 10:
                th = 0.9
            elif game < 50:
                th = 0.6
            else:
                th = 0.5
        elif self.N == 15:
            th = 0.5
        # self.agent_possessed = self.role_predictor.chooseMostLikely(Role.POSSESSED, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.agent_possessed, P_prob = self.role_predictor.chooseMostLikely(Role.POSSESSED, self.get_others(self.game_info.agent_list), threshold=th, returns_prob=True)
        Util.debug_print("agent_possessed, P_prob:\t", self.agent_possessed, P_prob)
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
    # 狂人の誤爆は考えないことにする
    def estimate_seer(self) -> None:
        # self.agent_seer = self.role_predictor.chooseMostLikely(Role.SEER, self.get_others(self.game_info.agent_list), threshold=0.9)
        self.agent_seer = AGENT_NONE
        self.found_me = False
        for judge in self.divination_reports:
            agent = judge.agent
            target = judge.target
            result = judge.result
            # if agent == self.agent_seer and target == self.me and result == Species.WEREWOLF:
            if target in self.allies and result == Species.WEREWOLF:
                self.agent_seer = agent
                if target == self.me:
                    self.found_me = True
                break
        if self.agent_seer != AGENT_NONE:
            self.alive_seer = self.is_alive(self.agent_seer)
        if self.alive_seer and self.talk_turn >= 12:
            Util.debug_print(f"真占い推定:\t{self.agent_seer}\t 生存:\t{self.alive_seer}")

    # 確定狂人の占い結果
    def get_possessed_divination(self) -> Judge:
        ret_judge: Optional[Judge] = JUDGE_EMPTY
        # breakしないことで、最新の狂人の結果を反映する
        for judge in self.divination_reports:
            if judge.agent == self.agent_possessed:
                ret_judge = judge
        return ret_judge

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
        if self.game_info.attacked_agent is not None and len(self.game_info.last_dead_agent_list) == 0:
            self.guard_success = True
            self.guard_success_agent = self.game_info.attacked_agent
            Util.debug_print("襲撃失敗：attacked agent:\t", self.game_info.attacked_agent)
        # 襲撃成功（護衛失敗）
        if self.game_info.attacked_agent is not None and len(self.game_info.last_dead_agent_list) == 1:
            self.guard_success = False
            self.guard_success_agent = AGENT_NONE

    # CO、結果報告
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.estimate_possessed()
        self.estimate_seer()
        others_seer_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        others_seer_co_num = len(others_seer_co)
        self.vote_candidate = self.vote()
        # ---------- PP ----------
        if self.PP_flag and not self.has_PP:
            self.has_PP = True
            Util.debug_print(f"狂人推定:\t{self.agent_possessed}\t 生存:\t{self.alive_possessed}")
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
        # ---------- 5人村 ----------
        if self.N == 5:
            if day == 1:
                # 村人と揃える
                if turn == 1:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(ComingoutContentBuilder(AGENT_ANY, Role.ANY))))
                # ----- CO -----
                # 1: 真占いの黒結果
                if not self.has_co and self.found_me:
                    Util.debug_print("占いCO：見つかった")
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # 2: 占い2COかつ狂人あり
                if not self.has_co and (others_seer_co_num >= 2 and self.alive_possessed):
                    Util.debug_print("占いCO：2COかつ狂人あり")
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # 3: 3ターン目以降かつ占い1CO
                if not self.has_co and (turn >= 3 and others_seer_co_num == 1):
                    Util.debug_print("占いCO：3ターン目以降かつ占い1CO")
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # ----- 結果報告 -----
                if self.has_co and self.my_judge_queue:
                    judge: Judge = self.my_judge_queue.popleft()
                    # 基本は get_fake_judge を利用する
                    # 黒結果
                    # 対象：確定占い→狂人に合わせる→占いっぽいエージェント
                    if self.alive_seer:
                        self.new_target = self.agent_seer
                    elif self.alive_possessed:
                        self.new_target = self.vote_candidate
                    else:
                        self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, self.get_alive_others(self.game_info.agent_list))
                    if self.new_target == AGENT_NONE:
                        self.new_target = judge.target
                    return Content(DivinedResultContentBuilder(self.new_target, Species.WEREWOLF))
            elif day == 2:
                # PP盤面でない場合、適当に白結果を出して、占いっぽく見せる
                if turn == 1:
                    # 勝率の低いエージェントに白結果を出して、占いっぽく見せる
                    alive_others: List[Agent] = self.get_alive_others(self.game_info.agent_list)
                    weak_agent: Agent = Util.get_weak_agent(alive_others)
                    if weak_agent in alive_others:
                        alive_others.remove(weak_agent)
                    self.new_target = self.role_predictor.chooseLeastLikely(Role.POSSESSED, alive_others)
                    self.new_result = Species.HUMAN
                    return Content(DivinedResultContentBuilder(weak_agent, Species.HUMAN))
            # ----- VOTE and REQUEST -----
            if 2 <= turn <= 9:
                if self.PP_flag:
                    self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.POSSESSED, self.get_alive_others(self.game_info.agent_list))
                if turn % 2 == 0:
                    return Content(VoteContentBuilder(self.vote_candidate))
                else:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 村人と揃える
            if day == 1 and turn == 1:
                return Content(RequestContentBuilder(AGENT_ANY, Content(ComingoutContentBuilder(AGENT_ANY, Role.ANY))))
            # 人狼仲間のCO状況を確認して、COするかを決める
            allies_co: List[Agent] = [a for a in self.comingout_map if a in self.allies]
            if len(allies_co) >= 1 and not self.has_co and day == 1 and self.fake_role != Role.VILLAGER:
                Util.debug_print("騙り変更")
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
        # ----------  同数投票の処理 ----------
        latest_vote_list = self.game_info.latest_vote_list
        tmp_vote_candidate = self.vote_candidate
        if latest_vote_list:
            Util.debug_print("latest_vote_list:\t", self.vote_to_dict(latest_vote_list))
            # 3人で1:1:1に割れた時、周りが投票を変更しないと仮定すると、絶対に投票を変更するべき
            if len(latest_vote_list) == 3:
                Util.debug_print("------------------------------ 3人で1:1:1 ------------------------------")
                alive_others: List[Agent] = self.get_alive_others(self.game_info.agent_list)
                if self.vote_candidate in alive_others:
                    alive_others.remove(self.vote_candidate)
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
            else:
                # self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF, mostlikely=False)
                self.vote_candidate = self.changeVote(latest_vote_list, Role.POSSESSED, mostlikely=False)
            # 人狼仲間に投票されるのを防ぐ
            if self.vote_candidate in self.allies:
                self.vote_candidate = tmp_vote_candidate
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me

        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.estimate_possessed()
        self.estimate_seer()
        vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
        others_seer_co: List[Agent] = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        # 確定狂人がいたら除外
        if self.agent_possessed in vote_candidates:
            vote_candidates.remove(self.agent_possessed)
        # ---------- 5人村15人村共通 ----------
        if self.PP_flag:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, vote_candidates)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # ---------- 5人村 ----------
        if self.N == 5:
            # 確定狂人がいる場合→狂人の結果に合わせる
            if self.alive_possessed:
                Util.debug_print("alive_possessed")
                possessed_judge: Optional[Judge] = self.get_possessed_divination()
                target = possessed_judge.target
                result = possessed_judge.result
                # 自分への白結果の場合：自分の黒先→処刑されそうなエージェント
                if result == Species.HUMAN:
                    if self.new_target != AGENT_NONE:
                        Util.debug_print("自分の黒先")
                        self.vote_candidate = self.new_target
                    else:
                        Util.debug_print("処刑されそうなエージェント2")
                        self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=vote_candidates, exclude_list=[self.agent_possessed])
                # 自分以外への黒結果の場合：狂人の黒先
                elif result == Species.WEREWOLF:
                    if self.is_alive(target):
                        Util.debug_print("狂人の黒先")
                        self.vote_candidate = target
                    else:
                        Util.debug_print("処刑されそうなエージェント2")
                        self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=vote_candidates, exclude_list=[self.agent_possessed])
            else:
                # 自分の黒先→最も処刑されそうなエージェント（自分が死ぬよりはマシ）
                if self.new_target != AGENT_NONE:
                    self.vote_candidate = self.new_target
                else:
                    Util.debug_print("処刑されそうなエージェント2")
                    self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 投票候補の優先順位：仲間の投票先→自分の黒先→占い→処刑されそうなエージェント
            # * in vote_candidates で、人狼仲間と確定狂人を除く
            allies_will_vote_reports: List[Agent] = [target for agent, target in self.will_vote_reports.items() if agent in self.allies and target in vote_candidates]
            alive_werewolves: List[Agent] = self.get_alive_others(self.werewolves)
            humans_seer_co: List[Agent] = [a for a in self.comingout_map if a in vote_candidates and self.comingout_map[a] == Role.SEER]
            # self.vote_candidate = self.chooseMostlikelyExecuted2(exclude_list=self.allies)
            self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=vote_candidates, exclude_list=self.allies)
            Util.debug_print("処刑されそうなエージェント2:\t", self.vote_candidate.agent_idx)
            # if allies_will_vote_reports:
            #     self.vote_candidate = self.chooseMostlikelyExecuted(include_list=allies_will_vote_reports)
            #     Util.debug_print("仲間の投票先:\t", self.agent_to_index(allies_will_vote_reports))
            #     Util.debug_print("仲間の投票先投票:\t", self.vote_candidate.agent_idx)
            # elif alive_werewolves:
            #     self.vote_candidate = self.chooseMostlikelyExecuted(include_list=alive_werewolves)
            #     Util.debug_print("黒先投票:\t", self.agent_to_index(alive_werewolves))
            # # 初日に真偽がわからない占いに投票するのはおかしいから、2日目以降にする
            # elif humans_seer_co and day >= 2:
            #     self.vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, humans_seer_co)
            #     Util.debug_print("占い先投票:\t", self.agent_to_index(humans_seer_co))
            # else:
            #     self.vote_candidate = self.chooseMostlikelyExecuted(exclude_list=self.allies)
            #     Util.debug_print("処刑されそうなエージェント投票:\t", self.vote_candidate.agent_idx)
        # ----- 投票ミスを防ぐ -----
        if self.vote_candidate == AGENT_NONE or self.vote_candidate in self.allies:
            Util.debug_print("vote_candidates: AGENT_NONE or self.allies")
            self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.POSSESSED, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me

    # 襲撃スコア(=スコア + coef*勝率)の高いエージェント
    def get_attack_agent(self, agent_list: List[Agent], coef: float = 3.0) -> Agent:
        p = self.role_predictor.prob_all
        mx_score = 0
        ret_agent = AGENT_NONE
        for agent in agent_list:
            score = 2 * p[agent][Role.SEER] + p[agent][Role.MEDIUM] + p[agent][Role.BODYGUARD]
            score += 3 * Util.win_rate[agent]
            if score > mx_score:
                mx_score = score
                ret_agent = agent
        Util.debug_print("襲撃スコア:\t:", self.agent_to_index(agent_list), ret_agent.agent_idx, mx_score)
        return ret_agent

    # 内通
    # 注意：15人村かつ人狼が複数生きている場合のみ呼ばれる
    def whisper(self) -> Content:
        self.whisper_turn += 1
        day: int = self.game_info.day
        turn: int = self.whisper_turn
        self.estimate_possessed()
        self.estimate_seer()
        # ----- 襲撃対象 -----
        attack_vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        # 確定狂人は除外
        if self.agent_possessed in attack_vote_candidates:
            attack_vote_candidates.remove(self.agent_possessed)
        # 護衛成功したエージェントを除外
        if self.guard_success_agent in attack_vote_candidates:
            attack_vote_candidates.remove(self.guard_success_agent)
        if day == 0:
            # ----- 騙り役職宣言 -----
            if turn == 1:
                return Content(ComingoutContentBuilder(self.me, self.fake_role))
            elif turn == 2:
                self.attack_vote_candidate = Util.get_strong_agent(attack_vote_candidates)
                Util.debug_print("whisper襲撃対象:\t", self.attack_vote_candidate)
                return Content(AttackContentBuilder(self.attack_vote_candidate))
            else:
                return CONTENT_SKIP
        # 脅威：人狼に投票したエージェント
        latest_vote_list = self.game_info.latest_vote_list
        self.threat = [v.agent for v in latest_vote_list if v.target in self.allies and v.agent in attack_vote_candidates]
        # 襲撃候補の優先順位：狩人→確定占い→占い→霊媒→襲撃スコア
        others_bodygurad_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.BODYGUARD]
        others_seer_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.SEER]
        others_medium_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.MEDIUM]
        if others_bodygurad_co:
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, others_bodygurad_co)
        elif self.alive_seer:
            self.attack_vote_candidate = self.agent_seer
        elif others_seer_co:
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, others_seer_co)
        elif others_medium_co:
            self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, others_medium_co)
        elif self.threat:
            self.attack_vote_candidate = self.get_attack_agent(self.threat)
        else:
            self.attack_vote_candidate = self.get_attack_agent(attack_vote_candidates)
        # ----- 襲撃宣言、狂人推定、占い推定 -----
        if turn <= 2 and self.attack_vote_candidate != AGENT_NONE:
            return Content(AttackContentBuilder(self.attack_vote_candidate))
        elif turn == 3 and self.alive_possessed:
            return Content(EstimateContentBuilder(self.agent_possessed, Role.POSSESSED))
        elif turn == 4 and self.alive_seer:
            return Content(EstimateContentBuilder(self.agent_seer, Role.SEER))
        return CONTENT_SKIP

    # 襲撃
    def attack(self) -> Agent:
        self.estimate_possessed()
        self.estimate_seer()
        alive_werewolf_cnt = len(self.get_alive(self.allies))
        # ----- 襲撃対象 -----
        attack_vote_candidates: List[Agent] = self.get_alive_others(self.humans)
        # 確定狂人は除外
        if self.agent_possessed in attack_vote_candidates:
            attack_vote_candidates.remove(self.agent_possessed)
        # 護衛成功したエージェントを除外
        if self.guard_success_agent in attack_vote_candidates:
            attack_vote_candidates.remove(self.guard_success_agent)
        # 重要：これ以降、襲撃対象に、処刑者・確定狂人・護衛成功者は除きたいから、v.agent in attack_vote_candidates で確認する
        latest_vote_list = self.game_info.latest_vote_list
        Util.debug_print("----- attack -----")
        Util.debug_print("latest_vote_list:\t", self.vote_to_dict(latest_vote_list))
        Util.debug_print("latest_vote_cnt:\t", self.vote_cnt(latest_vote_list))
        # 脅威：人狼に投票したエージェント
        self.threat = [v.agent for v in latest_vote_list if v.target in self.allies and v.agent in attack_vote_candidates]
        Util.debug_print("脅威:\t", self.agent_to_index(self.threat))
        Util.debug_print("alive_comingout_map:\t", self.alive_comingout_map_str)
        # ---------- 5人村 ----------
        # 注意：5人村ではwhisperが呼ばれないので、attack関数で襲撃対象を決める
        if self.N == 5:
            # 襲撃候補：占いCOしていないエージェント
            # if a in attack_vote_candidates でaliveは保証されている
            others_seer_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.SEER]
            for seer_candidate in others_seer_co:
                if seer_candidate in attack_vote_candidates:
                    attack_vote_candidates.remove(seer_candidate)
            if not attack_vote_candidates:
                attack_vote_candidates = self.get_alive_others(self.humans)
            # 脅威噛み
            # 対象：最も村人っぽいエージェント＋勝率を考慮する
            if self.threat:
                self.attack_vote_candidate = self.role_predictor.chooseStrongLikely(Role.VILLAGER, self.threat, coef=3.0)
            else:
                self.attack_vote_candidate = self.role_predictor.chooseStrongLikely(Role.VILLAGER, attack_vote_candidates, coef=3.0)
            # self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, attack_vote_candidates)
            # 狂人っぽい場合、襲撃対象を変更する
            if self.role_predictor.getMostLikelyRole(self.attack_vote_candidate) == Role.POSSESSED:
                self.attack_vote_candidate = self.role_predictor.chooseLeastLikely(Role.POSSESSED, attack_vote_candidates)
        # ---------- 15人村 ----------
        # 注意：15人村でも人狼が1人になったらwhisperが呼ばれないので、attack関数で襲撃対象を決める：whisperのコピペ
        elif self.N == 15:
            # 襲撃候補の優先順位：狩人→確定占い→占い→霊媒→襲撃スコア
            others_bodygurad_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.BODYGUARD]
            others_seer_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.SEER]
            others_medium_co: List[Agent] = [a for a in self.comingout_map if a in attack_vote_candidates and self.comingout_map[a] == Role.MEDIUM]
            # 自分がラストウルフなら、脅威噛みを優先する
            if alive_werewolf_cnt == 1:
                if self.threat:
                    Util.debug_print('脅威噛み')
                    self.attack_vote_candidate = self.get_attack_agent(self.threat)
                else:
                    Util.debug_print('スコア襲撃')
                    self.attack_vote_candidate = self.get_attack_agent(attack_vote_candidates)
            else:
                if others_bodygurad_co:
                    Util.debug_print('狩人襲撃')
                    self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.BODYGUARD, others_bodygurad_co)
                elif self.alive_seer:
                    Util.debug_print('確定占い襲撃')
                    self.attack_vote_candidate = self.agent_seer
                elif others_seer_co:
                    Util.debug_print('占い襲撃')
                    self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.SEER, others_seer_co)
                elif others_medium_co:
                    Util.debug_print('霊媒襲撃')
                    self.attack_vote_candidate = self.role_predictor.chooseMostLikely(Role.MEDIUM, others_medium_co)
                elif self.threat:
                    Util.debug_print('脅威噛み')
                    self.attack_vote_candidate = self.get_attack_agent(self.threat)
                else:
                    Util.debug_print('スコア襲撃')
                    self.attack_vote_candidate = self.get_attack_agent(attack_vote_candidates)
        Util.debug_print(f"襲撃対象:\t{self.attack_vote_candidate}")
        return self.attack_vote_candidate if self.attack_vote_candidate != AGENT_NONE else self.me
