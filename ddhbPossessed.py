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
        self.strategies = []
        self.has_report = False # 占い等の結果を報告したかのフラグ
        self.black_count = 0 # 霊媒師が黒判定した数

    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        super().initialize(game_info, game_setting)

        # ハックを検証するためのフラグ
        self.strategies = [False, False, False, True]
        self.hackA = self.strategies[0] # 一日で何回も占い結果を言う
        self.hackB = self.strategies[1] # 人狼に嚙まれないように、狩人COしない
        self.hackC = self.strategies[2] # 基本的に占い師COする
        self.hackD = self.strategies[3] # 占い師でずっと黒判定を出し続ける

        if self.N == 5:
            self.fake_role = Role.SEER
        else :
            if self.hackC == True: # 基本的に占い師COするハック
                self.fake_role = Role.SEER
            else:
                # 65%の確率で占い師、35%の確率で霊媒師
                self.fake_role = Role.SEER if random.random() < 0.65 else Role.MEDIUM
        self.N = game_setting.player_num

        self.co_date = 1 # 最低でも1日目にCO → 変更する
        self.has_co = False
        self.my_judgee_queue.clear()
        self.not_judged_agents = self.get_others(self.game_info.agent_list)
        self.num_wolves = game_setting.role_num_map.get(Role.WEREWOLF, 0)
        self.werewolves.clear()
        self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)

        self.has_report = False
        self.black_count = 0

    def day_start(self) -> None:
        super().day_start()
        # 狩人以外のとき、報告済みフラグをfalseにする
        # つまりこれから報告する内容がある
        if (self.fake_role != Role.BODYGUARD):
            self.has_report = False

        # 狩人のとき、護衛成功したか確認
        if self.fake_role == Role.BODYGUARD and self.game_info.guarded_agent != None and len(self.game_info.last_dead_agent_list) == 0:
            # これから報告する
            self.has_report = False

        # # Process the fake judgement.
        # # 昼に騙り結果
        # judge: Judge = self.get_fake_judge()
        # if judge != JUDGE_EMPTY:
        #     self.my_judgee_queue.append(judge)
        #     # 占い対象を、占っていないエージェントリストから除く
        #     if judge.target in self.not_judged_agents:
        #         self.not_judged_agents.remove(judge.target)
        #     # 人狼発見 → 人狼結果リストに追加
        #     if judge.result == Species.WEREWOLF:
        #         self.werewolves.append(judge.target)

    def vote(self) -> Agent:
        max_score = -1
        agent_vote_for : Agent = AGENT_NONE

        #  狂人の場合：生存するエージェントが3人以下だったら、一番人狼っぽくない人に投票
        if(len(self.get_alive(self.game_info.agent_list)) <= 3) :
            for agent in self.game_info.agent_list:
                if agent != self.me and self.is_alive(agent) :
                    score = 1 - self.role_predictor.getProb(agent, Role.WEREWOLF)
                    if score > max_score :
                        max_score = score
                        agent_vote_for = agent
        else :
            # 狂人の場合：5人村だったら、一番人狼っぽくない人に投票
            if(self.N == 5) :
                for agent in self.game_info.agent_list:
                    if agent != self.me and self.is_alive(agent) :
                        # 元のコードでは、自分の役職を占い師としたときの確率から、狂人としたときの確率を引いている
                        score = 1 - self.role_predictor.getProb(agent, Role.WEREWOLF)
                        if score > max_score :
                            max_score = score
                            agent_vote_for = agent
                        break
            # 狂人の場合：15人村だったら、一番人狼っぽい人に投票
            else :
                for agent in self.game_info.agent_list:
                    if agent != self.me and self.is_alive(agent) :
                        score = self.role_predictor.getProb(agent, Role.WEREWOLF)
                        if score > max_score :
                            max_score = score
                            agent_vote_for = agent

        return agent_vote_for
    
    

    # CO、結果報告
    def talk(self) -> Content:
        # もし占い師を語るならば
        if self.fake_role == Role.SEER:
            if self.hackA == True: # 一日で何回も占い結果を言うハック
                # トークのたびにまだ報告していないことにする
                self.has_report = False

            if self.has_co == False:
                # 占い師が何人いるかを数える
                others_seer_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.SEER]
                num_seer = len(others_seer_co)

                if(self.hackB == False): # 人狼に嚙まれないように、狩人COしないハック
                    # 占い師が既に二人以上いるならば、狩人を騙る
                    if num_seer >= 2:
                        self.fake_role = Role.BODYGUARD
                        # 狩人は毎回報告する内容があるとは限らないから、has_reportはTrueにする
                        self.has_report = True
                    else:
                        self.has_co = True
                        return Content(ComingoutContentBuilder(self.me, self.fake_role))
                    
            if self.has_report == False:
                self.has_report = True
                # 5人村のとき
                if self.N == 5:
                    # 対抗がいたら、対抗が黒だという
                    # review: i を使う必要がなければ for agent in self.game_info.agent_list: とする方が良い
                    # review: getProb(i, Role.WEREWOLF) は getProb(agent, Role.WEREWOLF) でもOK
                    # review: self.me の型は agent なので i と比較しない (agent != self.me はOK)
                    # review: i != self.me は色々なところで使われているので他も直しておいてほしい
                    for agent in self.game_info.agent_list:
                        if agent != self.me and self.is_alive(agent):
                            if self.comingout_map[self.game_info.agent_list[i]] == Role.SEER:
                                return Content(DivinedResultContentBuilder(agent, Species.WEREWOLF))
                            # 対抗がいなかったら、最も人狼っぽい人を白だと言う
                            # review: ここで else を使うと、Agent[01] が占いCOしていなければという条件になってしまいそう
                            # review: else を消して中身をインデント3つ下げれば良さそう (つまりこの for で return されずに続きが実行されるなら対抗がいない)
                            else:
                                max = -1
                                # review: for i in range(self.N) の間違い？
                                # review: i が被ってないかは確認した方が良い (この場合 i を上書きしているので)
                                # review: ここも for agent in self.game_info.agent_list: とかで良さそう
                                for agent in self.game_info.agent_list:
                                    if agent != self.me and self.is_alive(agent):
                                        score = self.role_predictor.getProb(agent, Role.WEREWOLF)
                                        if score > max:
                                            max = score
                                            agent_white : Agent = agent
                                return Content(DivinedResultContentBuilder(agent_white, Species.HUMAN))
                            
                # 15人村のとき
                else:
                    if self.hackD == True: # 占い師でずっと黒判定を出し続けるハック
                        # 一番人狼っぽい人を黒だという
                        max = -1
                        for agent in self.game_info.agent_list:
                            if agent != self.me and self.is_alive(agent):
                                score = self.role_predictor.getProb(agent, Role.WEREWOLF)
                                if score > max:
                                    max = score
                                    agent_black : Agent = agent
                        return Content(DivinedResultContentBuilder(agent_black, Species.WEREWOLF))
                    
                    # review: 5人村と同様のミスがあるので、全体的にチェックしてほしい
                    # 二日目以外は、最も村人っぽい人を黒だという
                    else:
                        if self.game_info.day != 2:
                            max = -1
                            for agent in self.game_info.agent_list:
                                if agent != self.me and self.is_alive(agent):
                                    score = 1 - self.role_predictor.getProb(agent, Role.WEREWOLF)
                                    if score > max:
                                        max = score
                                        agent_black : Agent = agent
                            return Content(DivinedResultContentBuilder(agent_black, Species.WEREWOLF))
                        
                        # 二日目は、最も人狼っぽい人を白だという
                        else:
                            max = -1
                            for agent in self.game_info.agent_list:
                                if agent != self.me and self.is_alive(agent):
                                    score = self.role_predictor.getProb(agent, Role.WEREWOLF)
                                    if score > max:
                                        max = score
                                        agent_white : Agent = agent
                            return Content(DivinedResultContentBuilder(agent_white, Species.HUMAN))
                    
        # もし霊媒師を語るならば
        elif self.fake_role == Role.MEDIUM:
            if self.has_co == False:
                # 霊媒師が何人いるかを数える
                # 注意：comingout_mapには、自分は含まれていない
                others_medium_co = [a for a in self.comingout_map if self.comingout_map[a] == Role.MEDIUM]
                num_medium = len(others_medium_co)

                if self.hackB == False: # 人狼に嚙まれないように、狩人COしないハック
                    # 霊媒師が既に二人以上いるならば、狩人を騙る
                    if num_medium >= 2:
                        self.fake_role = Role.BODYGUARD
                        self.has_report = True
                    else:
                        self.has_co = True
                        return Content(ComingoutContentBuilder(self.me, self.fake_role))
                    
            if self.has_report == False:
                self.has_report = True
                if self.game_info.executed_agent != None:
                    target : Agent = self.game_info.executed_agent
                    # もしtargetが占い師COしていたら、白判定する
                    if target in self.comingout_map and self.comingout_map[target] == Role.SEER:
                        return Content(IdentContentBuilder(target, Species.HUMAN))
                    # 占い師COしていなかったら、黒判定する(二人まで)
                    elif self.black_count < 2:
                        self.black_count += 1
                        return Content(IdentContentBuilder(target, Species.WEREWOLF))
                    # それ以外は白判定する
                    else:
                        return Content(IdentContentBuilder(target, Species.HUMAN))

        # もし狩人を騙るならば          
        else:
            # COしていなかったら
            if self.has_co == False:
                # 護衛成功したら、狩人CO
                if self.has_report == False:
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, self.fake_role))
                
                # 3人以上が自分に投票したら、狩人CO
                vote_num = 0
                for vote in self.game_info.vote_list:
                    if vote.target == self.me:
                        vote_num += 1
                
                if vote_num >= 3:
                    self.has_co = True
                    return Content(ComingoutContentBuilder(self.me, self.fake_role))

            # COしていたら    
            else :
                # まだ報告してなくて、2日目以降ならば
                if self.has_report == False and self.game_info.day >= 2:
                    # 最も人狼っぽい人を護衛したと発言する
                    max = -1
                    for agent in self.game_info.agent_list:
                        if agent != self.me and self.is_alive(agent):
                            score = self.role_predictor.getProb(agent, Role.WEREWOLF)
                            if score > max:
                                max = score
                                agent_guard : Agent = agent

                    self.has_report = True
                    # 発言をする（未実装）
                    
        # 共通の処理
        # 生存者が3人以下だったら、人狼COする
        if len(self.get_alive(self.game_info.agent_list)) <= 3:
            return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))    

            
        # # Vote for one of the alive fake werewolves.
        # # 投票候補：人狼結果リスト
        # candidates: List[Agent] = self.get_alive(self.werewolves)
        # # Vote for one of the alive agent that declared itself the same role of Possessed
        # # if there are no candidates.
        # # 候補なし → 対抗
        # if not candidates:
        #     candidates = self.get_alive([a for a in self.comingout_map
        #                                  if self.comingout_map[a] == self.fake_role])
        # # Vite for one of the alive agents if there are no candidates.
        # # 生存者
        # if not candidates:
        #     candidates = self.get_alive_others(self.game_info.agent_list)
        # # Declare which to vote for if not declare yet or the candidate is changed.
        # # 候補からランダムセレクト
        # if self.vote_candidate == AGENT_NONE or self.vote_candidate not in candidates:
        #     self.vote_candidate = self.random_select(candidates)
        #     if self.vote_candidate != AGENT_NONE:
        #         return Content(VoteContentBuilder(self.vote_candidate))
        return CONTENT_SKIP
