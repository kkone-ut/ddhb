#
# ddhbPlyaer.py
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

from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role

from ddhbBodyguard import ddhbBodyguard
from ddhbMedium import ddhbMedium
from ddhbPossessed import ddhbPossessed
from ddhbSeer import ddhbSeer
from ddhbVillager import ddhbVillager
from ddhbWerewolf import ddhbWerewolf


# ddhbプレイヤー
class ddhbPlayer(AbstractPlayer):

    villager: AbstractPlayer
    bodyguard: AbstractPlayer
    medium: AbstractPlayer
    seer: AbstractPlayer
    possessed: AbstractPlayer
    werewolf: AbstractPlayer
    player: AbstractPlayer

    def __init__(self) -> None:
        self.villager = ddhbVillager()
        self.bodyguard = ddhbBodyguard()
        self.medium = ddhbMedium()
        self.seer = ddhbSeer()
        self.possessed = ddhbPossessed()
        self.werewolf = ddhbWerewolf()
        self.player = self.villager

    # オーバーライドしていく
    def attack(self) -> Agent:
        return self.player.attack()

    def day_start(self) -> None:
        self.player.day_start()

    def divine(self) -> Agent:
        return self.player.divine()

    def finish(self) -> None:
        self.player.finish()

    def guard(self) -> Agent:
        return self.player.guard()

    # 役職の初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        role: Role = game_info.my_role
        if role == Role.VILLAGER:
            self.player = self.villager
        elif role == Role.BODYGUARD:
            self.player = self.bodyguard
        elif role == Role.MEDIUM:
            self.player = self.medium
        elif role == Role.SEER:
            self.player = self.seer
        elif role == Role.POSSESSED:
            self.player = self.possessed
        elif role == Role.WEREWOLF:
            self.player = self.werewolf
        self.player.initialize(game_info, game_setting)

    def talk(self) -> Content:
        return self.player.talk()

    def update(self, game_info: GameInfo) -> None:
        self.player.update(game_info)

    def vote(self) -> Agent:
        return self.player.vote()

    def whisper(self) -> Content:
        return self.player.whisper()
