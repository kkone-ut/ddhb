#
# villager.py
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
from collections import defaultdict
from typing import Dict, List

from aiwolf import (AbstractPlayer, Agent,ComingoutContentBuilder, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic,
                    VoteContentBuilder)
from aiwolf.constant import (AGENT_NONE, AGENT_ANY, AGENT_UNSPEC)

from const import CONTENT_SKIP

from Util import Util
from ScoreMatrix import ScoreMatrix
from RolePredictor import RolePredictor
from Assignment import Assignment
from TeamPredictor import TeamPredictor

# 村役職
class ddhbVillager(AbstractPlayer):
    """ddhb villager agent."""

    me: Agent # 自身
    """Myself."""
    vote_candidate: Agent # 投票候補
    """Candidate for voting."""
    game_info: GameInfo # ゲーム情報
    """Information about current game."""
    game_setting: GameSetting # ゲーム設定
    """Settings of current game."""
    comingout_map: Dict[Agent, Role] # CO辞書
    """Mapping between an agent and the role it claims that it is."""
    divination_reports: List[Judge] # 占い結果
    """Time series of divination reports."""
    identification_reports: List[Judge] # 霊媒結果
    """Time series of identification reports."""
    will_vote_reports: "defaultdict[Agent, Agent]" # 投票宣言
    talk_list_head: int # talkのインデックス
    """Index of the talk to be analysed next."""
    talk_list_all: List[Talk] # 全talkリスト

    def __init__(self) -> None:
        """Initialize a new instance of ddhbVillager."""

        self.me = AGENT_NONE
        self.vote_candidate = AGENT_NONE
        self.game_info = None  # type: ignore
        self.comingout_map = {}
        self.divination_reports = []
        self.identification_reports = []
        self.will_vote_reports = defaultdict(lambda: AGENT_NONE)
        self.talk_list_head = 0
        self.talk_list_all = []

        self.role_predictor = None
        
        self.N = -1
        self.M = -1
        
        self.agent_idx_0based = -1

        # フルオープンしたかどうか
        self.doFO = False

        # 統計
        self.game_count = 0
        self.win_count = 0
        self.sum_score = 0

    # エージェントが生存しているか
    def is_alive(self, agent: Agent) -> bool:
        """Return whether the agent is alive.

        Args:
            agent: The agent.

        Returns:
            True if the agent is alive, otherwise false.
        """
        return self.game_info.status_map[agent] == Status.ALIVE

    # 自分以外のエージェントリスト
    def get_others(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of agents excluding myself from the given list of agents.

        Args:
            agent_list: The list of agent.

        Returns:
            A list of agents excluding myself from agent_list.
        """
        return [a for a in agent_list if a != self.me]

    # 生存するエージェントリスト
    def get_alive(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of alive agents contained in the given list of agents.

        Args:
            agent_list: The list of agents.

        Returns:
            A list of alive agents contained in agent_list.
        """
        return [a for a in agent_list if self.is_alive(a)]

    # 自分以外の生存するエージェントリスト
    def get_alive_others(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of alive agents that is contained in the given list of agents
        and is not equal to myself.

        Args:
            agent_list: The list of agents.

        Returns:
            A list of alie agents that is contained in agent_list
            and is not equal to mysef.
        """
        return self.get_alive(self.get_others(agent_list))

    # ランダムセレクト
    def random_select(self, agent_list: List[Agent]) -> Agent:
        """Return one agent randomly chosen from the given list of agents.

        Args:
            agent_list: The list of agents.

        Returns:
            A agent randomly chosen from agent_list.
        """
        return random.choice(agent_list) if agent_list else AGENT_NONE
    
    # 最も処刑されそうなエージェントを返す
    def chooseMostlikelyExecuted(self) -> Agent:
        # return self.random_select(self.get_alive_others(self.game_info.agent_list))
        count : defaultdict[Agent, float] = defaultdict(float)

        for talker, target in self.will_vote_reports.items():
            count[target] += 1
        
        if self.vote_candidate != AGENT_NONE:
            count[self.vote_candidate] += 1
        
        return max(count.items(), key=lambda x: x[1])[0]
        

    # 初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        self.game_info = game_info
        self.game_setting = game_setting
        self.me = game_info.me
        # Clear fields not to bring in information from the last game.
        self.comingout_map.clear()
        self.divination_reports.clear()
        self.identification_reports.clear()
        self.will_vote_reports.clear()
        self.talk_list_head = 0
        self.talk_list_all = []

        self.score_matrix = ScoreMatrix(game_info, game_setting, self)
        self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)

        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)

        self.agent_idx_0based = self.me.agent_idx - 1

        Util.debug_print("my role:\t", game_info.my_role)
        Util.debug_print("my idx:\t", self.me.agent_idx-1)

        # 統計
        self.game_count += 1

    # 昼スタート
    def day_start(self) -> None:
        self.talk_list_head = 0
        self.vote_candidate = AGENT_NONE
        self.will_vote_reports.clear()

        Util.debug_print("")
        Util.debug_print("DayStart:\t", self.game_info.day)

        # self.game_info.last_dead_agent_list は昨夜殺されたエージェントのリスト
        # (self.game_info.executed_agent が昨夜処刑されたエージェント)
        killed = self.game_info.last_dead_agent_list
        if len(killed) > 0:
            self.score_matrix.killed(self.game_info, self.game_setting, killed[0])
            Util.debug_print("Killed:\t", self.game_info.last_dead_agent_list[0])
            # 本来複数人殺されることはないが、念のためkilled()は呼び出した上でエラーログを出しておく
            if len(killed) > 1:
                Util.error_print("Killed:\t", *self.game_info.last_dead_agent_list)
        else:
            Util.debug_print("Killed:\t", AGENT_NONE)

        Util.debug_print("Executed:\t", self.game_info.executed_agent)

        for v in self.game_info.vote_list:
            self.score_matrix.vote(self.game_info, self.game_setting, v.agent, v.target, v.day)

    # ゲーム情報の更新
    # talk-listの処理
    def update(self, game_info: GameInfo) -> None:
        self.game_info = game_info  # Update game information.
        for i in range(self.talk_list_head, len(game_info.talk_list)):  # Analyze talks that have not been analyzed yet.
            tk: Talk = game_info.talk_list[i]  # The talk to be analyzed.
            talker: Agent = tk.agent
            self.talk_list_all.append(tk)
            if talker == self.me:  # Skip my talk.
                continue
            if TeamPredictor.should_skip(self, tk):
                continue
            # 内容に応じて更新していく
            content: Content = Content.compile(tk.text)

            # Util.debug_print("Topic:\t", talker, content.topic)

            # content.target が不要な場合デフォルトの AGENT_ANY が入っている
            # 不正な場合はここで弾く
            if content.target == AGENT_NONE or (content.target != AGENT_ANY and content.target.agent_idx > self.N):
                Util.debug_print("Invalid target:\t", content.target)
                continue
            # target が不要なトピック以外で AGENT_ANY が入っている場合は弾く
            if content.target == AGENT_ANY:
                if content.topic not in [Topic.Over, Topic.Skip, Topic.OPERATOR, Topic.AGREE, Topic.DISAGREE, Topic.DUMMY]:
                    Util.debug_print("Invalid target:\t", content.topic, content.target)
                    continue

            if content.topic == Topic.COMINGOUT:
                self.comingout_map[talker] = content.role
                self.score_matrix.talk_co(self.game_info, self.game_setting, talker, content.role)
                Util.debug_print("CO:\t", talker, content.role)
            elif content.topic == Topic.DIVINED:
                self.divination_reports.append(Judge(talker, game_info.day, content.target, content.result))
                self.score_matrix.talk_divined(self.game_info, self.game_setting, talker, content.target, content.result)
                Util.debug_print("DIVINED:\t", talker, content.target, content.result)
            elif content.topic == Topic.IDENTIFIED:
                self.identification_reports.append(Judge(talker, game_info.day, content.target, content.result))
                self.score_matrix.talk_identified(self.game_info, self.game_setting, talker, content.target, content.result)
                Util.debug_print("IDENTIFIED:\t", talker, content.target, content.result)
            elif content.topic == Topic.VOTE:
                # 古い投票先が上書きされる前にスコアを更新 (2回以上投票宣言している場合に信頼度を下げるため)
                self.score_matrix.talk_will_vote(self.game_info, self.game_setting, talker, content.target)
                # 投票先を保存
                self.will_vote_reports[talker] = content.target
            elif content.topic == Topic.VOTED:
                self.score_matrix.talk_voted(self.game_info, self.game_setting, talker, content.target)
            elif content.topic == Topic.GUARDED:
                self.score_matrix.talk_guarded(self.game_info, self.game_setting, talker, content.target)
                Util.debug_print("GUARDED:\t", talker, content.target)
            elif content.topic == Topic.ESTIMATE:
                self.score_matrix.talk_estimate(self.game_info, self.game_setting, talker, content.target, content.role)

        self.talk_list_head = len(game_info.talk_list)  # All done.

    # 会話
    # まだ実装途中です
    def talk(self) -> Content:

        # フルオープンの処理
        if(self.doFO == False) :
            self.doFO = True
            return Content(ComingoutContentBuilder(self.me, Role.VILLAGER))
        
        # 元のコードでの投票先の決定
        # c = 0

        # if (self.N == 5) :
        #     c = self.role_predictor.chooseMostLikely(Role.Werewolf)
        # else :
        #     c = self.chooseMostlikelyExecuted(len(self.game_info.alive_agent_list)*0.7)
        #     if (c == -1) :
        #         c = self.role_predictor.chooseMostLikely(Role.Werewolf)



        # Choose an agent to be voted for while talking.
        #
        # The list of fake seers that reported me as a werewolf.
        # 偽占い
        fake_seers: List[Agent] = [j.agent for j in self.divination_reports
                                   if j.target == self.me and j.result == Species.WEREWOLF]
        # Vote for one of the alive agents that were judged as werewolves by non-fake seers.
        # 偽占い以外の黒結果
        reported_wolves: List[Agent] = [j.target for j in self.divination_reports
                                        if j.agent not in fake_seers and j.result == Species.WEREWOLF]
        # 投票候補：黒結果
        candidates: List[Agent] = self.get_alive_others(reported_wolves)
        # Vote for one of the alive fake seers if there are no candidates.
        # 候補なし → 偽占い
        if not candidates:
            candidates = self.get_alive(fake_seers)
        # Vote for one of the alive agents if there are no candidates.
        # 候補なし → 生存者
        if not candidates:
            candidates = self.get_alive_others(self.game_info.agent_list)
        # Declare which to vote for if not declare yet or the candidate is changed.
        # 候補からランダムセレクト
        # if self.vote_candidate == AGENT_NONE or self.vote_candidate not in candidates:
        #     self.vote_candidate = self.random_select(candidates)
        #     if self.vote_candidate != AGENT_NONE:
        #         return Content(VoteContentBuilder(self.vote_candidate))

        if self.vote_candidate == AGENT_NONE:
            # self.vote_candidate = self.chooseMostlikelyExecuted()
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF)
            if self.vote_candidate != AGENT_NONE:
                return Content(VoteContentBuilder(self.vote_candidate))

        return CONTENT_SKIP
  
    def vote(self) -> Agent:

        # agent_vote_for: Agent = AGENT_NONE
        # if self.N == 5:
        #     agent_vote_for = self.role_predictor.chooseMostLikely(Role.WEREWOLF)
        # else:
        #     agent_vote_for = self.role_predictor.chooseMostLikely(Role.WEREWOLF)
        #     # agent_vote_for = self.chooseMostlikelyExecuted(len(self.game_info.alive_agent_list) * 0.5)
        #     # if agent_vote_for == AGENT_NONE:
        #     #     agent_vote_for = self.role_predictor.chooseMostLikely(Role.WEREWOLF)

        # return agent_vote_for

        return self.vote_candidate

    def attack(self) -> Agent:
        raise NotImplementedError()

    def divine(self) -> Agent:
        raise NotImplementedError()

    def guard(self) -> Agent:
        raise NotImplementedError()

    def whisper(self) -> Content:
        raise NotImplementedError()

    def finish(self) -> None:
        
        # 自分が人狼陣営で人狼が生存しているか、自分が村人陣営で人狼が生存していない場合に勝利
        alive_wolves = [a for a in self.game_info.alive_agent_list if self.game_info.role_map[a] == Role.WEREWOLF]
        is_werewolf_side = self.game_info.my_role in [Role.WEREWOLF, Role.POSSESSED]
        if (len(alive_wolves) > 0) == is_werewolf_side:
            self.win_count += 1

        Util.debug_print("")
        Util.debug_print("win_rate:\t", self.win_count, "/", self.game_count, " = ", self.win_count / self.game_count)
        Util.debug_print("")

        # 確率を表示
        p = self.role_predictor.getProbAll()
        Util.debug_print("", end="\t")
        for r in self.game_info.existing_role_list:
            Util.debug_print(r.name, end="\t")
        Util.debug_print("")
        for a in self.game_info.agent_list:
            Util.debug_print(a, end="\t")
            for r in self.game_info.existing_role_list:
                if p[a][r] > 0.005:
                    Util.debug_print(round(p[a][r], 2), end="\t")
                else:
                    Util.debug_print("-", end="\t")
            Util.debug_print("")
        Util.debug_print("")

        # COを表示
        for a, r in self.comingout_map.items():
            Util.debug_print("CO:\t", a, r)
        Util.debug_print("")

        # 実際の割り当てと予測の割り当てを比較
        assignment = []
        for a, r in self.game_info.role_map.items():
            assignment.append(r)
        actual_assignment = Assignment(self.game_info, self.game_setting, self, assignment)
        predicted_assignment = self.role_predictor.assignments[-1]
        Util.debug_print("\t", "1, 2, 3, 4, 5, 6, 7, 8, 9, A, B, C, D, E, F")
        Util.debug_print("actual:\t", actual_assignment)
        Util.debug_print("predicted:\t", predicted_assignment)
        
        # 一致率を計算
        score = 0
        for i in range(self.N):
            if predicted_assignment[i] == actual_assignment[i]:
                score += 1
            elif predicted_assignment[i] == Role.MEDIUM or actual_assignment[i] == Role.MEDIUM or predicted_assignment[i] == Role.SEER or actual_assignment[i] == Role.SEER:
                Util.debug_print("")
                for r in self.game_info.existing_role_list:
                    Util.debug_print(self.game_info.agent_list[i], "\t", r, "\t", round(self.role_predictor.getProb(i, r), 2))
        
        self.sum_score += score
        Util.debug_print("")
        Util.debug_print("score:\t", score, "/", self.N)
        Util.debug_print("rate:\t", self.sum_score, "/", self.game_count, "=", round(self.sum_score / self.game_count, 2))
        Util.debug_print("")

        # 実際の割り当てが予測の割り当てに含まれていたのか
        Util.debug_print("in role_predictor.assignments:\t", actual_assignment in self.role_predictor.assignments)
        Util.debug_print("in role_predictor.assignments(set):\t", actual_assignment in self.role_predictor.assignments_set)
        Util.debug_print("len(role_predictor.assignments(set):\t", len(self.role_predictor.assignments_set))
        Util.debug_print("")

        # もし含まれていないなら、含まれていたときのスコアを表示
        if actual_assignment not in self.role_predictor.assignments:
            actual_assignment.evaluate(self.score_matrix)
        
        # 予測の割り当てのスコアを表示 (デバッグモード)
        predicted_assignment.evaluate(self.score_matrix, debug=True)
        Util.debug_print("")
        Util.debug_print("best score:\t", round(predicted_assignment.score, 4))
        Util.debug_print("")

        # 実際の割り当てのスコアを表示 (デバッグモード)
        actual_assignment.evaluate(self.score_matrix, debug=True)
        Util.debug_print("")
        Util.debug_print("actual score:\t", round(actual_assignment.score, 4))

        # 最下位の割り当てのスコアを表示
        Util.debug_print("")
        Util.debug_print("worst score:\t", round(self.role_predictor.assignments[0].score, 4))
        Util.debug_print("")

        # COしていない人から占い師、霊媒師、狩人が選ばれてはいないかのチェック
        for a in self.game_info.agent_list:
            if predicted_assignment[a] in [Role.SEER, Role.MEDIUM, Role.BODYGUARD] and (a not in self.comingout_map or predicted_assignment[a] != self.comingout_map[a]):
                Util.debug_print(a, "CO", self.comingout_map[a] if a in self.comingout_map else Role.UNC, "but assigned", predicted_assignment[a])
        Util.debug_print("")

        pass
