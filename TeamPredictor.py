from aiwolf import Agent, GameInfo, GameSetting, Talk, Topic, Content
from const import CONTENT_SKIP
from aiwolf.constant import (AGENT_NONE, AGENT_ANY, AGENT_UNSPEC)

from enum import Enum
from aiwolf import Role

from Util import Util

from collections import defaultdict

class Team(Enum):
    ddhb = "ddhb"
    Jawa = "Jawa"
    ooh = "ooh"
    daphne = "daphne"
    ioh = "ioh"
    hiro = "hiro"
    mikami = "mikami"
    Camellia = "Camellia"
    aitel2020 = "aitel2020"
    kokuto = "kokuto"
    inaba = "inaba"
    HO = "HO"
    wasabi = "wasabi"
    t222364m = "t222364m"
    MayQueen = "MayQueen"
    UNC = "UNC"

class TeamPredictor:

    game_info: GameInfo
    game_setting: GameSetting

    team_map: "defaultdict[Agent, Team]"

    @staticmethod
    def init():
        TeamPredictor.team_map = defaultdict(lambda: Team.UNC)

    @staticmethod
    def update(game_info: GameInfo):
        TeamPredictor.game_info = game_info
        pass

    @staticmethod
    def should_skip(player, talk: Talk) -> bool:
        if TeamPredictor.team_map[talk.agent] in [Team.daphne, Team.t222364m]:
            return True
        return False

    @staticmethod
    def finish(player):
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

            if content.topic == Topic.COMINGOUT:
                co_map[talker] = content.role
                if co_map[talker] == Role.SEER and actual_role_map[talker] in [Role.VILLAGER, Role.BODYGUARD, Role.MEDIUM]:
                    # 占い師COしている村人陣営は t222364m と判定
                    if TeamPredictor.team_map[talker] == Team.UNC:
                        TeamPredictor.team_map[talker] = Team.t222364m

            if actual_role_map[talker] in [Role.VILLAGER, Role.BODYGUARD, Role.MEDIUM] and content.topic == Topic.DIVINED:
                if co_map[talker] == Role.UNC:
                    # 何もCOしていないのに占い結果を報告している村人陣営は daphne と判定
                    TeamPredictor.team_map[talker] = Team.daphne

            if TeamPredictor.team_map[talker] == Team.UNC: # 正体不明のリア狂を探す

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

                # todo: 一日に何度も占い・霊媒報告

        Util.debug_print("TeamPredictor")
        for agent in game_info.agent_list:
            Util.debug_print(agent, "\t", TeamPredictor.team_map[agent])
        Util.debug_print("")