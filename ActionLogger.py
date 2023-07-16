from aiwolf import Agent, GameInfo, GameSetting, Role, Talk, Content, Topic, Species, Operator

from enum import Enum
from typing import List, DefaultDict, Dict, Deque
from collections import defaultdict, deque
from Util import Util


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
    OTHER = "OTHER"

class ActionLog:
    game: int
    day: int
    turn: int
    agent: Agent
    action: Action
    def __init__(self, game: int, day: int, turn: int, agent: Agent, action: Action):
        self.game = game
        self.day = day
        self.turn = turn
        self.agent = agent
        self.action = action


class ActionLogger:

    game_info: GameInfo
    game_setting: GameSetting
    N: int
    M: int
    MAX_DAY: int
    MAX_TURN: int
    action_log: Deque[ActionLog]
    action_count_all: "DefaultDict[(int, int, Agent, Role, Action), int]"
    old_role_map: List[Dict[Agent, Role]]

    @staticmethod
    def init():
        ActionLogger.action_log: Deque[ActionLog] = deque()
        # day, turn, agent, role, action
        ActionLogger.action_count_all: "DefaultDict[(int, int, Agent, Role, Action), int]" = defaultdict(int)
        ActionLogger.old_role_map: List[Dict[Agent, Role]] = []
    
    @staticmethod
    def initialize(game_info: GameInfo, game_setting: GameSetting):
        ActionLogger.game_info = game_info
        ActionLogger.game_setting = game_setting
        ActionLogger.N = game_setting.player_num
        ActionLogger.M = len(game_info.existing_role_list)
        ActionLogger.MAX_DAY = ActionLogger.N
        ActionLogger.MAX_TURN = 20


    @staticmethod
    def update(game_info: GameInfo, talk: Talk, content: Content) -> Action:
        ActionLogger.game_info = game_info
        talker: Agent = talk.agent
        day: int = talk.day
        turn: int = talk.turn
        action: Action = ActionLogger.get_action(content)
        if action is not None:
            # ActionLogger.action_count[(day, turn, talker, action)] += 1
            ActionLogger.action_log.append(ActionLog(Util.game_count, day, turn, talker, action))

        # 前ゲームのログを action_count_all に反映
        # 現在のターンまでのもののみを処理することで負担を減らす
        oldest_log: ActionLog = ActionLogger.action_log[0] if len(ActionLogger.action_log) > 0 else None
        while oldest_log is not None and oldest_log.game < Util.game_count and (oldest_log.day <= day or (oldest_log.day == day and oldest_log.turn == turn)):
            role: Role = ActionLogger.old_role_map[oldest_log.game-1][oldest_log.agent]
            # Util.debug_print("ActionLogger.update", f"game={oldest_log.game}, day={oldest_log.day}, turn={oldest_log.turn}, agent={oldest_log.agent}, role={role}, action={oldest_log.action}")
            ActionLogger.action_count_all[(oldest_log.day, oldest_log.turn, oldest_log.agent, role, oldest_log.action)] += 1
            ActionLogger.action_log.popleft()
            if len(ActionLogger.action_log) > 0:
                oldest_log = ActionLogger.action_log[0]
        
        return action


    @staticmethod
    def get_score(d: int, t: int, talker: Agent, action:Action) -> DefaultDict[Role, float]:
        count: DefaultDict[Role, float] = defaultdict(float)
        score: DefaultDict[Role, float] = defaultdict(float)
        sum = 0

        if t >= 4:
            return score

        for r in ActionLogger.game_info.existing_role_list:
            if Util.agent_role_count[talker][r] == 0:
                count[r] = 0
            else:
                # count[r] = ActionLogger.action_count_all[(d, t, talker, r, action)]
                count[r] = ActionLogger.action_count_all[(d, t, talker, r, action)] / Util.agent_role_count[talker][r]
            sum += count[r]
        
        if sum > 0:
            for r in ActionLogger.game_info.existing_role_list:
                score[r] = count[r] / sum

        return score


    @staticmethod
    def finish(game_info: GameInfo):
        ActionLogger.old_role_map.append(game_info.role_map)


    @staticmethod
    def get_action(content: Content) -> Action:
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
        return Action.OTHER