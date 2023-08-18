import random
from collections import deque
from typing import Deque, List, Optional

from const import CONTENT_SKIP
from ddhbVillager import ddhbVillager
from Util import Util

from aiwolf import (Agent, ComingoutContentBuilder, Content,
                    DivinedResultContentBuilder, EstimateContentBuilder,
                    GameInfo, GameSetting, Judge, RequestContentBuilder, Role,
                    Species, VoteContentBuilder)
from aiwolf.constant import AGENT_ANY, AGENT_NONE


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
    # ----- 5人村用：結果を変更して報告する -----
    new_target: Agent # 偽の占い対象
    new_result: Species # 偽の占い結果


    def __init__(self) -> None:
        """Initialize a new instance of ddhbSeer."""
        super().__init__()
        self.co_date = 0
        self.has_co = False
        self.my_judge_queue = deque()
        self.not_divined_agents = []
        self.werewolves = []
        
        self.new_target = AGENT_NONE
        self.new_result = Species.UNC
        self.strategies = []


    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        self.co_date = 1
        self.has_co = False
        self.my_judge_queue.clear()
        self.not_divined_agents = self.get_others(self.game_info.agent_list)
        self.werewolves.clear()
        self.new_target = AGENT_NONE
        self.new_result = Species.UNC
        
        self.strategies = [True]
        self.strategyA = self.strategies[0] # 戦略A: COする日にちの変更（初日CO）
        # 戦略A: 初日CO
        if self.strategyA:
            self.co_date = 1


    # 昼スタート
    def day_start(self) -> None:
        super().day_start()
        
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        # 占い結果
        judge: Optional[Judge] = self.game_info.divine_result
        if judge is not None:
            self.my_judge_queue.append(judge) # 結果追加
            # 占い対象を、占っていないエージェントリストから除く
            if judge.target in self.not_divined_agents:
                self.not_divined_agents.remove(judge.target)
            # 黒結果
            if judge.result == Species.WEREWOLF:
                self.werewolves.append(judge.target) # 人狼リストに追加
            # スコアの更新
            self.score_matrix.my_divined(self.game_info, self.game_setting, judge.target, judge.result)


    # CO、結果報告、投票宣言
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        game: int = Util.game_count
        # if self.is_alive(a)でaliveを保証している
        others_seer_co: List[Agent] = [a for a in self.comingout_map if self.is_alive(a) and self.comingout_map[a] == Role.SEER]
        others_co_num: int = len(others_seer_co)
        self.vote_candidate = self.vote()
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
                    if self.has_co and self.my_judge_queue:
                        judge: Judge = self.my_judge_queue.popleft()
                        self.new_target = judge.target
                        self.new_result = judge.result
                        # 黒結果：そのまま報告
                        if judge.result == Species.WEREWOLF:
                            return Content(DivinedResultContentBuilder(judge.target, judge.result))
                        # 白結果：状況に応じて黒結果を報告
                        elif judge.result == Species.HUMAN:
                            self.new_result = Species.WEREWOLF
                            # 対抗なし：人狼確率＋勝率が高いエージェント
                            if others_co_num == 0:
                                self.new_target = self.role_predictor.chooseStrongLikely(Role.WEREWOLF, self.get_alive_others(self.not_divined_agents), coef=0.1)
                            # 対抗あり：game<50では対抗で人狼っぽいエージェント、game>=50では人狼っぽいエージェント
                            else:
                                if game < 50:
                                    self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, others_seer_co)
                                else:
                                    self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.get_alive_others(self.not_divined_agents))
                            if self.new_target == AGENT_NONE:
                                self.new_target = judge.target
                                self.new_result = judge.result
                            return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # ----- VOTE and REQUEST -----
                elif 3 <= turn <= 9:
                    if turn % 2 == 0:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                    else:
                        return Content(VoteContentBuilder(self.new_target))
                else:
                    return CONTENT_SKIP
            elif day >= 2:
                # ----- 結果報告 -----
                if turn == 1:
                    if self.has_co and self.my_judge_queue:
                        judge: Judge = self.my_judge_queue.popleft()
                        self.new_target = judge.target
                        self.new_result = judge.result
                        # 黒結果：そのまま報告
                        if judge.result == Species.WEREWOLF:
                            return Content(DivinedResultContentBuilder(judge.target, judge.result))
                        # 白結果：生存者3人だから、残りの1人に黒結果（結果としては等価）
                        # 注意：占い先が噛まれた場合は等価ではない→人狼っぽい方に黒結果
                        elif judge.result == Species.HUMAN:
                            self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, self.get_alive_others(self.not_divined_agents))
                            self.new_result = Species.WEREWOLF
                            return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # 狂人が生きている場合→人狼COでPPを防ぐ
                elif turn == 2 and self.role_predictor.estimate_alive_possessed(threshold=0.5):
                    return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
                # ----- VOTE and REQUEST -----
                elif 2 <= turn <= 9:
                    if turn % 2 == 0:
                        return Content(VoteContentBuilder(self.new_target))
                    else:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                else:
                    return CONTENT_SKIP
            return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            # ---------- CO ----------
            # 絶対にCOする→1,2,3
            # 1: 予定の日にち
            if not self.has_co and day == self.co_date:
                Util.debug_print("占いCO：予定日")
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER))
            # 2: 人狼発見
            if not self.has_co and self.werewolves:
                Util.debug_print("占いCO：人狼発見")
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER))
            # 3: 他の占い師がCOしたら(CCO)
            if not self.has_co and others_seer_co:
                Util.debug_print("占いCO：CCO")
                self.has_co = True
                return Content(ComingoutContentBuilder(self.me, Role.SEER)) 
            # ---------- 結果報告 ----------
            if self.has_co and self.my_judge_queue:
                judge: Judge = self.my_judge_queue.popleft()
                # 正しい結果を報告する
                return Content(DivinedResultContentBuilder(judge.target, judge.result))
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
        # ----------  同数投票の処理 ---------- 
        latest_vote_list = self.game_info.latest_vote_list
        if latest_vote_list:
            self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # 投票候補
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # if a in vote_candidates としているから、aliveは保証されている
        others_seer_co: List[Agent] = [a for a in self.comingout_map if a in vote_candidates and self.comingout_map[a] == Role.SEER]
        alive_werewolves: List[Agent] = self.get_alive_others(self.werewolves)
        # ---------- 5人村 ----------
        if self.N == 5:
            # 投票対象の優先順位：黒結果→偽の黒先→人狼っぽいエージェント
            if alive_werewolves:
                Util.debug_print("alive_werewolves:\t", self.agent_to_index(alive_werewolves))
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_werewolves)
            # 2ターン目の推論だとミスしている可能性があるので、行動学習で推定した結果を使う
            # elif self.new_target != AGENT_NONE:
            #     self.vote_candidate = self.new_target
            else:
                Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 投票対象の優先順位：黒結果→偽占い→人狼っぽいエージェント
            if alive_werewolves:
                Util.debug_print("alive_werewolves:\t", self.agent_to_index(alive_werewolves))
                self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=alive_werewolves)
                # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_werewolves)
            elif others_seer_co:
                Util.debug_print("others_seer_co:\t", self.agent_to_index(others_seer_co))
                self.vote_candidate = self.chooseMostlikelyExecuted2(include_list=others_seer_co)
                # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, others_seer_co)
            else:
                Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ----- 投票ミスを防ぐ -----
        if self.vote_candidate == AGENT_NONE or self.vote_candidate == self.me:
            Util.debug_print("vote_candidates: AGENT_NONE or self.me")
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me


    # 占い対象
    def divine(self) -> Agent:
        day: int = self.game_info.day
        game: int = Util.game_count
        divine_candidate: Agent = AGENT_NONE
        # 占い候補：占っていないエージェント
        divine_candidates: List[Agent] = self.get_alive_others(self.not_divined_agents)
        others_co: List[Agent] = [a for a in self.comingout_map if a in divine_candidates and (self.comingout_map[a] == Role.SEER or self.comingout_map[a] == Role.MEDIUM)]
        # 占い候補：占っていないエージェント＋(占いor霊媒)COしていないエージェント
        divine_no_co_candidates: List[Agent] = [a for a in divine_candidates if a not in others_co]
        # 占い対象：game<50では人狼確率＋勝率が高いエージェント、game>=50では人狼っぽいエージェント
        # game後半は、推論精度が高いため、人狼っぽいエージェントを占う
        if game < 50:
            # divine_candidate = self.role_predictor.chooseStrongLikely(Role.WEREWOLF, divine_candidates, coef=0.5)
            divine_candidate = self.role_predictor.chooseStrongLikely(Role.WEREWOLF, divine_no_co_candidates, coef=0.5)
        else:
            # divine_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, divine_candidates)
            divine_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, divine_no_co_candidates)
        # ---------- 5人村15人村共通 ----------
        # 初日：勝率が高いエージェント（情報がほぼないため）
        # 白結果：味方になる、黒結果：早めに処理できる
        if day == 0:
            divine_candidate = Util.get_strong_agent(divine_candidates)
        Util.debug_print("alive_comingout_map:\t", self.alive_comingout_map_str)
        Util.debug_print(f"占い対象：{divine_candidate}")
        return divine_candidate if divine_candidate != AGENT_NONE else self.me
