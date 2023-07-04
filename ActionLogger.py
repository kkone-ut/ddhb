from aiwolf import Agent, GameInfo, GameSetting, Role, Talk, Content, Topic, Species, Operator

from enum import Enum
from typing import List
from collections import defaultdict

class Action(Enum):
    DIVINED_BLACK = "DIVINED_BLACK"
    DIVINED_WHITE = "DIVINED_WHITE"
    DIVINED_WITHOUT_CO = "DIVINED_WITHOUT_CO"
    IDENTIFIED_BLACK = "IDENTIFIED_BLACK"
    IDENTIFIED_WHITE = "IDENTIFIED_WHITE"
    IDENTIFIED_WITHOUT_CO = "IDENTIFIED_WITHOUT_CO"
    GUARDED = "GUARDED"
    VOTE = "VOTE"
    CO_SEER = "CO_SEER"
    CO_MEDIUM = "CO_MEDIUM"
    CO_BODYGUARD = "CO_BODYGUARD"
    REQUEST_VOTE = "REQUEST_VOTE"
    ESTIMATE_WEREWOLF = "ESTIMATE_WEREWOLF"
    ESTIMATE_POSSESSED = "ESTIMATE_POSSESSED"
    ESTIMATE_VILLAGER = "ESTIMATE_VILLAGER"

class ActionLooger:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting):
        self.game_info = game_info
        self.game_setting = game_setting
        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)
        self.MAX_DAY = self.N
        self.MAX_TURN = 20
        # day, turn, agent, action
        self.action_count: "defaultdict[(int, int, Agent, Action), int]" = defaultdict(int)
        # day, turn, agent, role, action
        self.action_count_all: "defaultdict[(int, int, Agent, Role, Action), int]" = defaultdict(int)

    def update(self, content: Content):
        talker: Agent = content.agent
        day: int = content.day
        turn: int = content.turn
        action: Action = self.get_action(content)
        if action is not None:
            self.action_count[(day, turn, talker, action)] += 1

    def get_score(self) -> float:
        pass
    
    def finish(self):
        for a, r in self.game_info.role_map.items():
            for d in range(1, self.MAX_DAY + 1):
                for t in range(self.MAX_TURN):
                    for action in Action:
                        self.action_count_all[(d, t, a, r, action)] = self.action_count[(d, t, a, action)]
    
    def get_action(self, content: Content) -> Action:
        if content.topic == Topic.DIVINED:
            if content.result == Species.WEREWOLF:
                return Action.DIVINED_BLACK
            elif content.result == Species.HUMAN:
                return Action.DIVINED_WHITE
        elif content.topic == Topic.IDENTIFIED:
            if content.result == Species.WEREWOLF:
                return Action.IDENTIFIED_BLACK
            elif content.result == Species.HUMAN:
                return Action.IDENTIFIED_WHITE
        elif content.topic == Topic.GUARDED:
            return Action.GUARDED
        elif content.topic == Topic.VOTE:
            return Action.VOTE
        elif content.topic == Topic.COMINGOUT:
            if content.role == Role.SEER:
                return Action.CO_SEER
            elif content.role == Role.MEDIUM:
                return Action.CO_MEDIUM
            elif content.role == Role.BODYGUARD:
                return Action.CO_BODYGUARD
        elif content.topic == Topic.OPERATOR:
            if content.operator == Operator.REQUEST and content.content_list[0].topic == Topic.VOTE:
                return Action.REQUEST_VOTE
        elif content.topic == Topic.ESTIMATE:
            if content.role == Role.WEREWOLF:
                return Action.ESTIMATE_WEREWOLF
            elif content.role == Role.POSSESSED:
                return Action.ESTIMATE_POSSESSED
            elif content.role == Role.VILLAGER:
                return Action.ESTIMATE_VILLAGER
        return None