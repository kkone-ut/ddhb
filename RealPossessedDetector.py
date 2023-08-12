from aiwolf import Agent, GameInfo, GameSetting, Talk, Topic, Content
from const import CONTENT_SKIP
from aiwolf.constant import (AGENT_NONE, AGENT_ANY, AGENT_UNSPEC)

from enum import Enum
from aiwolf import Role

from Util import Util

from collections import defaultdict
from typing import List
from ddhbVillager import *

class RealPossessedDetector:

    game_info: GameInfo
    game_setting: GameSetting

    real_possesssed: List[Agent]

    @staticmethod
    def init():
        RealPossessedDetector.real_possesssed = []

    @staticmethod
    def update(game_info: GameInfo):
        RealPossessedDetector.game_info = game_info
        pass

    @staticmethod
    def should_skip(player, talk: Talk) -> bool:
        # return talk.agent in RealPossessedDetector.real_possesssed
        return False # 検出するだけでスキップはしない

    @staticmethod
    def finish(player):
        player: ddhbVillager = player
        game_info: GameInfo = player.game_info
        game_setting: GameSetting = player.game_setting

        co_map: "defaultdict[Agent, Role]" = defaultdict(lambda: Role.UNC)
        actual_role_map: "defaultdict[Agent, Role]" = game_info.role_map

        for tk in player.talk_list_all:
            talker: Agent = tk.agent
            if talker == player.me:  # Skip my talk.
                continue
            # 内容に応じて更新していく
            content: Content = Content.compile(tk.text)

            # 占い師以外の村人陣営が占い師を騙っている場合
            if actual_role_map[talker] in [Role.VILLAGER, Role.BODYGUARD, Role.MEDIUM]:
                if content.topic == Topic.DIVINED or (content.topic == Topic.COMINGOUT and content.role == Role.SEER):
                    Util.error_print("Real Possessed ", "Not Seer", talker)
            
            # 霊媒師以外の村人陣営が霊媒師を騙っている場合
            if actual_role_map[talker] in [Role.VILLAGER, Role.BODYGUARD, Role.SEER]:
                if content.topic == Topic.IDENTIFIED or (content.topic == Topic.COMINGOUT and content.role == Role.MEDIUM):
                    Util.error_print("Real Possessed ", "Not Medium", talker)
            
            # 狩人以外の村人陣営が狩人を騙っている場合
            if actual_role_map[talker] in [Role.VILLAGER, Role.SEER, Role.MEDIUM]:
                if content.topic == Topic.GUARDED or (content.topic == Topic.COMINGOUT and content.role == Role.BODYGUARD):
                    Util.error_print("Real Possessed ", "Not Bodyguard", talker)

        Util.debug_print("Real Possessed:\t", player.convert_to_agentids(RealPossessedDetector.real_possesssed), "\n")