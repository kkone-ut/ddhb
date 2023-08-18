import random
from collections import deque
from typing import Deque, List

from const import CONTENT_SKIP
from ddhbVillager import ddhbVillager
from Util import Util

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, EstimateContentBuilder,
                    GameInfo, GameSetting, GuardContentBuilder,
                    GuardedAgentContentBuilder, IdentContentBuilder, Judge,
                    RequestContentBuilder, Role, Species, VoteContentBuilder)
from aiwolf.constant import AGENT_ANY, AGENT_NONE


# 狂人
class ddhbPossessed(ddhbVillager):
    """ddhb possessed agent."""

    fake_role: Role # 騙る役職
    """Fake role."""
    co_date: int # COする日にち
    """Scheduled comingout date."""
    has_co: bool # COしたか
    """Whether or not comingout has done."""
    my_judge_queue: Deque[Judge] # 自身の（占い or 霊媒）結果キュー
    """Queue of fake judgements."""
    not_judged_agents: List[Agent] # 占っていないエージェント
    """Agents that have not been judged."""
    num_wolves: int # 人狼数
    """The number of werewolves."""
    werewolves: List[Agent] # 人狼結果のエージェント
    """Fake werewolves."""
    PP_flag: bool # PPフラグ
    has_PP: bool # PP宣言したか
    # ----- 騙り共通 -----
    has_report: bool # 結果を報告したか
    black_count: int # 黒判定した数
    # ----- 占い騙り -----
    new_target: Agent # 偽の占い対象
    new_result: Species # 偽の占い結果
    agent_werewolf: Agent # 人狼っぽいエージェント


    def __init__(self) -> None:
        """Initialize a new instance of ddhbPossessed."""
        super().__init__()
        self.fake_role = Role.SEER
        self.co_date = 0
        self.has_co = False
        self.my_judge_queue = deque()
        self.not_judged_agents = []
        self.num_wolves = 0
        self.werewolves = []
        self.PP_flag = False
        self.has_PP = False
        self.has_report = False
        self.black_count = 0 # 霊媒師が黒判定した数
        self.new_target = AGENT_NONE
        self.new_result = Species.UNC
        self.agent_werewolf = AGENT_NONE
        self.strategies = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # ---------- 5人村15人村共通 ----------
        self.co_date = 1
        self.has_co = False
        self.my_judge_queue.clear()
        self.not_judged_agents = self.get_others(self.game_info.agent_list)
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()
        self.PP_flag = False
        self.has_PP = False
        self.has_report = False
        self.black_count = 0
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        self.agent_werewolf = AGENT_NONE
        
        # 自分のロールがPOSSESEDでない時、以下をスキップする
        if self.game_info.my_role != Role.POSSESSED:
            return
        
        # 戦略を検証するためのフラグ
        self.strategies = [False, True, True]
        self.strategyA = self.strategies[0] # 戦略A：一日で何回も占い結果を言う
        self.strategyB = self.strategies[1] # 戦略B：100%で占いCO
        self.strategyC = self.strategies[2] # 戦略C：15人村：COしてから占い結果
        
        # ---------- 5人村 ----------
        if self.N == 5:
            self.fake_role = Role.SEER
        # ---------- 15人村 ----------
        elif self.N == 15:
            # ----- 戦略B：100%で占いCO -----
            if self.strategyB:
                self.fake_role = Role.SEER
            else:
                # 65%の確率で占い師、35%の確率で霊媒師
                self.fake_role = Role.SEER if random.random() < 0.65 else Role.MEDIUM


    # スコアマトリックスから人狼を推測する
    def estimate_werewolf(self) -> None:
        th: float = 0.7
        game: int = Util.game_count
        # ---------- 5人村 ----------
        if self.N == 5:
            if game < 10:
                th = 0.9
            elif game < 50:
                th = 0.7
            else:
                th = 0.4
        # ---------- 15人村 ----------
        elif self.N == 15:
            th = 0.4
        self.agent_werewolf, W_prob = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.get_alive_others(self.game_info.agent_list), threshold=th, returns_prob=True)
        Util.debug_print("agent_werewolf, W_prob:\t", self.agent_werewolf, W_prob)


    def day_start(self) -> None:
        super().day_start()
        # 自分のロールがPOSSESEDでない時、以下をスキップする
        if self.game_info.my_role != Role.POSSESSED:
            return
        
        day: int = self.game_info.day
        if day >= 2:
            vote_list = self.game_info.vote_list
            Util.debug_print("----- day_start -----")
            Util.debug_print("vote_list:\t", self.vote_to_dict(vote_list))
            Util.debug_print("vote_cnt:\t", self.vote_cnt(vote_list))
        
        self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, self.get_alive_others(self.game_info.agent_list))
        self.new_result = Species.WEREWOLF
        # ----- 狩人騙り -----
        if self.fake_role == Role.BODYGUARD:
            # 護衛成功の場合
            if self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
                self.has_report = False
        # ----- 他の騙り -----
        # 常に報告内容あり
        else:
            self.has_report = False
        # PP：3人以下
        alive_cnt: int = len(self.get_alive(self.game_info.agent_list))
        if alive_cnt <= 3:
            self.PP_flag = True
        self.not_judged_agents = self.get_alive_others(self.not_judged_agents)


    # CO、結果報告
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.estimate_werewolf()
        alive_others: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # if self.is_alive(a)でaliveを保証している
        others_seer_co = [a for a in self.comingout_map if self.is_alive(a) and self.comingout_map[a] == Role.SEER]
        others_seer_co_num = len(others_seer_co)
        self.vote_candidate = self.vote()
        # ---------- PP ----------
        if self.PP_flag and not self.has_PP:
            Util.debug_print('PP: Possessed')
            self.has_PP = True
            # return Content(ComingoutContentBuilder(self.me, Role.POSSESSED))
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
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
                    if self.has_co and not self.has_report:
                        self.has_report = True
                        self.new_result = Species.WEREWOLF
                        # 候補の優先順位：対抗の占いっぽいエージェント→人狼っぽくないエージェント
                        if others_seer_co:
                            self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, others_seer_co)
                        else:
                            self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                        return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # # ----- 結果報告 -----
                # if turn == 1:
                #     if not self.has_report:
                #         self.has_report = True
                #         self.new_result = Species.WEREWOLF
                #         # 候補の優先順位：対抗の占いっぽいエージェント→人狼っぽくないエージェント
                #         if others_seer_co:
                #             self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, others_seer_co)
                #         else:
                #             self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                #         return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # ----- VOTE and REQUEST -----
                elif 2 <= turn <= 9:
                    if turn % 2 == 0:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                    else:
                        return Content(VoteContentBuilder(self.new_target))
                else:
                    return CONTENT_SKIP
            elif day >= 2:
                if turn == 1:
                    # ----- PP -----
                    # 上のPPでreturnされているから、特に必要ない
                    return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
                # ----- VOTE and REQUEST -----
                elif 2 <= turn <= 9:
                    # 候補：人狼っぽくないエージェント
                    self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                    if turn % 2 == 0:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                    else:
                        return Content(VoteContentBuilder(self.new_target))
                else:
                    return CONTENT_SKIP
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            if others_seer_co_num >= 3:
                self.fake_role = Role.MEDIUM
            # ---------- 占い騙り ----------
            if self.fake_role == Role.SEER:
                # ----- 戦略A：占い結果を複数回言う -----
                if self.strategyA:
                    self.has_report = False
                # ----- 戦略C：COしてから占い結果 -----
                if self.strategyC:
                    # ----- CO -----
                    if not self.has_co and day == self.co_date:
                        self.has_co = True
                        return Content(ComingoutContentBuilder(self.me, Role.SEER))
                    # ----- 結果報告 -----
                    if self.has_co and not self.has_report:
                        self.has_report = True
                        if day == 1:
                            # 人狼っぽくないエージェントに黒結果
                            # self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                            self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, self.not_judged_agents)
                            self.new_result = Species.WEREWOLF
                        else:
                            r = random.random()
                            # 80%で人狼っぽいエージェントに白結果、20%で人狼っぽくないエージェントに黒結果
                            if r < 0.8:
                                # self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                                self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.not_judged_agents)
                                self.new_result = Species.HUMAN
                            else:
                                # self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                                self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, self.not_judged_agents)
                                self.new_result = Species.WEREWOLF
                        # 占い対象を占っていないエージェントから除く
                        if self.new_target in self.not_judged_agents:
                            self.not_judged_agents.remove(self.new_target)
                        return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                else:
                    # ----- 結果報告 -----
                    if turn == 1:
                        if not self.has_report:
                            self.has_report = True
                            r = random.random()
                            # 80%で人狼っぽいエージェントに白結果、20%で村人っぽいエージェントに黒結果
                            if r < 0.8:
                                self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                                self.new_result = Species.HUMAN
                            else:
                                self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                                self.new_result = Species.WEREWOLF
                            return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
            # ---------- 霊媒騙り ----------
            elif self.fake_role == Role.MEDIUM:
                # ----- CO -----
                if not self.has_co and day == self.co_date:
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.MEDIUM))
                # ----- 結果報告 -----
                if self.has_co and not self.has_report:
                    self.has_report = True
                    target: Agent = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
                    result: Species = Species.HUMAN
                    if target == AGENT_NONE:
                        return CONTENT_SKIP
                    # targetが占いCO or 人狼っぽい→白結果
                    # 注意：死亡しているエージェントを含めた占いCO
                    others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
                    estimate_role: Role = self.role_predictor.getMostLikelyRole(target)
                    if target in others_seer_co or estimate_role == Role.WEREWOLF:
                        result = Species.HUMAN
                    # 2人までは黒結果
                    elif self.black_count < 2:
                        self.black_count += 1
                        result = Species.WEREWOLF
                    return Content(IdentContentBuilder(target, result))
            # ---------- 狩人騙り ----------
            elif self.fake_role == Role.BODYGUARD:
                # ----- CO -----
                # 処刑されそうになったらCO
                if not self.has_co and self.is_Low_HP():
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, self.fake_role))
                # ----- 結果報告 -----
                if self.has_co and not self.has_report:
                    self.has_report = True
                    # 人狼っぽいエージェントを護衛
                    guard_agent: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                    return Content(GuardedAgentContentBuilder(guard_agent))
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
        return CONTENT_SKIP


    # 投票対象
    def vote(self) -> Agent:
        day: int = self.game_info.day
        game: int = Util.game_count
        self.estimate_werewolf()
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # 確定人狼がいたら除外
        if self.agent_werewolf:
            if self.agent_werewolf in vote_candidates:
                vote_candidates.remove(self.agent_werewolf)
        # ----------  同数投票の処理 ----------
        latest_vote_list = self.game_info.latest_vote_list
        if latest_vote_list:
            self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF, mostlikely=False)
            # 最多投票者が自分Aともう1人Bの場合、Bが選ばれている
            # Bが人狼っぽいなら、投票を人狼っぽくないエージェントに変更する
            # これにより、自分の投票が原因で人狼が処刑されることを防ぐ
            if self.role_predictor.getMostLikelyRole(self.vote_candidate) == Role.WEREWOLF:
                self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, vote_candidates)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # ---------- PP ----------
        if self.PP_flag:
            # 投票対象：人狼っぽくないエージェント
            self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, vote_candidates)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # ---------- 5人村 ----------
        if self.N == 5:
            # 人狼を判別できている場合：人狼の投票先に合わせる
            if self.agent_werewolf != AGENT_NONE:
                self.vote_candidate = self.will_vote_reports.get(self.agent_werewolf, AGENT_NONE)
                Util.debug_print('投票先\t', self.will_vote_reports_str)
                Util.debug_print(f'人狼っぽい:{self.agent_werewolf}\t投票を合わせる:{self.vote_candidate}')
                # 投票対象が自分 or 投票対象が死んでいる：処刑されそうなエージェントに投票
                if self.vote_candidate == self.me or self.vote_candidate == AGENT_NONE or not self.is_alive(self.vote_candidate):
                    self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=vote_candidates)
                    Util.debug_print('処刑されそうなエージェント2:', self.vote_candidate)
            # 人狼を判別できていない or 投票対象が自分 or 投票対象が死んでいる：人狼っぽくないエージェントに投票
            elif self.agent_werewolf == AGENT_NONE or self.vote_candidate == AGENT_NONE:
                Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
                self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 初日：自分の黒先
            if day == 1:
                self.vote_candidate = self.new_target
            else:
                # 人狼を判別できている場合：人狼の投票先に合わせる
                if self.agent_werewolf != AGENT_NONE:
                    self.vote_candidate = self.will_vote_reports.get(self.agent_werewolf, AGENT_NONE)
                    # Util.debug_print('投票先\t', self.will_vote_reports_str)
                    Util.debug_print(f'人狼っぽい:{self.agent_werewolf}\t投票を合わせる:{self.vote_candidate}')
                # 人狼を判別できていない or 投票対象がいない or 投票対象が自分 or 投票対象が死んでいる：人狼っぽくないエージェントに投票
                if self.agent_werewolf == AGENT_NONE or self.vote_candidate == AGENT_NONE or self.vote_candidate == self.me or not self.is_alive(self.vote_candidate):
                    Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
                    self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, vote_candidates)
        # ----- 投票ミスを防ぐ -----
        if self.vote_candidate == AGENT_NONE or self.vote_candidate == self.me:
            Util.debug_print("vote_candidates: AGENT_NONE or self.me")
            self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
