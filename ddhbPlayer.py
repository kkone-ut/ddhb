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

from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role, Topic, Status
from const import CONTENT_SKIP
from aiwolf.constant import AGENT_NONE

from ddhbBodyguard import ddhbBodyguard
from ddhbMedium import ddhbMedium
from ddhbPossessed import ddhbPossessed
from ddhbSeer import ddhbSeer
from ddhbVillager import ddhbVillager
from ddhbWerewolf import ddhbWerewolf

from Util import Util
from TeamPredictor import TeamPredictor

import library.timeout_decorator as timeout_decorator
import time
import traceback

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

        TeamPredictor.init()

    # オーバーライドしていく
    def attack(self) -> Agent:
        Util.start_timer("ddhbPlayer.attack")
        agent = AGENT_NONE
        try:
            agent = self.player.attack()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.attack")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.attack")
            Util.error_print("TimeoutError:\t", "attack")
        return agent

    def day_start(self) -> None:
        Util.start_timer("ddhbPlayer.day_start")
        try:
            self.player.day_start()
            # 自分が死んでいる場合は次のゲーム開始時の ROLE までリクエストが来ないため、ある程度時間をかけても問題ない
            # finish のときに RolePredictor を更新していると次のゲーム開始に間に合わない
            if self.game_info.status_map[self.player.me] == Status.DEAD:
                self.player.role_predictor.update(self.game_info, self.game_setting)
            Util.end_timer("ddhbPlayer.day_start")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.day_start")
            Util.error_print("TimeoutError:\t", "day_start")

    def divine(self) -> Agent:
        Util.start_timer("ddhbPlayer.divine")
        agent = AGENT_NONE
        try:
            agent = self.player.divine()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.divine")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.divine")
            Util.error_print("TimeoutError:\t", "divine")
        return agent

    def finish(self) -> None:
        Util.start_timer("ddhbPlayer.finish")
        try:
            self.player.finish()
            TeamPredictor.finish(self.player)
            Util.end_timer("ddhbPlayer.finish")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.finish")
            Util.error_print("TimeoutError:\t", "finish")
        except Exception as e:
            Util.error_print(traceback.format_exc())
        Util.debug_print("finish")
        Util.debug_print("---------")
        Util.debug_print("")

    def guard(self) -> Agent:
        Util.start_timer("ddhbPlayer.guard")
        agent = AGENT_NONE
        try:
            agent = self.player.guard()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.guard")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.guard")
            Util.error_print("TimeoutError:\t", "guard")
        return agent

    # 役職の初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        Util.start_timer("ddhbPlayer.initialize")
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
        Util.end_timer("ddhbPlayer.initialize", 40)
        
    def talk(self) -> Content:
        Util.start_timer("ddhbPlayer.talk")
        content = CONTENT_SKIP
        try:
            Util.start_timer("ddhbPlayer.talk.update")
            self.player.role_predictor.update(self.game_info, self.game_setting, 30)
            Util.end_timer("ddhbPlayer.talk.update")
            content = self.player.talk()
            if content.topic != Topic.Skip:
                Util.debug_print("My Topic:\t", content.text)
            Util.start_timer("ddhbPlayer.talk.addAssignments")
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 30)
            Util.end_timer("ddhbPlayer.talk.addAssignments")
            Util.end_timer("ddhbPlayer.talk")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.talk")
            Util.error_print("TimeoutError:\t", "talk")
        return content

    @timeout_decorator.timeout(0.02)
    def _update(self, game_info: GameInfo) -> None:
        self.game_info = game_info
        TeamPredictor.update(game_info)
        self.player.update(game_info)

    def update(self, game_info: GameInfo) -> None:
        Util.start_timer("ddhbPlayer.update")
        try:
            self._update(game_info)
            Util.end_timer("ddhbPlayer.update")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.update")
            Util.error_print("TimeoutError:\t", "update")

    def vote(self) -> Agent:
        Util.start_timer("ddhbPlayer.vote")
        agent = AGENT_NONE
        try:
            agent = self.player.vote()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.vote")
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.vote")
            Util.error_print("TimeoutError:\t", "vote")
        return agent

    def whisper(self) -> Content:
        Util.start_timer("ddhbPlayer.whisper")
        content = CONTENT_SKIP
        try:
            content = self.player.whisper()
            # self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
        except timeout_decorator.TimeoutError:
            Util.end_timer("ddhbPlayer.whisper")
            Util.error_print("TimeoutError:\t", "whisper")
        return content
