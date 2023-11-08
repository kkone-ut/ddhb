import traceback

from const import CONTENT_SKIP
from ddhbBodyguard import ddhbBodyguard
from ddhbMedium import ddhbMedium
from ddhbSeer import ddhbSeer
from ddhbVillager import ddhbVillager
from ddhbWerewolf import ddhbWerewolf
from RealPossessedDetector import RealPossessedDetector
from Util import Util

from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Role, Status, Topic)
from aiwolf.constant import AGENT_NONE
from ddhbPossessed import ddhbPossessed


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
        RealPossessedDetector.init()

    # オーバーライドしていく
    def attack(self) -> Agent:
        agent = AGENT_NONE
        try:
            Util.start_timer("ddhbPlayer.attack")
            agent = self.player.attack()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.attack", 70)
        except Exception:
            Util.end_timer("ddhbPlayer.attack")
            Util.error_print("Trace:\t", traceback.format_exc())
        if agent is None:
            agent = AGENT_NONE
        return agent

    def day_start(self) -> None:
        self.player.talk_turn = 0
        try:
            Util.start_timer("ddhbPlayer.day_start")
            self.player.day_start()
            # 自分が死んでいる場合は次のゲーム開始時の ROLE までリクエストが来ないため、ある程度時間をかけても問題ない
            # finish のときに RolePredictor を更新していると次のゲーム開始に間に合わない
            if self.game_info.status_map[self.player.me] == Status.DEAD:
                self.player.role_predictor.update(self.game_info, self.game_setting)
            Util.end_timer("ddhbPlayer.day_start", 10)
        except Exception:
            Util.end_timer("ddhbPlayer.day_start")
            Util.error_print("Trace:\t", traceback.format_exc())

    def divine(self) -> Agent:
        agent = AGENT_NONE
        try:
            Util.start_timer("ddhbPlayer.divine")
            agent = self.player.divine()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.divine", 70)
        except Exception:
            Util.end_timer("ddhbPlayer.divine")
            Util.error_print("Trace:\t", traceback.format_exc())
        if agent is None:
            agent = AGENT_NONE
        return agent

    def finish(self) -> None:
        Util.start_timer("ddhbPlayer.finish")
        try:
            self.player.finish()
            RealPossessedDetector.finish(self.player)
            Util.end_timer("ddhbPlayer.finish", 20)
        except Exception:
            Util.error_print(traceback.format_exc())
        Util.debug_print("finish")
        Util.debug_print("---------------------------------------")
        Util.debug_print("")

    def guard(self) -> Agent:
        agent = AGENT_NONE
        try:
            Util.start_timer("ddhbPlayer.guard")
            agent = self.player.guard()
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.guard", 70)
        except Exception:
            Util.end_timer("ddhbPlayer.guard")
            Util.error_print("Trace:\t", traceback.format_exc())
        if agent is None:
            agent = AGENT_NONE
        return agent

    # 役職の初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        try:
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
            Util.end_timer("ddhbPlayer.initialize", 20)
        except Exception:
            Util.end_timer("ddhbPlayer.initialize")
            Util.error_print("Trace:\t", traceback.format_exc())

    def talk(self) -> Content:
        content = CONTENT_SKIP
        self.player.talk_turn += 1
        try:
            Util.start_timer("ddhbPlayer.talk")
            Util.start_timer("ddhbPlayer.talk.update")
            self.player.role_predictor.update(self.game_info, self.game_setting, 30)
            Util.end_timer("ddhbPlayer.talk.update", 40)
            content = self.player.talk()
            if content.topic != Topic.Skip:
                # Util.debug_print("W--------------:\t", self.player.role_predictor.chooseMostLikely_demo(Role.WEREWOLF, self.player.get_alive_others(self.game_info.agent_list)))
                Util.debug_print("My Topic:\t", content.text)
            Util.start_timer("ddhbPlayer.talk.addAssignments")
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 30)
            Util.end_timer("ddhbPlayer.talk.addAssignments", 40)
            Util.end_timer("ddhbPlayer.talk", 80)
        except Exception:
            Util.end_timer("ddhbPlayer.talk")
            Util.error_print("Trace:\t", traceback.format_exc())
        if content is None:
            content = CONTENT_SKIP
        return content

    def update(self, game_info: GameInfo) -> None:
        Util.start_timer("ddhbPlayer.update")
        try:
            self.game_info = game_info
            RealPossessedDetector.update(game_info)
            self.player.update(game_info)
            Util.end_timer("ddhbPlayer.update", 10)
        except Exception:
            Util.end_timer("ddhbPlayer.update")
            Util.error_print("Trace:\t", traceback.format_exc())

    def vote(self) -> Agent:
        Util.start_timer("ddhbPlayer.vote")
        agent = AGENT_NONE
        try:
            agent = self.player.vote()
            Util.debug_print("----------")
            Util.debug_print("My Vote:\t", agent)
            Util.debug_print("----------")
            self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 60)
            Util.end_timer("ddhbPlayer.vote", 80)
        except Exception:
            Util.end_timer("ddhbPlayer.vote")
            Util.error_print("Trace:\t", traceback.format_exc())
        if agent is None:
            agent = AGENT_NONE
        return agent

    def whisper(self) -> Content:
        content = CONTENT_SKIP
        try:
            Util.start_timer("ddhbPlayer.whisper")
            content = self.player.whisper()
            # 人狼陣営の場合は割り当てはそこまで探索しなくてもいい
            # self.player.role_predictor.addAssignments(self.game_info, self.game_setting, 30)
            Util.end_timer("ddhbPlayer.whisper", 20)
        except Exception:
            Util.end_timer("ddhbPlayer.whisper")
            Util.error_print("Trace:\t", traceback.format_exc())
        if content is None:
            content = CONTENT_SKIP
        return content
