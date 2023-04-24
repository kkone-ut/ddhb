from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic)

import numpy as np
import ddhbVillager

class ScoreMatrix:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        # score_matrix[エージェント1, 役職1, エージェント2, 役職2]: エージェント1が役職1、エージェント2が役職2である相対確率の対数
        self.score_matrix: np.ndarray = np.zeros((self.N, self.M, self.N, self.M))
        self.player = _player
        self.me = _player.me

    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        N = self.N
        M = self.M

        pass

    # 公開情報から推測する

    def killed(self, game_info: GameInfo, game_setting: GameSetting, agent: Agent) -> None:
        id = agent.agent_idx()
        # 襲撃されたエージェントは人狼ではない
        self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')

    def vote(self, game_info: GameInfo, game_setting: GameSetting, voter: Agent, target: Agent) -> None:
        pass

    # 自身の能力の結果から推測する
    
    def my_divined(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        id = target.agent_idx()

        if species == Species.WEREWOLF:
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = +float('inf')
            # こう書くことも可能だが、自分が占い師であることは確定しているので、上の書き方に統一する
            # self.score_matrix[self.me.agent_idx(), Role.SEER, id, Role.WEREWOLF] = +float('inf')
        elif species == Species.HUMAN:
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')
        else:
            pass # 万が一不確定(Species.UNC, Species.ANY)の場合は何もしない

    def my_identified(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        id = target.agent_idx()

        if species == Species.WEREWOLF:
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = +float('inf')
        elif species == Species.HUMAN:
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')
        else:
            pass # 万が一不確定(Species.UNC, Species.ANY)の場合は何もしない

    def my_guarded(self, game_info: GameInfo, game_setting: GameSetting, target: Agent) -> None:
        id = target.agent_idx()

        # 護衛が成功したエージェントは人狼ではない
        self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')

    # 他の人の発言から推測する

    def talk_co(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, role: Role) -> None:
        pass

    def talk_will_vote(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass

    def talk_divined(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        pass

    def talk_identified(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        pass

    # 1日目の終わりに推測する (主に5人村の場合)

    def first_turn_end(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        pass

    # 新プロトコルでの発言に対応する

    def talk_guarded(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass

    def talk_voted(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass