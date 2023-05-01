from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic)

import numpy as np
from Util import Util
import ddhbVillager
from typing import Dict


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
        self.seer_co_count = 0
        self.seer_co_id = []
        self.medium_co_count = 0
        self.medium_co_id = []
        self.bodyguard_co_count = 0
        self.bodyguard_co_id = []

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
    
    def add_scores(self, talker: Agent, score_dict: Dict[Role, float]) -> None:
        for key, value in score_dict.items():
            self.add_scores(talker, key, talker, key, value)


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

    # 有限値の最大を-100で統一    
    def talk_co(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, role: Role) -> None:
        if talker == self.me:
            # 自分のCOは無視
            return
        my_role = game_info.role_map[talker]
        # 他者の占いCO
        if role == Role.SEER:
            # 自分が真占いの場合
            if my_role == Role.SEER:
                # talkerが占いの確率はありえない→-inf
                # 村陣営の役職騙りは考慮しない→村陣営確率を限りなく0にする 後で変更するかも
                self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -float('inf'), Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
            else:
                talker_id = talker.agent_idx-1
                if talker_id in self.seer_co_id:
                    # 既にCOしている場合→複数回COすることでscoreを稼ぐのを防ぐ
                    return
                # 初COの場合
                self.seer_co_count += 1
                self.seer_co_id.append(talker_id)
                if self.seer_co_count == 1:
                    # とりあえず村陣営の役職騙りは考慮しない
                    # 一人目COは真占いの確率が高いと仮定する→人狼と狂人の確率をある程度下げる
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: -5, Role.WEREWOLF: -5, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                elif self.seer_co_count == 2:
                    seer_co_id_first = self.seer_co_id[0]
                    # 一人目COの人狼と狂人の確率を元に戻す
                    self.add_scores(seer_co_id_first, {Role.POSSESSED: +5, Role.WEREWOLF: +5})
                    # 二人目COの村陣営の役職騙りは考慮しない
                    # 二人目COの方が気持ち真っぽい←一人目がHPが少なくてCOする場合があるから
                    # 二人目CO人狼と狂人の確率を少し下げる
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: -1, Role.WEREWOLF: -1, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                elif self.seer_co_count == 3:
                    seer_co_id_second = self.seer_co_id[1]
                    # 二人目COの人狼と狂人の確率を元に戻す
                    self.add_scores(seer_co_id_second, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
                    # 三人目COの村陣営の役職騙りは考慮しない
                    # 三人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                else:
                    # 四人目以降COの村陣営の役職騙りは考慮しない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                    # 四人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                    for i in range(3, self.seer_co_count):
                        id = self.seer_co_id[i]
                        self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
        # 他者の霊媒CO
        elif role == Role.MEDIUM:
            # 自分が真霊媒の場合
            if my_role == Role.MEDIUM:
                # とりあえず村陣営の役職騙りは考慮しない
                self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -float('inf'), Role.BODYGUARD: -100})
            else:
                talker_id = talker.agent_idx-1
                if talker_id in self.medium_co_id:
                    # 既にCOしている場合→複数回COすることでscoreを稼ぐのを防ぐ
                    return
                # 初COの場合
                self.medium_co_count += 1
                self.medium_co_id.append(talker_id)
                if self.medium_co_count == 1:
                    # 一人目COの場合、ほぼ真→人狼と狂人の確率を下げる
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: -10, Role.WEREWOLF: -10, Role.MEDIUM: 0, Role.BODYGUARD: -100})
                elif self.medium_co_count == 2:
                    medium_co_id_first = self.medium_co_id[0]
                    # 一人目COの人狼と狂人の確率を元に戻す
                    self.add_scores(medium_co_id_first, {Role.POSSESSED: +10, Role.WEREWOLF: +10})
                    # 二人目COの村陣営の役職騙りは考慮しない
                    # 二人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: 0, Role.BODYGUARD: -100})                    
                else:
                    # 三人目以降COの村陣営の役職騙りは考慮しない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: 0, Role.BODYGUARD: -100})                    
                    # 三人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                    for i in range(2, self.medium_co_count):
                        id = self.medium_co_id[i]
                        self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
        # 他者の狩人CO
        elif role == Role.BODYGUARD:
            # 自分が真狩人の場合
            if my_role == Role.BODYGUARD:
                # とりあえず村陣営の役職騙りは考慮しない
                self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -float('inf')})
            else:
                talker_id = talker.agent_idx-1
                if talker_id in self.bodyguard_co_id:
                    # 既にCOしている場合→複数回COすることでscoreを稼ぐのを防ぐ
                    return
                # 初COの場合
                self.bodyguard_co_count += 1
                self.bodyguard_co_id.append(talker_id)
                if self.bodyguard_co_count == 1:
                    # 一人目COの場合、ほぼ真→人狼と狂人の確率を下げる
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: -10, Role.WEREWOLF: -10, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                elif self.bodyguard_co_count == 2:
                    bodyguard_co_id_first = self.bodyguard_co_id[0]
                    # 一人目COの人狼と狂人の確率を元に戻す
                    self.add_scores(bodyguard_co_id_first, {Role.POSSESSED: +10, Role.WEREWOLF: +10})
                    # 二人目COの村陣営の役職騙りは考慮しない
                    # 二人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                else:
                    # 三人目以降COの村陣営の役職騙りは考慮しない
                    self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: 0, Role.BODYGUARD: -100})
                    # 三人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                    for i in range(2, self.bodyguard_co_count):
                        id = self.bodyguard_co_id[i]
                        self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
        # 他者の狂人CO
        elif role == Role.POSSESSED:
            # 自分が真狂人の場合
            if my_role == Role.POSSESSED:
                pass
            # 自分が人狼の場合
            elif my_role == Role.WEREWOLF:
                pass
            # 自分が村陣営の場合
            else:
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
            pass # 有益な情報ではないので無視する

    def talk_identified(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        # 本物の霊媒師が嘘を言うことは無いと仮定する
        if species == Species.WEREWOLF:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, +100)
        elif species == Species.HUMAN:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, -100)
        else:
            pass # 有益な情報ではないので無視する

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
        # latest_vote_list で参照できるので意味がないかも
        pass