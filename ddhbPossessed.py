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
                    VoteContentBuilder, EstimateContentBuilder,
                    RequestContentBuilder, GuardContentBuilder, GuardedAgentContentBuilder)
from aiwolf.constant import AGENT_NONE, AGENT_ANY

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
    others_seer_co: List[Agent] # 他の占い師のCOリスト
    new_target: Agent # 偽の占い対象
    new_result: Species # 偽の占い結果



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
        self.strategies = []
        self.has_report = False # 占い等の結果を報告したかのフラグ
        self.black_count = 0 # 霊媒師が黒判定した数
        self.PP_flag = False
        self.has_PP = False
        self.others_seer_co = []

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)
        # 自分のロールがPOSSESEDでない時、以下をスキップする
        if self.game_info.my_role != Role.POSSESSED:
            return

        # 戦略を検証するためのフラグ
        self.strategies = [False, False, True, False, False, False, False, False, True]
        self.strategyA = self.strategies[0] # 一日で何回も占い結果を言う
        self.strategyB = self.strategies[1] # 狩人COしない(3人目でも出る)
        self.strategyC = self.strategies[2] # 基本的に占い師COする(これ単体だと占いしない)
        self.strategyD = self.strategies[3] # 人狼っぽい人に、占い師でずっと黒判定を出し続ける
        self.strategyE = self.strategies[4] # 人狼っぽくない人に、占い師でずっと黒判定を出し続ける
        self.strategyF = self.strategies[5] # 人狼っぽい人に、占い師でずっと白判定を出し続ける
        self.strategyG = self.strategies[6] # 対抗の占い師がいたら、対抗が黒だという
        self.strategyH = self.strategies[7] # COせず、完全に潜伏（比較用）
        self.strategyI = self.strategies[8] # 占い師/霊媒師っぽい動きをする
        
        ##### ここから変更 #####
        # ---------- 5人村15人村共通 ----------
        self.co_date = 1
        self.has_co = False
        self.my_judge_queue.clear()
        self.not_judged_agents = self.get_others(self.game_info.agent_list)
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()
        self.has_report = False
        self.black_count = 0
        self.PP_flag = False
        self.has_PP = False
        self.others_seer_co.clear()
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        # ---------- 5人村 ----------
        if self.N == 5:
            self.co_date = 1
            self.fake_role = Role.SEER
        # ---------- 15人村 ----------
        elif self.N == 15:
            # self.co_date = 2
            # ----- 戦略C：100%で占いCO -----
            if self.strategyC:
                self.fake_role = Role.SEER
            else:
                # 65%の確率で占い師、35%の確率で霊媒師
                self.fake_role = Role.SEER if random.random() < 0.65 else Role.MEDIUM
        
        # ----- 戦略H：潜伏する -----
        if self.strategyH:
            self.fake_role = Role.VILLAGER

        ##### ここまで変更 #####
        

        # # ---------- 5人村 ----------
        # if self.N == 5:
        #     self.fake_role = Role.SEER
            
        # # ---------- 15人村 ----------
        # elif self.N == 15:
        # # else :
        #     if self.strategyC == True: # 基本的に占い師COする戦略
        #         self.fake_role = Role.SEER
        #     else:
        #         # 65%の確率で占い師、35%の確率で霊媒師
        #         self.fake_role = Role.SEER if random.random() < 0.65 else Role.MEDIUM
        # # self.N = game_setting.player_num

        # self.co_date = 1 # 最低でも1日目にCO → 変更する
        # self.has_co = False
        # self.my_judge_queue.clear()
        # self.not_judged_agents = self.get_others(self.game_info.agent_list)
        # self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        # self.werewolves.clear()
        # # self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)

        # self.has_report = False
        # self.black_count = 0
        # self.PP_flag = False
        # self.others_seer_co.clear()
        # self.new_target = AGENT_NONE
        # self.new_result = Species.WEREWOLF


    def day_start(self) -> None:
        super().day_start()
        self.new_target = AGENT_NONE
        self.new_result = Species.WEREWOLF
        # 自分のロールがPOSSESEDでない時、以下をスキップする
        if self.game_info.my_role != Role.POSSESSED:
            return
        
        # 狩人以外のとき、報告済みフラグをfalseにする
        # つまりこれから報告する内容がある
        if (self.fake_role != Role.BODYGUARD):
            self.has_report = False

        # 狩人のとき、護衛成功したか確認
        if self.fake_role == Role.BODYGUARD and self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            # これから報告する
            self.has_report = False

        # PP：3人以下
        alive_cnt: int = len(self.get_alive(self.game_info.agent_list))
        if alive_cnt <= 3:
            self.PP_flag = True
            self.has_PP = False

        self.not_judged_agents = self.get_alive_others(self.not_judged_agents)
        # # Process the fake judgement.
        # # 昼に騙り結果
        # judge: Judge = self.get_fake_judge()
        # if judge != JUDGE_EMPTY:
        #     self.my_judge_queue.append(judge)
        #     # 占い対象を、占っていないエージェントリストから除く
        #     if judge.target in self.not_judged_agents:
        #         self.not_judged_agents.remove(judge.target)
        #     # 人狼発見 → 人狼結果リストに追加
        #     if judge.result == Species.WEREWOLF:
        #         self.werewolves.append(judge.target)


    # 投票対象
    def vote(self) -> Agent:
        ##### シンプルなコードに変更する #####
        alive_others: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
        if self.PP_flag:
            self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
            # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # ---------- 5人村 ----------
        if self.N == 5:
            self.vote_candidate = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
            # self.vote_candidate = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
        # ---------- 15人村 ----------
        elif self.N == 15:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        ##### シンプルなコードに変更する #####
        
        # alive_other_list: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # self.vote_candidate : Agent = AGENT_NONE

        # #  狂人の場合：生存するエージェントが3人以下だったら、一番人狼っぽくない人に投票
        # if(self.PP_flag == True) :
        #     mx = -1
        #     p = self.role_predictor.prob_all
        #     for agent in alive_other_list:
        #         score = 1 - p[agent][Role.WEREWOLF]
        #         if score > mx:
        #             mx = score
        #             self.vote_candidate = agent
        # else :
        #     # 狂人の場合：5人村だったら、一番人狼っぽくない人に投票
        #     if(self.N == 5) :
        #         mx = -1
        #         p = self.role_predictor.prob_all
        #         for agent in alive_other_list:
        #             score = 1 - p[agent][Role.WEREWOLF]
        #             if score > mx:
        #                 mx = score
        #                 self.vote_candidate = agent
        #     # 狂人の場合：15人村だったら、一番人狼っぽい人に投票
        #     else :
        #         self.vote_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)

        # return self.vote_candidate
    
    

    # CO、結果報告
    def talk(self) -> Content:
        # ---------- PP ----------
        if self.PP_flag and not self.has_PP:
            self.has_PP = False
            # return Content(ComingoutContentBuilder(self.me, Role.POSSESSED))
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
        
        ##### ここから変更する #####
        day: int = self.game_info.day
        turn: int = self.talk_turn
        alive_others: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # todo: not_judged_agentsを利用する
        self.not_judged_agents = self.get_alive_others(self.not_judged_agents)
        self.others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        others_seer_co_num = len(self.others_seer_co)
        if others_seer_co_num >= 3:
            self.fake_role = Role.MEDIUM
        self.vote_candidate = self.vote()
        # ---------- 5人村 ----------
        if self.N == 5:
            if day == 1:
                talk_start: int = 2
                # ----- CO -----
                if turn == talk_start:
                    if not self.has_co:
                        self.has_co = True
                        return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # ----- 結果報告 -----
                elif turn == talk_start + 1:
                    if self.has_co:
                        # 候補：対抗の占いっぽいエージェント
                        self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, self.others_seer_co)
                        # 候補なし → 村人っぽいエージェント or 人狼っぽくないエージェント
                        if self.new_target == AGENT_NONE:
                            # self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
                            self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                        self.new_result = Species.WEREWOLF
                        return Content(DivinedResultContentBuilder(self.new_target, self.new_result))
                # ----- VOTE and REQUEST -----
                elif turn == 3 or turn == 5:
                    return Content(VoteContentBuilder(self.new_target))
                elif turn == 4 or turn == 6:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                else:
                    return CONTENT_SKIP
            elif day >= 2:
                if turn == 1:
                    # ----- PP -----
                    return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
                elif turn == 2:
                    # 候補：村人っぽいエージェント or 人狼っぽくないエージェント
                    # self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
                    self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                # ----- VOTE and REQUEST -----
                elif turn == 3 or turn == 5:
                    return Content(VoteContentBuilder(self.new_target))
                elif turn == 4 or turn == 6:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.new_target))))
                else:
                    return CONTENT_SKIP
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            # ---------- 占い騙り ----------
            if self.fake_role == Role.SEER:
                # ----- 戦略A：占い結果を複数回言う -----
                if self.strategyA:
                    self.has_report = False
                # ----- CO -----
                if not self.has_co and day == self.co_date:
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, Role.SEER))
                # ----- 結果報告 -----
                if self.has_co and not self.has_report:
                    self.has_report = True
                    # ----- 戦略G：対抗の占いっぽいエージェントに黒結果 -----
                    if self.strategyG:
                        self.new_target = self.role_predictor.chooseMostLikely(Role.SEER, self.others_seer_co)
                        self.new_result = Species.WEREWOLF
                    # ----- 戦略D：人狼っぽいエージェントに黒結果 -----
                    if self.strategyD:
                        self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                        self.new_result = Species.WEREWOLF
                    # ----- 戦略E：村人っぽいエージェントに黒結果 -----
                    if self.strategyE:
                        # self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
                        self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                        self.new_result = Species.WEREWOLF
                    # ----- 戦略F：人狼っぽいエージェントに白結果 -----
                    if self.strategyF:
                        self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                        self.new_result = Species.HUMAN
                    # ----- 戦略I：占いっぽい結果 -----
                    if self.strategyI:
                        r = random.random()
                        # 80%で人狼っぽいエージェントに白結果、20%で村人っぽいエージェントに黒結果
                        if r < 0.8:
                            self.new_target = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_others)
                            self.new_result = Species.HUMAN
                        else:
                            # self.new_target = self.role_predictor.chooseMostLikely(Role.VILLAGER, alive_others)
                            self.new_target = self.role_predictor.chooseLeastLikely(Role.WEREWOLF, alive_others)
                            self.new_result = Species.WEREWOLF
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
                    # targetが占いCO→白結果
                    if target in self.others_seer_co:
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
            if turn >= 2 and turn <= 7:
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
        ##### ここまで変更する #####
                
        # alive_other_list: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # # もし占い師を語るならば
        # if self.fake_role == Role.SEER:
        #     if self.strategyA == True: # 一日で何回も占い結果を言う戦略
        #         # トークのたびにまだ報告していないことにする
        #         self.has_report = False

        #     if self.has_co == False:
        #         # 占い師が何人いるかを数える
        #         others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
        #         num_seer = len(others_seer_co)

        #         if(self.strategyB == False): # 人狼に嚙まれないように、狩人COしない戦略
        #             # 占い師が既に二人以上いるならば、霊媒師を騙る
        #             if num_seer >= 2:
        #                 self.fake_role = Role.MEDIUM
        #             elif self.strategyH == False: # 潜伏する戦略がFalse
        #                     self.has_co = True
        #                     return Content(ComingoutContentBuilder(self.me, self.fake_role))
                    
        #     if self.has_report == False:
        #         self.has_report = True
        #         # 5人村のとき
        #         if self.N == 5:
        #             if self.strategyI == True: # 占い師っぽい動きをする戦略
        #                 # 人狼っぽくない人を黒という
        #                 mx = -1
        #                 p = self.role_predictor.prob_all
        #                 divine_candidate: Agent = AGENT_NONE
        #                 for agent in alive_other_list:
        #                     score = 1 - p[agent][Role.WEREWOLF]
        #                     if score > mx:
        #                         mx = score
        #                         divine_candidate = agent
        #                 return Content(DivinedResultContentBuilder(divine_candidate, Species.WEREWOLF))
        #             else:
        #                 # 対抗がいたら、対抗が黒だという
        #                 for agent in alive_other_list:
        #                     if self.comingout_map[agent] == Role.SEER:
        #                         return Content(DivinedResultContentBuilder(agent, Species.WEREWOLF))
                            
        #                 # 対抗がいなければ、最も人狼っぽい人を白だという
        #                 divine_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)
        #                 return Content(DivinedResultContentBuilder(divine_candidate, Species.HUMAN))
                            
        #         # 15人村のとき
        #         else:
        #             if self.strategyG == True: # 対抗の占い師がいたら、優先的に対抗が黒だという戦略
        #                 # 対抗がいたら、対抗が黒だという
        #                 for agent in self.comingout_map:
        #                     if self.comingout_map[agent] == Role.SEER:
        #                         return Content(DivinedResultContentBuilder(agent, Species.WEREWOLF))
               
        #             if self.strategyD == True: # 人狼っぽい人に、占い師でずっと黒判定を出し続ける戦略
        #                 # 一番人狼っぽい人を黒だという
        #                 divine_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)
        #                 return Content(DivinedResultContentBuilder(divine_candidate, Species.WEREWOLF))
                    
        #             if self.strategyE == True: # 人狼っぽくない人に、占い師でずっと黒判定を出し続ける戦略
        #                 # 一番人狼っぽくない人を黒だという
        #                 mx = -1
        #                 p = self.role_predictor.prob_all
        #                 divine_candidate: Agent = AGENT_NONE
        #                 for agent in alive_other_list:
        #                     score = 1 - p[agent][Role.WEREWOLF]
        #                     if score > mx:
        #                         mx = score
        #                         divine_candidate = agent
        #                 return Content(DivinedResultContentBuilder(divine_candidate, Species.WEREWOLF))
                    
        #             if self.strategyF == True: # 人狼っぽい人に、占い師でずっと白判定を出し続ける戦略
        #                 # 一番人狼っぽい人を白だという
        #                 divine_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)
        #                 return Content(DivinedResultContentBuilder(divine_candidate, Species.HUMAN))
                    
        #             if self.strategyI == True: # 占い師っぽい動きをする戦略
        #                 # 80%の確率で人狼っぽい人を白だという
        #                 if random.random() < 0.8:
        #                     divine_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)
        #                     return Content(DivinedResultContentBuilder(divine_candidate, Species.HUMAN))
        #                 # 20%の確率で人狼っぽくない人を黒だという
        #                 else:
        #                     mx = -1
        #                     p = self.role_predictor.prob_all
        #                     divine_candidate: Agent = AGENT_NONE
        #                     for agent in alive_other_list:
        #                         score = 1 - p[agent][Role.WEREWOLF]
        #                         if score > mx:
        #                             mx = score
        #                             divine_candidate = agent
        #                     return Content(DivinedResultContentBuilder(divine_candidate, Species.WEREWOLF))
                        
        #     if self.strategyI == True: # 占い師っぽい動きをする戦略
        #         # 二ターン目以降だったら、投票宣言する
        #         if self.talk_turn >= 2 and self.vote_candidate == AGENT_NONE:
        #             self.vote_candidate = self.vote()
        #             return Content(VoteContentBuilder(self.vote_candidate))
                
        #         return CONTENT_SKIP

                        
        # # もし霊媒師を語るならば
        # elif self.fake_role == Role.MEDIUM:
        #     # まだCOしてなくて、二日目以降ならば
        #     if self.has_co == False and self.game_info.day >= 2:
        #         if self.strategyB == False: # 人狼に嚙まれないように、狩人COしない戦略
        #             if self.strategyH == False: # 潜伏する戦略がFalse
        #                 self.has_co = True
        #                 return Content(ComingoutContentBuilder(self.me, self.fake_role))
                    
        #     if self.has_report == False:
        #         self.has_report = True
        #         if self.game_info.executed_agent != None:
        #             target = self.game_info.executed_agent if self.game_info.executed_agent is not None else AGENT_NONE
        #             # もしtargetが占い師COしていたら、白判定する
        #             if target in self.comingout_map and self.comingout_map[target] == Role.SEER:
        #                 return Content(IdentContentBuilder(target, Species.HUMAN))
        #             # 占い師COしていなかったら、黒判定する(二人まで)
        #             elif self.black_count < 2:
        #                 self.black_count += 1
        #                 return Content(IdentContentBuilder(target, Species.WEREWOLF))
        #             # それ以外は白判定する
        #             else:
        #                 return Content(IdentContentBuilder(target, Species.HUMAN))
            
        #     if self.strategyI == True: # 霊媒師っぽい動きをする戦略
        #         # ２週目以降で、投票宣言していなかったら
        #         if self.talk_turn >= 2 and self.vote_candidate == AGENT_NONE:
        #             self.vote_candidate = self.vote()
        #             return Content(VoteContentBuilder(self.vote_candidate))
        #         return CONTENT_SKIP

        # # もし狩人を騙るならば          
        # else:
        #     # COしていなかったら
        #     if self.has_co == False:
        #         # 護衛成功したら、狩人CO
        #         if self.has_report == False and self.strategyH == False: # 潜伏する戦略がFalse
        #             self.has_co = True
        #             return Content(ComingoutContentBuilder(self.me, self.fake_role))
                
        #         # 3人以上が自分に投票したら、狩人CO
        #         vote_num = 0
        #         for vote in self.game_info.vote_list:
        #             if vote.target == self.me:
        #                 vote_num += 1
                
        #         if vote_num >= 3 and self.strategyH == False: # 潜伏する戦略がFalse
        #             self.has_co = True
        #             return Content(ComingoutContentBuilder(self.me, self.fake_role))

        #     # COしていたら    
        #     else :
        #         # まだ報告してなくて、2日目以降ならば
        #         if self.has_report == False and self.game_info.day >= 2:
        #             # 最も人狼っぽい人を護衛したと発言する
        #             guard_candidate: Agent = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_other_list)

        #             self.has_report = True
        #             # 発言をする（未実装）

        # return CONTENT_SKIP
