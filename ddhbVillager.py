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
from typing import Dict, List

from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic,
                    VoteContentBuilder)
from aiwolf.constant import AGENT_NONE

from const import CONTENT_SKIP

from Util import Util
from ScoreMatrix import ScoreMatrix
from RolePredictor import RolePredictor

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
    talk_list_head: int # talkのインデックス
    """Index of the talk to be analysed next."""

    def __init__(self) -> None:
        """Initialize a new instance of ddhbVillager."""

        self.me = AGENT_NONE
        self.vote_candidate = AGENT_NONE
        self.game_info = None  # type: ignore
        self.comingout_map = {}
        self.divination_reports = []
        self.identification_reports = []
        self.talk_list_head = 0

        self.role_predictor = None

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

    # 初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        self.game_info = game_info
        self.game_setting = game_setting
        self.me = game_info.me
        # Clear fields not to bring in information from the last game.
        self.comingout_map.clear()
        self.divination_reports.clear()
        self.identification_reports.clear()

        self.score_matrix = ScoreMatrix(game_info, game_setting, self)
        self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)

        print(game_info.my_role)

    # 昼スタート
    def day_start(self) -> None:
        self.talk_list_head = 0
        self.vote_candidate = AGENT_NONE

        # self.game_info.last_dead_agent_list は昨夜殺されたエージェントのリスト
        # (self.game_info.executed_agent が昨夜処刑されたエージェント)
        killed = self.game_info.last_dead_agent_list
        if len(killed) > 0:
            self.score_matrix.killed(self.game_info, self.game_setting, killed[0])
            Util.debug_print("Killed:", self.game_info.last_dead_agent_list[0])
            # 本来複数人殺されることはないが、念のためkilled()は呼び出した上でエラーログを出しておく
            if len(killed) > 1:
                Util.error_print("Killed:", *self.game_info.last_dead_agent_list)
        else:
            Util.debug_print("Killed: None")

    # ゲーム情報の更新
    # talk-listの処理
    def update(self, game_info: GameInfo) -> None:
        self.game_info = game_info  # Update game information.
        for i in range(self.talk_list_head, len(game_info.talk_list)):  # Analyze talks that have not been analyzed yet.
            tk: Talk = game_info.talk_list[i]  # The talk to be analyzed.
            talker: Agent = tk.agent
            if talker == self.me:  # Skip my talk.
                continue
            # 内容に応じて更新していく
            content: Content = Content.compile(tk.text)
            if content.topic == Topic.COMINGOUT:
                self.comingout_map[talker] = content.role
                self.score_matrix.talk_co(self.game_info, self.game_setting, talker, content.role)
            elif content.topic == Topic.DIVINED:
                self.divination_reports.append(Judge(talker, game_info.day, content.target, content.result))
                self.score_matrix.talk_divined(self.game_info, self.game_setting, talker, content.target, content.result)
            elif content.topic == Topic.IDENTIFIED:
                self.identification_reports.append(Judge(talker, game_info.day, content.target, content.result))
                self.score_matrix.talk_identified(self.game_info, self.game_setting, talker, content.target, content.result)
        self.talk_list_head = len(game_info.talk_list)  # All done.

    # 会話
    def talk(self) -> Content:
        self.role_predictor.update(self.game_info, self.game_setting)
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
        if self.vote_candidate == AGENT_NONE or self.vote_candidate not in candidates:
            self.vote_candidate = self.random_select(candidates)
            if self.vote_candidate != AGENT_NONE:
                return Content(VoteContentBuilder(self.vote_candidate))
        return CONTENT_SKIP

    def vote(self) -> Agent:
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me

    def attack(self) -> Agent:
        raise NotImplementedError()

    def divine(self) -> Agent:
        raise NotImplementedError()

    def guard(self) -> Agent:
        raise NotImplementedError()

    def whisper(self) -> Content:
        raise NotImplementedError()

    def finish(self) -> None:
        pass
