from collections import defaultdict, deque
from enum import Enum
from typing import DefaultDict, Deque, Dict, List

import numpy as np
from Util import Util

from aiwolf import (Agent, Content, GameInfo, GameSetting, Judge, Operator,
                    Role, Species, Talk, Topic)


class Action(Enum):
    DIVINED_BLACK = "DIVINED_BLACK"
    DIVINED_WHITE = "DIVINED_WHITE"
    DIVINED_WITHOUT_CO = "DIVINED_WITHOUT_CO"
    DIVINED_CONTRADICT = "DIVINED_CONTRADICT" # 同じ日に対抗と被っていて、かつ、対抗と逆の結果を主張した場合
    IDENTIFIED_BLACK = "IDENTIFIED_BLACK"
    IDENTIFIED_WHITE = "IDENTIFIED_WHITE"
    IDENTIFIED_WITHOUT_CO = "IDENTIFIED_WITHOUT_CO"
    IDENTIFIED_WITHOUT_CO_TO_COUNTERPART = "IDENTIFIED_WITHOUT_CO_TO_COUNTERPART" # 霊媒CO以外のCOをしていて、対抗に霊媒黒出しした場合
    IDENTIFIED_TO_ALIVE = "IDENTIFIED_TO_ALIVE"
    GUARDED = "GUARDED"
    VOTE = "VOTE"
    CO_SEER = "CO_SEER"
    CO_MEDIUM = "CO_MEDIUM"
    CO_BODYGUARD = "CO_BODYGUARD"
    CO_VILLAGER = "CO_VILLAGER"
    REQUEST_VOTE = "REQUEST_VOTE"
    REQUEST_COMINGOUT = "REQUEST_COMINGOUT"
    ESTIMATE_WEREWOLF = "ESTIMATE_WEREWOLF"
    ESTIMATE_POSSESSED = "ESTIMATE_POSSESSED"
    ESTIMATE_VILLAGER = "ESTIMATE_VILLAGER"
    SHOW_HOSTILITY = "SHOW_HOSTILITY"
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
    action_count_sum: "DefaultDict[(Agent, Role, Action), int]"
    old_role_map: List[Dict[Agent, Role]]

    @staticmethod
    def init():
        ActionLogger.action_log: Deque[ActionLog] = deque()
        # day, turn, agent, role, action
        ActionLogger.action_count_all: "DefaultDict[(int, int, Agent, Role, Action), int]" = defaultdict(int)
        # agent, role, action
        ActionLogger.action_count_sum: "DefaultDict[(Agent, Role, Action), int]" = defaultdict(int)
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
    def update(game_info: GameInfo, talk: Talk, content: Content, player) -> Action:
        ActionLogger.game_info = game_info
        talker: Agent = talk.agent
        day: int = talk.day
        turn: int = talk.turn
        action: Action = ActionLogger.get_action(content, talker, day, turn, player)
        if action is not None:
            # ActionLogger.action_count[(day, turn, talker, action)] += 1
            ActionLogger.action_log.append(ActionLog(Util.game_count, day, turn, talker, action))
        # 前ゲームのログを action_count_all に反映
        # 現在のターンまでのもののみを処理することで負担を減らす
        oldest_log: ActionLog = ActionLogger.action_log[0] if len(ActionLogger.action_log) > 0 else None
        while oldest_log is not None:
            proceeds = False
            proceeds |= oldest_log.game <= Util.game_count - 2
            proceeds |= oldest_log.game == Util.game_count - 1 and oldest_log.day <= day
            proceeds |= oldest_log.game == Util.game_count - 1 and oldest_log.day == day and oldest_log.turn <= turn
            if not proceeds:
                break
            role: Role = ActionLogger.old_role_map[oldest_log.game - 1][oldest_log.agent]
            # Util.debug_print("ActionLogger.update", f"game={oldest_log.game}, day={oldest_log.day}, turn={oldest_log.turn}, agent={oldest_log.agent}, role={role}, action={oldest_log.action}")
            ActionLogger.action_count_all[(oldest_log.day, oldest_log.turn, oldest_log.agent, role, oldest_log.action)] += 1
            ActionLogger.action_count_sum[(oldest_log.agent, role, oldest_log.action)] += 1
            ActionLogger.action_log.popleft()
            if len(ActionLogger.action_log) > 0:
                oldest_log = ActionLogger.action_log[0]
        return action

    @staticmethod
    def get_score(d: int, t: int, talker: Agent, action: Action) -> DefaultDict[Role, float]:
        count: DefaultDict[Role, float] = defaultdict(float)
        score: DefaultDict[Role, float] = defaultdict(float)
        sum = 0
        role_list = ActionLogger.game_info.existing_role_list
        # is_important: bool = action in [Action.DIVINED_WITHOUT_CO, Action.IDENTIFIED_WITHOUT_CO, Action.IDENTIFIED_WITHOUT_CO_TO_COUNTERPART, Action.IDENTIFIED_TO_ALIVE, Action.CO_VILLAGER]
        is_important: bool = action in [Action.DIVINED_WITHOUT_CO, Action.CO_VILLAGER]

        if not is_important and Util.game_count <= 10:
            return score

        if ActionLogger.N == 5 and t >= 20:
            return score
        elif ActionLogger.N == 15 and (t >= 4 or d >= 4):
            return score
        # if t >= 4:
        #     return score

        # 相対確率を計算
        for r in role_list:
            if Util.agent_role_count[talker][r] == 0:
                count[r] = 0
            else:
                if is_important:
                    count[r] = ActionLogger.action_count_sum[(talker, r, action)] / Util.agent_role_count[talker][r]
                else:
                    count[r] = ActionLogger.action_count_all[(d, t, talker, r, action)] / Util.agent_role_count[talker][r]
            sum += count[r]

        if sum > 0:
            # 絶対確率に変換
            for r in role_list:
                score[r] = count[r] / sum

            if ActionLogger.N == 5:
                for r in role_list:
                    score[r] *= 5
            else:
                # 係数調整
                for r in role_list:
                    # if is_important and score[r] > 0.99:
                    if is_important:
                        # is_important かつ他の役職で同じ行動をしていないならスコアを上げる
                        Util.debug_print("ActionLogger.get_score\t", f"game={Util.game_count}, day={d}, turn={t}, agent={talker}, role={r}, action={action}, score={score[r]}")
                        score[r] *= 25
                        score_ = {str(r)[5]: np.round(s, 3) for r, s in score.items()}
                        print('score', score_)
                    else:
                        # 平均以上ならプラス、平均以下ならマイナスにする (デバッグ時にわかりやすくするため)
                        score[r] = score[r] - 1 / len(role_list)
                        # 係数調整
                        score[r] *= 1
        return score

    @staticmethod
    def finish(game_info: GameInfo):
        ActionLogger.old_role_map.append(game_info.role_map)

    @staticmethod
    def get_action(content: Content, talker: Agent, day: int, turn: int, player) -> Action:
        player: ddhbVillager = player # 循環参照エラー回避
        divination_reports: List[Judge] = player.divination_reports
        comingout_map: DefaultDict[Agent, Role] = player.comingout_map
        if content.topic == Topic.DIVINED:
            for report in divination_reports:
                # 同じ日の対抗の結果に被せて別の結果を報告した場合
                if report.agent != talker and report.target == content.target and report.day == content.day and report.result != content.result:
                    return Action.DIVINED_CONTRADICT
            if comingout_map[talker] != Role.SEER:
                return Action.DIVINED_WITHOUT_CO
            elif content.result == Species.WEREWOLF:
                return Action.DIVINED_BLACK
            elif content.result == Species.HUMAN:
                return Action.DIVINED_WHITE
        elif content.topic == Topic.IDENTIFIED:
            if comingout_map[talker] != Role.MEDIUM:
                if comingout_map[content.target] == comingout_map[talker] != Role.UNC:
                    return Action.IDENTIFIED_WITHOUT_CO_TO_COUNTERPART
                else:
                    return Action.IDENTIFIED_WITHOUT_CO
            elif player.is_alive(content.target):
                return Action.IDENTIFIED_TO_ALIVE
            elif content.result == Species.WEREWOLF:
                return Action.IDENTIFIED_BLACK
            elif content.result == Species.HUMAN:
                return Action.IDENTIFIED_WHITE
        elif content.topic == Topic.GUARDED:
            return Action.GUARDED
        elif content.topic == Topic.VOTE:
            # return Action.VOTE
            return Action.SHOW_HOSTILITY
        elif content.topic == Topic.COMINGOUT:
            if content.role == Role.SEER:
                return Action.CO_SEER
            elif content.role == Role.MEDIUM:
                return Action.CO_MEDIUM
            elif content.role == Role.BODYGUARD:
                return Action.CO_BODYGUARD
            elif content.role == Role.VILLAGER:
                return Action.CO_VILLAGER
        elif content.topic == Topic.OPERATOR:
            if content.operator == Operator.REQUEST:
                if content.content_list[0].topic == Topic.VOTE:
                    # return Action.REQUEST_VOTE
                    return Action.SHOW_HOSTILITY
                elif content.content_list[0].topic == Topic.COMINGOUT:
                    return Action.REQUEST_COMINGOUT
        elif content.topic == Topic.ESTIMATE:
            if content.role == Role.WEREWOLF:
                # return Action.ESTIMATE_WEREWOLF
                return Action.SHOW_HOSTILITY
            elif content.role == Role.POSSESSED:
                return Action.ESTIMATE_POSSESSED
            elif content.role == Role.VILLAGER:
                return Action.ESTIMATE_VILLAGER
        return Action.OTHER
