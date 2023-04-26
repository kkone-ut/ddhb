from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic)

import numpy as np
import ddhbVillager

class ScoreMatrix:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        # score_matrix[エージェント1, 役職1, エージェント2, 役職2]: エージェント1が役職1、エージェント2が役職2である相対確率の対数
        # -infで相対確率は0になる
        self.score_matrix: np.ndarray = np.zeros((self.N, self.M, self.N, self.M))
        self.player = _player
        self.me = _player.me

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
            # 人狼であることが確定しているので、他の役職のスコアを-inf(相対確率0)にする
            # WEREWOLFのスコアを+infにするだけでも良いが、相対確率が無限に発散するので嬉しくない
            self.score_matrix[id, :, id, :] = -float('inf')
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = 0

            # こう書くことも可能だが、自分が占い師であることは確定しているので、上の書き方に統一する
            # self.score_matrix[self.me.agent_idx(), Role.SEER, id, Role.WEREWOLF] = +float('inf')
        elif species == Species.HUMAN:
            # 人狼でないことが確定しているので、人狼のスコアを-inf(相対確率0)にする
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')
        else:
            pass # 万が一不確定(Species.UNC, Species.ANY)の場合は何もしない

    def my_identified(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        id = target.agent_idx()

        # my_divinedと同様
        if species == Species.WEREWOLF:
            self.score_matrix[id, :, id, :] = -float('inf')
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = 0
        elif species == Species.HUMAN:
            self.score_matrix[id, Role.WEREWOLF, id, Role.WEREWOLF] = -float('inf')
        else:
            pass

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
        id_talker = talker.agent_idx()
        id_target = target.agent_idx()

        if species == Species.WEREWOLF:
            # 本物の占い師が間違って黒出しする可能性を考慮してスコアに有限の値を加算する
            # (5人村で占い結果が白だったとき、別のエージェントに黒出しすることがある)
            self.score_matrix[id_talker, Role.SEER, id_target, Role.WEREWOLF] += 1
        elif species == Species.HUMAN:
            # 本物の占い師が人狼に白出しすることはないと仮定する
            self.score_matrix[id_talker, Role.SEER, id_target, Role.WEREWOLF] = -float('inf')
        else:
            pass

        pass

    def talk_identified(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        id_talker = talker.agent_idx()
        id_target = target.agent_idx()

        # 本物の霊媒師が嘘を言うことは無いと仮定する
        if species == Species.WEREWOLF:
            self.score_matrix[id_talker, Role.MEDIUM, id_target, :] = -float('inf')
            self.score_matrix[id_talker, Role.MEDIUM, id_target, Role.WEREWOLF] = 0
        elif species == Species.HUMAN:
            self.score_matrix[id_talker, Role.MEDIUM, id_target, Role.WEREWOLF] = -float('inf')
        else:
            pass
        
        pass

    # 1日目の終わりに推測する (主に5人村の場合)

    def first_turn_end(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        pass

    # 新プロトコルでの発言に対応する

    def talk_guarded(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass

    def talk_voted(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass