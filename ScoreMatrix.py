from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic)

import numpy as np
from Util import Util
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
        self.rtoi = {Role.VILLAGER: 0, Role.SEER: 1, Role.POSSESSED: 2, Role.WEREWOLF: 3, Role.MEDIUM: 4, Role.BODYGUARD: 5}

    # スコアの取得
    # agent1, agent2: Agent or int
    # role1, role2: Role or int
    def get_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role) -> float:
        i = agent1.agent_idx-1 if type(agent1) is Agent else agent1
        ri = self.rtoi[role1] if type(role1) is Role else role1
        j = agent2.agent_idx-1 if type(agent2) is Agent else agent2
        rj = self.rtoi[role2] if type(role2) is Role else role2
        return self.score_matrix[i, ri, j, rj]
    
    # スコアの設定
    # agent1, agent2: Agent or int
    # role1, role2: Role or int
    def set_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role, score: float) -> None:
        i = agent1.agent_idx-1 if type(agent1) is Agent else agent1
        ri = self.rtoi[role1] if type(role1) is Role else role1
        j = agent2.agent_idx-1 if type(agent2) is Agent else agent2
        rj = self.rtoi[role2] if type(role2) is Role else role2
        if score == float('inf'): # スコアを+infにすると相対確率も無限に発散するので、代わりにそれ以外のスコアを0にする。
            self.score_matrix[i, :, j, :] = -float('inf')
            self.score_matrix[i, ri, j, rj] = 0
        else:
            self.score_matrix[i, ri, j, rj] = score

    # スコアの加算
    # agent1, agent2: Agent or int
    # role1, role2: Role or int
    def add_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role, score: float) -> None:
        score = self.get_score(agent1, role1, agent2, role2) + score
        self.set_score(agent1, role1, agent2, role2, score)

    # 公開情報から推測する

    def killed(self, game_info: GameInfo, game_setting: GameSetting, agent: Agent) -> None:
        # 襲撃されたエージェントは人狼ではない
        self.set_score(agent, Role.WEREWOLF, agent, Role.WEREWOLF, -float('inf'))

    # 呼び出し未実装
    def vote(self, game_info: GameInfo, game_setting: GameSetting, voter: Agent, target: Agent) -> None:
        pass

    # 自身の能力の結果から推測する
    # 確定情報なのでスコアを +inf または -inf にする
    
    def my_divined(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        if species == Species.WEREWOLF:
            # 人狼であることが確定しているので、人狼のスコアを+inf(実際には他の役職のスコアを-inf(相対確率0)にする)
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, +float('inf'))
        elif species == Species.HUMAN:
            # 人狼でないことが確定しているので、人狼のスコアを-inf(相対確率0)にする
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))
        else:
            # 万が一不確定(Species.UNC, Species.ANY)の場合
            Util.error('my_divined: species is not Species.WEREWOLF or Species.HUMAN')

    def my_identified(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        # my_divinedと同様
        if species == Species.WEREWOLF:
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, +float('inf'))
        elif species == Species.HUMAN:
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))
        else:
            Util.error('my_identified: species is not Species.WEREWOLF or Species.HUMAN')

    def my_guarded(self, game_info: GameInfo, game_setting: GameSetting, target: Agent) -> None:
        # 護衛が成功したエージェントは人狼ではない
        self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))

    # 他の人の発言から推測する
    # 確定情報ではないので有限の値を加減算する

    def talk_co(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, role: Role) -> None:
        pass

    def talk_will_vote(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass

    def talk_estimate(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, role: Role) -> None:
        pass

    def talk_divined(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        if species == Species.WEREWOLF:
            # 本物の占い師が間違って黒出しする可能性を考慮して少し低めにする
            # (5人村で占い結果が白だったとき、別のエージェントに黒出しすることがある)
            self.add_score(talker, Role.SEER, target, Role.WEREWOLF, 50)
        elif species == Species.HUMAN:
            # 本物の占い師が人狼に白出しすることはないと仮定する
            self.add_score(talker, Role.SEER, target, Role.WEREWOLF, -100)
        else:
            Util.error('talk_divined: species is not Species.WEREWOLF or Species.HUMAN')

    def talk_identified(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        # 本物の霊媒師が嘘を言うことは無いと仮定する
        if species == Species.WEREWOLF:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, +100)
        elif species == Species.HUMAN:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, -100)
        else:
            Util.error('talk_identified: species is not Species.WEREWOLF or Species.HUMAN')

    # 1日目の終わりに推測する (主に5人村の場合)

    # 呼び出し未実装
    def first_turn_end(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        pass

    # 新プロトコルでの発言に対応する

    def talk_guarded(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        if len(game_info.last_dead_agent_list) == 0:
            # 護衛が成功していたら護衛対象は人狼ではない
            self.add_score(talker, Role.BODYGUARD, target, Role.WEREWOLF, -100)

    def talk_voted(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        pass