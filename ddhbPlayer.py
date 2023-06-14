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

from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role, Topic
from const import CONTENT_SKIP

from ddhbBodyguard import ddhbBodyguard
from ddhbMedium import ddhbMedium
from ddhbPossessed import ddhbPossessed
from ddhbSeer import ddhbSeer
from ddhbVillager import ddhbVillager
from ddhbWerewolf import ddhbWerewolf

from Util import Util
from TeamPredictor import TeamPredictor

import library.timeout_decorator as timeout_decorator

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

        self.game_setting: GameSetting = None
        self.game_info: GameInfo = None

    # オーバーライドしていく
    def attack(self) -> Agent:
        return self.player.attack()

    def day_start(self) -> None:
        self.player.day_start()

    def divine(self) -> Agent:
        return self.player.divine()

    def finish(self) -> None:
        self.player.finish()
        TeamPredictor.finish(self.player)
        
        Util.debug_print("finish")
        Util.debug_print("---------")
        Util.debug_print("")

    def guard(self) -> Agent:
        return self.player.guard()

    # 役職の初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        self.game_setting = game_setting
        self.game_info = game_info

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

        TeamPredictor.init(game_info, game_setting)

    @timeout_decorator.timeout(0.08)
    def _talk(self) -> Content:
        self.player.role_predictor.update(self.game_info, self.game_setting)
        content = self.player.talk()
        if content.topic != Topic.Skip:
            Util.debug_print("My Topic:\t", content.text)
        return content
        
    def talk(self) -> Content:
        try:
            return self._talk()
        except timeout_decorator.TimeoutError:
            Util.error_print("TimeoutError:\t", "talk", 80, "ms")
            return CONTENT_SKIP

    @timeout_decorator.timeout(0.08)
    def _update(self, game_info: GameInfo) -> None:
        self.game_info = game_info
        TeamPredictor.update(game_info)
        self.player.update(game_info)

    def update(self, game_info: GameInfo) -> None:
        try:
            self._update(game_info)
        except timeout_decorator.TimeoutError:
            Util.error_print("TimeoutError:\t", "update", 80, "ms")

    def vote(self) -> Agent:
        return self.player.vote()

    def whisper(self) -> Content:
        return self.player.whisper()
