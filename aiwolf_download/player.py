#
# player.py
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
"""player module."""
from abc import ABC, abstractmethod

from aiwolf.agent import Agent
from aiwolf.content import Content
from aiwolf.gameinfo import GameInfo
from aiwolf.gamesetting import GameSetting


# 抽象クラス：継承を前提としたクラス
# 抽象メソッド：実装を持たないメソッド
class AbstractPlayer(ABC):
    """Abstract class that defines the functions every player agents must have."""

    @abstractmethod
    # 人狼の襲撃先
    def attack(self) -> Agent:
        """Return the agent this werewolf wants to attack.

        The agent that does not exist means not wanting to attack any other.

        Returns:
            The agent this werewolf wants to attack.
        """
        pass

    @abstractmethod
    # 昼スタート
    def day_start(self) -> None:
        """Called when the day starts."""
        pass

    @abstractmethod
    # 占い対象
    def divine(self) -> Agent:
        """Return the agent this seer wants to divine.

        The agent that does not exist means no divination.

        Returns:
            The agent this seer wants to divine.
        """
        pass

    @abstractmethod
    # 夜終了
    def finish(self) -> None:
        """Called when the game finishes."""
        pass

    # エージェントの名前
    def get_name(self) -> str:
        """Return this player's name.

        Returns:
            This player's name.
        """
        return type(self).__name__

    @abstractmethod
    # 狩人の護衛先
    def guard(self) -> Agent:
        """Return the agent this bodyguard wants to guard.

        The agent that does not exist means no guard.        

        Returns:
            The agent this bodyguard wants to guard.
        """
        pass

    @abstractmethod
    # ゲーム初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        """Called when the game starts."""
        pass

    @abstractmethod
    # 会話内容
    def talk(self) -> Content:
        """Return this player's talk.

        Returns:
            This player's talk.
        """
        pass

    @abstractmethod
    # ゲーム情報の更新
    def update(self, game_info: GameInfo) -> None:
        """Called when the game information is updated."""
        pass

    @abstractmethod
    # 投票先
    def vote(self) -> Agent:
        """Return the agent this player wants to exclude from this game.

        Returning the agent that does not exist results in ramdom vote.

        Returns:
            The agent this player wants to exclude from this game.
        """
        pass

    @abstractmethod
    # 囁き内容
    def whisper(self) -> Content:
        """Return this player's whisper.

        Returns:
            This player's whisper.
        """
        pass
