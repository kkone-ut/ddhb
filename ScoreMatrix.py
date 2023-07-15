from aiwolf import (AbstractPlayer, Agent, Content, GameInfo, GameSetting,
                    Judge, Role, Species, Status, Talk, Topic)
from aiwolf.constant import AGENT_NONE

import numpy as np
from Util import Util
import ddhbVillager
from typing import Dict, List
from Side import Side
from ddhbVillager import *

class ScoreMatrix:

    seer_co_id: List[int]
    medium_co_id: List[int]
    bodyguard_co_id: List[int]
    
    seer_co: List[Agent]
    medium_co: List[Agent]
    bodyguard_co: List[Agent]


    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player) -> None:
        self.game_info = game_info
        self.game_setting = game_setting
        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)
        # score_matrix[エージェント1, 役職1, エージェント2, 役職2]: エージェント1が役職1、エージェント2が役職2である相対確率の対数
        # -infで相対確率は0になる
        self.score_matrix: np.ndarray = np.zeros((self.N, self.M, self.N, self.M))
        self.player: ddhbVillager = _player
        self.me = _player.me # 自身のエージェント
        self.my_role = game_info.my_role # 自身の役職
        self.rtoi = {Role.VILLAGER: 0, Role.SEER: 1, Role.POSSESSED: 2, Role.WEREWOLF: 3, Role.MEDIUM: 4, Role.BODYGUARD: 5}
        self.seer_co_count = 0
        self.seer_co_id = []
        self.medium_co_count = 0
        self.medium_co_id = []
        self.bodyguard_co_count = 0
        self.bodyguard_co_id = []
        self.seer_co = []
        self.medium_co = []
        self.bodyguard_co = []

        for a, r in game_info.role_map.items():
            if r != Role.ANY and r != Role.UNC:
                self.set_score(a, r, a, r, float('inf'))


    def update(self, game_info: GameInfo) -> None:
        self.game_info = game_info


    # スコアは相対確率の対数を表す
    # スコア = log(相対確率)
    # スコアの付け方
    # 確定情報: +inf または -inf
    # 非確定情報: 有限値 最大を1で統一する
    # 書くときは100を最大として、相対確率に直すときに1/10倍する

    # スコアの取得
    # agent1, agent2: Agent or int
    # role1, role2: Role or int
    def get_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role) -> float:
        i = agent1.agent_idx-1 if type(agent1) is Agent else agent1
        ri = self.rtoi[role1] if type(role1) is Role else role1
        j = agent2.agent_idx-1 if type(agent2) is Agent else agent2
        rj = self.rtoi[role2] if type(role2) is Role else role2

        if ri >= self.M or rj >= self.M: # 存在しない役職の場合はスコアを-infにする (5人村の場合)
            return -float('inf')
        
        return self.score_matrix[i, ri, j, rj]


    # スコアの設定
    # agent1, agent2: Agent or int
    # role1, role2: Role or int
    def set_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role, score: float) -> None:
        i = agent1.agent_idx-1 if type(agent1) is Agent else agent1
        ri = self.rtoi[role1] if type(role1) is Role else role1
        j = agent2.agent_idx-1 if type(agent2) is Agent else agent2
        rj = self.rtoi[role2] if type(role2) is Role else role2
        
        if ri >= self.M or rj >= self.M: # 存在しない役職の場合はスコアを設定しない (5人村の場合)
            return
        
        if score == float('inf'): # スコアを+infにすると相対確率も無限に発散するので、代わりにそれ以外のスコアを0にする。
            self.score_matrix[i, :, j, :] = -float('inf')
            self.score_matrix[i, ri, j, rj] = 0
        else:
            self.score_matrix[i, ri, j, rj] = score


    # スコアの加算
    # agent1, agent2: Agent or int
    # role1, rold2: Role, int, Species, Side or List
    def add_score(self, agent1: Agent, role1: Role, agent2: Agent, role2: Role, score: float) -> None:
        if type(role1) is Side:
            role1 = role1.get_role_list(self.N)
        if type(role2) is Side:
            role2 = role2.get_role_list(self.N)
        if type(role1) is Species:
            if role1 == Species.HUMAN:
                role1 = Side.VILLAGERS.get_role_list(self.N) + [Role.POSSESSED]
            elif role1 == Species.WEREWOLF:
                role1 = Role.WEREWOLF
            else:
                Util.error_print('role1 is not Species.HUMAN or Species.WEREWOLF')
        if type(role2) is Species:
            if role2 == Species.HUMAN:
                role2 = Side.VILLAGERS.get_role_list(self.N) + [Role.POSSESSED]
            elif role2 == Species.WEREWOLF:
                role2 = Role.WEREWOLF
            else:
                Util.error_print('role2 is not Species.HUMAN or Species.WEREWOLF')
        if type(role1) is not list:
            role1 = [role1]
        if type(role2) is not list:
            role2 = [role2]
        for r1 in role1:
            for r2 in role2:
                modified_score = self.get_score(agent1, r1, agent2, r2) + score
                self.set_score(agent1, r1, agent2, r2, modified_score)


    # スコアの加算をまとめて行う
    def add_scores(self, agent: Agent, score_dict: Dict[Role, float]) -> None:
        for key, value in score_dict.items():
            self.add_score(agent, key, agent, key, value)


    # --------------- 公開情報から推測する ---------------
    # 襲撃結果を反映→OK
    def killed(self, game_info: GameInfo, game_setting: GameSetting, agent: Agent) -> None:
        # 襲撃されたエージェントは人狼ではない
        self.set_score(agent, Role.WEREWOLF, agent, Role.WEREWOLF, -float('inf'))


    # 投票行動を反映→OK
    # todo: will vote の順番を見て、その人の投票で結果が変わらないならその投票の重みは軽くする
    # todo:     実際には吊られなそうなタイミングで黒に投票していたような場合はライン切りの可能性を考慮する
    def vote(self, game_info: GameInfo, game_setting: GameSetting, voter: Agent, target: Agent, day: int) -> None:
        N = self.N
        my_role = self.my_role
        day = self.game_info.day
        # 自分の投票行動は無視
        if voter == self.me:
            return
        # ---------- 5人村 ----------
        # 2日目でゲームの勝敗が決定しているので、1日目の投票行動の反映はほとんど意味ない
        if N == 5:
            # 投票者が村陣営で、投票対象が人狼である確率を上げる
            self.add_score(voter, Role.VILLAGER, target, Role.WEREWOLF, +0.1)
            self.add_score(voter, Role.SEER, target, Role.WEREWOLF, +0.3)
        # ---------- 15人村 ----------
        elif N == 15:
            # 日が進むほど判断材料が多くなるので、日にちで重み付けする
            weight = day * 0.1
            # 投票者が村陣営で、投票対象が人狼である確率を上げる
            self.add_score(voter, Role.VILLAGER, target, Role.WEREWOLF, weight)
            self.add_score(voter, Role.SEER, target, Role.WEREWOLF, weight*3)
            self.add_score(voter, Role.MEDIUM, target, Role.WEREWOLF, weight*1.5)
            self.add_score(voter, Role.BODYGUARD, target, Role.WEREWOLF, weight*1.5)
            # 人狼が仲間の人狼に投票する確率は低い
            self.add_score(voter, Side.WEREWOLVES, target, Role.WEREWOLF, -weight*5)
    # --------------- 公開情報から推測する ---------------


    # --------------- 自身の能力の結果から推測する：確定情報なのでスコアを +inf or -inf にする ---------------
    # 自分の占い結果を反映→OK
    # 結果騙りは考慮しない
    def my_divined(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        # 黒結果
        if species == Species.WEREWOLF:
            # 人狼であることが確定しているので、人狼のスコアを+inf(実際には他の役職のスコアを-inf(相対確率0)にする)
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, +float('inf'))
        # 白結果
        elif species == Species.HUMAN:
            # 人狼でないことが確定しているので、人狼のスコアを-inf(相対確率0)にする
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))
        else:
            # 万が一不確定(Species.UNC, Species.ANY)の場合
            Util.error_print('my_divined: species is not Species.WEREWOLF or Species.HUMAN')


    # 自分の霊媒結果を反映→OK
    # 結果騙りは考慮しない
    def my_identified(self, game_info: GameInfo, game_setting: GameSetting, target: Agent, species: Species) -> None:
        # 黒結果
        if species == Species.WEREWOLF:
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, +float('inf'))
        # 白結果
        elif species == Species.HUMAN:
            self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))
        else:
            Util.error_print('my_identified: species is not Species.WEREWOLF or Species.HUMAN')


    # 自分の護衛結果を反映→OK
    # 人狼の自噛みはルール上なし
    def my_guarded(self, game_info: GameInfo, game_setting: GameSetting, target: Agent) -> None:
        # 護衛が成功したエージェントは人狼ではない
        self.set_score(target, Role.WEREWOLF, target, Role.WEREWOLF, -float('inf'))
    # --------------- 自身の能力の結果から推測する ---------------


    # --------------- 他の人の発言から推測する：確定情報ではないので有限の値を加減算する ---------------
    # 他者のCOを反映 
    # Basketでは、人外は3CO目のCOはしないので、3CO目は真占いである確率が極めて高い？
    # todo: 15人村
    def talk_co(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, role: Role) -> None:
        N = self.N
        my_role = self.my_role
        day = self.game_info.day
        turn = self.player.talk_turn
        # 自分のCOは無視
        if talker == self.me:
            return
        # ---------- 5人村 ----------
        if N == 5:
            # ----- 占いCO -----
            # 基本、初日の早いターンでCOなので、COでのスコア変更は少なめにして、結果でスコアを変更する
            if role == Role.SEER:
                # --- 占い ---
                if my_role == Role.SEER:
                    # 結果に関わらず、人狼と狂人の確率を上げる（村陣営の役職騙りを考慮しない）
                    self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
                # --- それ以外 ---
                else:
                    # 既にCOしている場合：複数回COすることでscoreを稼ぐのを防ぐ
                    if talker in self.seer_co:
                        return
                    # 複数占いCOがあった場合、誰か一人が真で残りは偽である確率はほぼ100%
                    # (両方とも偽という割り当ての確率を0%にする)
                    # todo: これがどういう意図なのか確認する
                    for seer in self.seer_co:
                        self.add_score(seer, Role.SEER, talker, Side.WEREWOLVES, +100)
                        self.add_score(talker, Role.SEER, seer, Side.WEREWOLVES, +100)
                    # 村人である確率を下げる（村人の役職騙りを考慮しない）
                    self.add_scores(talker, {Role.VILLAGER: -100})
                    # 初COの場合
                    self.seer_co_count += 1
                    self.seer_co.append(talker)
                    # --- 人狼 ---
                    if my_role == Role.WEREWOLF:
                        # 占いと狂人どちらもありうるので、CO段階では何もしない→結果でスコアを変更する
                        return
                    # --- 狂人 ---
                    if my_role == Role.POSSESSED:
                        # 占いと人狼どちらもありうるので、CO段階では少しの変更にする
                        # 気持ち、1CO目は占い っぽい
                        if self.seer_co_count == 1:
                            self.add_scores(talker, {Role.SEER: +1})
                        # 2CO目以降は無視
                        else:
                            return
                    # --- 村人 ---
                    # 村人視点では、COを重視する：結果では正確に判断できないから
                    # todo: 本来は行動学習するべき
                    # 気持ち、1,2CO目は占いor狂人、3CO目は占いor人狼 っぽい
                    if self.seer_co_count == 1:
                        self.add_scores(talker, {Role.SEER: +3, Role.POSSESSED: +3, Role.WEREWOLF: +1})
                    elif self.seer_co_count == 2:
                        self.add_scores(talker, {Role.SEER: +3, Role.POSSESSED: +3, Role.WEREWOLF: +2})
                    else:
                        self.add_scores(talker, {Role.SEER: +2, Role.POSSESSED: +1, Role.WEREWOLF: +3})
            # ----- 狂人CO -----
            # 村人の狂人COはないと仮定する
            elif role == Role.POSSESSED:
                # --- 人狼 ---
                if my_role == Role.WEREWOLF:
                    self.add_scores(talker, {Role.POSSESSED: +10})
                # --- 狂人 ---
                elif my_role == Role.POSSESSED:
                    self.add_scores(talker, {Role.WEREWOLF: +10})
                # --- 村人 or 占い ---
                else:
                    self.add_scores(talker, {Role.POSSESSED: +5, Role.WEREWOLF: +1})
            # ----- 人狼CO -----
            elif role == Role.WEREWOLF:
                # --- 狂人 ---
                if my_role == Role.POSSESSED:
                    # 村陣営がPP阻止のために、人狼COする場合があるので、少しの変更にする
                    self.add_scores(talker, {Role.WEREWOLF: +5})
                # --- 人狼 ---
                elif my_role == Role.WEREWOLF:
                    # 村陣営がPP阻止のために、人狼COする場合があるので、少しの変更にする
                    self.add_scores(talker, {Role.POSSESSED: +5})
                # --- 村人 or 占い ---
                else:
                    # 狂人と人狼どちらもありうるので、少しの変更にする：狂人と人狼で優劣をつけない→あくまで今までの結果を重視する
                    self.add_scores(talker, {Role.POSSESSED: +5, Role.WEREWOLF: +5})
        # ---------- 15人村 ----------
        elif N == 15:
            # ----- 占いCO -----
            if role == Role.SEER:
                # --- 占い ---
                if my_role == Role.SEER:
                    # talkerが占いの確率はありえない→-inf
                    # 村陣営の役職騙りは考慮しない→村陣営確率を限りなく0にする 後で変更するかも
                    # self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -float('inf'), Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                    self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
                else:
                    # 既にCOしている場合：複数回COすることでscoreを稼ぐのを防ぐ
                    if talker in self.seer_co:
                        return
                    # 複数占いCOがあった場合、誰か一人が真で残りは偽である確率はほぼ100%
                    # (両方とも偽という割り当ての確率を0%にする)
                    for seer in self.seer_co:
                        self.add_score(seer, Role.SEER, talker, Side.WEREWOLVES, +100)
                        self.add_score(talker, Role.SEER, seer, Side.WEREWOLVES, +100)
                    # 村人である確率を下げる（村人の役職騙りを考慮しない）
                    self.add_scores(talker, {Role.VILLAGER: -100})
                    # 初COの場合
                    self.seer_co_count += 1
                    self.seer_co.append(talker)
                    # --- 人狼 ---
                    if my_role == Role.WEREWOLF:
                        # 霊媒と狂人どちらもありうるので、主には結果でスコアを変更する
                        # 気持ち、霊媒っぽい
                        self.add_scores(talker, {Role.MEDIUM: +1})
                        return
                    # todo: ここから後を変更する
                    # review: 1人目、2人目、3人目の狂人、人狼のスコアをどうするか。
                    # review: 1人目のスコアが少し低めになっているのは確定するとしているからだが、一日目の夜に確定させる方針になった
                    if self.seer_co_count == 1:
                        # とりあえず村陣営の役職騙りは考慮しない
                        # 一人目COは真占いの確率が高いと仮定する→人狼と狂人の確率をある程度下げる
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: -5, Role.WEREWOLF: -5, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                    elif self.seer_co_count == 2:
                        seer_co_first = self.seer_co[0]
                        # 一人目COの人狼と狂人の確率を元に戻す
                        self.add_scores(seer_co_first, {Role.POSSESSED: +5, Role.WEREWOLF: +5})
                        # 二人目COの村陣営の役職騙りは考慮しない
                        # 二人目COの方が気持ち真っぽい←一人目がHPが少なくてCOする場合があるから
                        # 二人目CO人狼と狂人の確率を少し下げる
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: -1, Role.WEREWOLF: -1, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                    elif self.seer_co_count == 3:
                        seer_co_first = self.seer_co[0]
                        seer_co_second = self.seer_co[1]
                        # 二人目COの人狼と狂人の確率を元に戻す
                        self.add_scores(seer_co_second, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
                        # 三人目COの村陣営の役職騙りは考慮しない
                        # 三人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                    else:
                        # 四人目以降COの村陣営の役職騙りは考慮しない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: 0, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -100})
                        # 四人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                        for i in range(3, self.seer_co_count):
                            id = self.seer_co[i]
                            self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
            # ----- 霊媒CO -----
            elif role == Role.MEDIUM:
                # --- 霊媒 ---
                if my_role == Role.MEDIUM:
                    # とりあえず村陣営の役職騙りは考慮しない
                    self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
                    # self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -float('inf'), Role.BODYGUARD: -100})
                else:
                    # 既にCOしている場合：複数回COすることでscoreを稼ぐのを防ぐ
                    if talker in self.medium_co:
                        return
                    # 複数霊媒COがあった場合、誰か一人が真で残りは偽である確率はほぼ100%
                    # (両方とも偽という割り当ての確率を0%にする)
                    for medium in self.medium_co:
                        self.add_score(medium, Role.MEDIUM, talker, Side.WEREWOLVES, +100)
                        self.add_score(talker, Role.MEDIUM, medium, Side.WEREWOLVES, +100)
                    # 村人である確率を下げる（村人の役職騙りを考慮しない）
                    self.add_scores(talker, {Role.VILLAGER: -100})
                    # 初COの場合
                    self.medium_co_count += 1
                    self.medium_co.append(talker)
                    # --- 人狼 ---
                    if my_role == Role.WEREWOLF:
                        # 占いと狂人どちらもありうるので、CO段階では何もしない→結果でスコアを変更する
                        return
                    # ここから後を変更する
                    if self.medium_co_count == 1:
                        # 一人目COの場合、ほぼ真→人狼と狂人の確率を下げる
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: -10, Role.WEREWOLF: -10, Role.MEDIUM: 0, Role.BODYGUARD: -100})
                    elif self.medium_co_count == 2:
                        medium_co_first = self.medium_co[0]
                        # 一人目COの人狼と狂人の確率を元に戻す
                        self.add_scores(medium_co_first, {Role.POSSESSED: +10, Role.WEREWOLF: +10})
                        # 二人目COの村陣営の役職騙りは考慮しない
                        # 二人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: 0, Role.BODYGUARD: -100})          
                    else:
                        # 三人目以降COの村陣営の役職騙りは考慮しない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: 0, Role.BODYGUARD: -100})                    
                        # 三人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                        for i in range(2, self.medium_co_count):
                            id = self.medium_co[i]
                            self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
            # ----- 狩人CO -----
            elif role == Role.BODYGUARD:
                # --- 狩人 ---
                if my_role == Role.BODYGUARD:
                    # とりあえず村陣営の役職騙りは考慮しない
                    self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
                    # self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: -float('inf')})
                else:
                    # 既にCOしている場合：複数回COすることでscoreを稼ぐのを防ぐ
                    if talker in self.medium_co:
                        return
                    # 初COの場合
                    self.bodyguard_co_count += 1
                    self.bodyguard_co.append(talker)
                    # --- 人狼 ---
                    if my_role == Role.WEREWOLF:
                        # 占いと狂人どちらもありうるので、CO段階では何もしない→結果でスコアを変更する
                        return
                    if self.bodyguard_co_count == 1:
                        # 一人目COの場合、ほぼ真→人狼と狂人の確率を下げる
                        # self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: -10, Role.WEREWOLF: -10, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                        # todo: 狩人はCOせずに死ぬことが多い。他の役職についても同様だが、特に日付によって重み付けすべき。
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                    elif self.bodyguard_co_count == 2:
                        bodyguard_co_first = self.bodyguard_co[0]
                        # 一人目COの人狼と狂人の確率を元に戻す
                        self.add_scores(bodyguard_co_first, {Role.POSSESSED: +10, Role.WEREWOLF: +10})
                        # 二人目COの村陣営の役職騙りは考慮しない
                        # 二人COの場合は、どの占いも同じくらい真っぽいと仮定する→scoreの変更はしない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                    else:
                        # 三人目以降COの村陣営の役職騙りは考慮しない
                        self.add_scores(talker, {Role.VILLAGER: -100, Role.SEER: -100, Role.POSSESSED: 0, Role.WEREWOLF: 0, Role.MEDIUM: -100, Role.BODYGUARD: 0})
                        # 三人目以降はHPが少なくてCOする場合があるから、人狼と狂人の確率を少し上げる
                        for i in range(2, self.bodyguard_co_count):
                            id = self.bodyguard_co[i]
                            self.add_scores(id, {Role.POSSESSED: +1, Role.WEREWOLF: +1})
            # ----- 狂人CO -----
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
            # ----- 人狼CO ------
            elif role == Role.WEREWOLF:
                if my_role == Role.POSSESSED:
                    # 自分が狂人なので、人狼COは信用できる
                    self.add_scores(talker, {Role.WEREWOLF: +100})
                elif my_role == Role.WEREWOLF:
                    # 人狼でないことが確定していれば狂人と推測する
                    # 本当に人狼なら特にすることはない
                    if talker in game_info.role_map and game_info.role_map[talker] != Role.WEREWOLF:
                        self.add_scores(talker, {Role.POSSESSED: +100})
                else:
                    # 自分が村陣営の場合、人狼か狂人と推測する
                    self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
            # ----- 村人CO -----
            elif role == Role.VILLAGER:
                # 自分が人狼の場合→人狼の時は役職を噛みたいから、村人COも認知する
                if my_role == Role.WEREWOLF:
                    self.add_scores(talker, {Role.VILLAGER: +10})


    # 投票意思を反映→OK
    # それほど重要ではないため、スコアの更新は少しにする
    def talk_will_vote(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        N = self.N
        day = self.game_info.day
        turn = self.player.talk_turn
        will_vote = self.player.will_vote_reports
        # 自分の投票意思は無視
        if talker == self.me:
            return
        # 同じ対象に二回目以降の投票意思は無視
        if will_vote[talker] == target:
            return
        # 初日初ターンは無視
        if day == 1 and turn <= 1:
            return
        # ---------- 5人村 ----------
        if N == 5:
            # 発言者が村人・占い師で、対象が人狼である確率を上げる
            self.add_score(talker, Role.VILLAGER, target, Role.WEREWOLF, +0.1)
            self.add_score(talker, Role.SEER, target, Role.WEREWOLF, +0.3)
            # 人狼は投票意思を示しがちだから、人狼である確率を上げる
            # 違う対象に投票意思を示している
            self.add_scores(talker, {Role.WEREWOLF: +1})
        # ---------- 15人村 ----------
        elif N == 15:
            # 発言者の役職ごとに、対象が人狼である確率を上げる
            self.add_score(talker, Role.VILLAGER, target, Role.WEREWOLF, +0.005)
            self.add_score(talker, Role.SEER, target, Role.WEREWOLF, +0.02)
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, +0.01)
            self.add_score(talker, Role.BODYGUARD, target, Role.WEREWOLF, +0.01)
            # 人狼のライン切りを反映する
            self.add_score(talker, Role.WEREWOLF, target, Role.WEREWOLF, -0.1)
            # 人狼は投票意思を示しがちだから、人狼である確率を上げる
            self.add_scores(talker, {Role.WEREWOLF: +0.01})


    # Basketにないため、後で実装する
    def talk_estimate(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, role: Role) -> None:
        pass

    # 他者の占い結果を反映→OK
    # 条件分岐は、N人村→myrole→白黒結果→targetが自分かどうか
    def talk_divined(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        N = self.N
        my_role = self.my_role
        role_map = self.game_info.role_map
        # 自分と仲間の人狼の結果は無視
        if talker == self.me or (talker in role_map and role_map[talker] == Role.WEREWOLF):
            return
        # ---------- 5人村 ----------
        if N == 5:
            # ----- 占い -----
            if my_role == Role.SEER:
                # 結果に関わらず、人狼と狂人の確率を上げる（村陣営の役職騙りを考慮しない）
                self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
            # ----- 人狼 -----
            elif my_role == Role.WEREWOLF:
                # 黒結果
                if species == Species.WEREWOLF:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を上げる (誤爆を考慮しなければ100%)
                        self.add_scores(talker, {Role.SEER: +100, Role.POSSESSED: +0})
                    # 対象：自分以外
                    else:
                        # talkerの狂人である確率を上げる (ほぼ100%と仮定)
                        if self.player.comingout_map[target] == Role.SEER:
                            self.add_scores(talker, {Role.POSSESSED: +100})
                # 白結果
                elif species == Species.HUMAN:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を下げる、狂人である確率を上げる（結果の矛盾が起こっているから、値を大きくしている）
                        self.add_scores(talker, {Role.SEER: -100, Role.POSSESSED: +100})
                    # 対象：自分以外
                    else:
                        # 狂人は基本的に黒結果を出すことが多いので、talkerの占い師である確率を上げる
                        # 確定ではないので、値は控えめにする
                        self.add_scores(talker, {Role.SEER: +10, Role.POSSESSED: +5})
            # ----- 狂人 -----
            elif my_role == Role.POSSESSED:
                # 黒結果
                if species == Species.WEREWOLF:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を下げる
                        # 本来は占い師である確率を0%にしたいが、占い師の結果騙りがあるため、-100にはしない
                        self.add_scores(talker, {Role.SEER: +1, Role.WEREWOLF: +5})
                    # 対象：自分以外
                    else:
                        # talkerが占い師で、targetが人狼である確率を上げる
                        # かなりの確率で人狼であると仮定する
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, +10)
                        # 占い師は確率的に白結果を出すことが多いので、talkerの人狼である確率を少し上げる
                        self.add_scores(talker, {Role.WEREWOLF: +3})
                # 白結果
                elif species == Species.HUMAN:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を上げる
                        # 自分への白結果はほぼ占い師確定
                        self.add_scores(talker, {Role.SEER: +10})
                    # 対象：自分以外
                    else:
                        # talkerが占い師で、targetが人狼である確率を下げる
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, -5)
                        # 人狼は基本的に黒結果を出すことが多いので、talkerの占い師である確率を上げる
                        # 確定ではないので、値は控えめにする
                        self.add_scores(talker, {Role.SEER: +3, Role.WEREWOLF: +1})
            # ----- 村人 -----
            else:
                # 黒結果
                if species == Species.WEREWOLF:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を下げる（結果の矛盾が起こっているから、値を大きくしている）
                        self.add_scores(talker, {Role.SEER: -100, Role.POSSESSED: +10, Role.WEREWOLF: +10})
                    # 対象：自分以外
                    else:
                        # talkerが占い師で、targetが人狼である確率を上げる
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, +3)
                        # # talkerが狂人と人狼である確率を少し下げる→むしろ上げるべき
                        # self.add_scores(talker, {Role.POSSESSED: -1, Role.WEREWOLF: -1})
                        # talkerが狂人と人狼である確率を少し上げる
                        self.add_scores(talker, {Role.POSSESSED: +3, Role.WEREWOLF: +1})
                # 白結果
                elif species == Species.HUMAN:
                    # 対象：自分
                    if target == self.me:
                        # talkerの占い師である確率を上げる
                        # 自分への白結果はほぼ占い師確定
                        self.add_scores(talker, {Role.SEER: +10})
                    # 対象：自分以外
                    else:
                        # talkerが占い師で、targetが人狼である確率を下げる
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, -5)
                        # 人狼は基本的に黒結果を出すことが多いので、talkerの占い師である確率を上げる
                        # 確定ではないので、値は控えめにする
                        self.add_scores(talker, {Role.SEER: +3, Role.POSSESSED: +1, Role.WEREWOLF: +1})
        # ---------- 15人村 ----------
        elif N == 15:
            # ----- 占い -----
            if my_role == Role.SEER:
                # 結果に関わらず、人狼と狂人の確率を上げる（村陣営の役職騙りを考慮しない）
                self.add_scores(talker, {Role.POSSESSED: +100, Role.WEREWOLF: +100})
            # ----- 人狼 -----
            elif my_role == Role.WEREWOLF:
                allies: List[Agent] = role_map.keys()
                # 黒結果
                if species == Species.WEREWOLF:
                    # 対象：人狼仲間
                    # if target in role_map and role_map[target] == Role.WEREWOLF:
                    if target in allies:
                        # 人狼に黒出ししている場合は本物の可能性が高い
                        self.add_scores(talker, {Role.SEER: +10, Role.POSSESSED: +1})
                    # 対象：それ以外
                    else:
                        # 外れてる場合は狂人確定
                        self.add_scores(talker, {Role.SEER: -100, Role.POSSESSED: +100})
                        Util.debug_print('狂人:\t', talker)
                # 白結果
                elif species == Species.HUMAN:
                    # 対象：人狼仲間
                    # if target in role_map and role_map[target] == Role.WEREWOLF:
                    if target in allies:
                        # 人狼に白だししている場合は狂人確定
                        self.add_scores(talker, {Role.SEER: -100, Role.POSSESSED: +100})
                        Util.debug_print('狂人:\t', talker)
                    # 対象：それ以外
                    else:
                        # 当たっている場合は、若干占い師である可能性を上げる
                        self.add_scores(talker, {Role.SEER: +5, Role.POSSESSED: +1})
            # ----- 狂人 or 村人 -----
            else:
                # 黒結果
                if species == Species.WEREWOLF:
                    # 対象：自分
                    if target == self.me:
                        # COの段階で占い師以外の市民の確率が下がっているが、COをせずに占い結果を報告する場合も考慮して人狼陣営の可能性も上げる
                        self.add_scores(talker, {Role.SEER: -100, Role.POSSESSED: +100, Role.WEREWOLF: +100})
                    # 対象：自分以外
                    else:
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, +5)
                        # todo: 逆・裏・対偶を一つにまとめた関数を作る
                        self.add_score(talker, Role.SEER, target, Species.HUMAN, -5)
                        self.add_score(talker, Side.WEREWOLVES, target, Species.HUMAN, +5)
                        self.add_score(talker, Side.WEREWOLVES, target, Role.WEREWOLF, -5)
                # 白結果
                elif species == Species.HUMAN:
                    # 対象：自分
                    if target == self.me:
                        self.add_scores(talker, {Role.SEER: +5, Role.POSSESSED: +1})
                    # 対象：自分以外
                    else:
                        self.add_score(talker, Role.SEER, target, Role.WEREWOLF, -5)
                        self.add_score(talker, Role.SEER, target, Species.HUMAN, +5)
                        self.add_score(talker, Side.WEREWOLVES, target, Role.WEREWOLF, +5)
                        # 人狼陣営でも本物の白出しをする場合があるので、この可能性は排除しない (黒出しと白出しで異なる部分)
                        # self.add_score(talker, Side.WEREWOLVES, target, Species.HUMAN, -5)


    # 他者の霊媒結果を反映
    # todo: talk_divinedと同じように条件分岐する
    def talk_identified(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent, species: Species) -> None:
        # 本物の霊媒師が嘘を言うことは無いと仮定する
        if species == Species.WEREWOLF:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, +5)

            self.add_score(talker, Role.MEDIUM, target, Species.HUMAN, -5)
            self.add_score(talker, Side.WEREWOLVES, target, Species.HUMAN, +5)
            self.add_score(talker, Side.WEREWOLVES, target, Role.WEREWOLF, -5)
        elif species == Species.HUMAN:
            self.add_score(talker, Role.MEDIUM, target, Role.WEREWOLF, -5)

            self.add_score(talker, Role.MEDIUM, target, Species.HUMAN, +5)
            self.add_score(talker, Side.WEREWOLVES, target, Role.WEREWOLF, +5)
            # 人狼陣営でも本物の白出しをする場合があるので、この可能性は排除しない (黒出しと白出しで異なる部分)
            # self.add_score(talker, Side.WEREWOLVES, target, Species.HUMAN, -5) 
        else:
            pass # 有益な情報ではないので無視する
    # --------------- 他の人の発言から推測する ---------------


    # 1日目の終わりに推測する (主に5人村の場合)

    # 5人村の場合、1日目の終わりに推測する→使う必要なさそう
    # todo: 5人村用に最適化する and 15人村でも、占い師や霊媒師の確定に利用する
    def first_turn_end(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        # 5人村の場合、1日目の終わりに推測する
        N = self.N
        my_role = self.my_role
        if N == 5:
            for i in range(N):
                # 占いCOをしていない人の確率を操作する
                if i not in self.seer_co_id:
                    # 占い師、人狼、狂人である確率を下げる
                    self.add_scores(i, {Role.SEER: -10, Role.WEREWOLF: -5, Role.POSSESSED: -10})
                # 占いCOをしている人の確率を操作する
                elif i in self.seer_co_id:
                    # 自分のCOは無視
                    if i == self.me:
                        continue
                    # 自分が占い師の場合
                    if my_role == Role.SEER:
                        self.add_scores(i, {Role.SEER: -100, Role.WEREWOLF: +5, Role.POSSESSED: +10})
                    else:
                        self.add_scores(i, {Role.SEER: +10, Role.WEREWOLF: +1, Role.POSSESSED: +10})



    # --------------- 新プロトコルでの発言に対応する ---------------
    # 護衛成功発言を反映→OK
    # todo: 人狼視点では、確定情報になりうるから、後で実装する：襲撃先と護衛成功発言先が一致していたら真狩人
    def talk_guarded(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        if len(game_info.last_dead_agent_list) == 0:
            # 護衛が成功していたら護衛対象は人狼ではない
            self.add_score(talker, Role.BODYGUARD, target, Role.WEREWOLF, -10)
            self.add_score(talker, Role.BODYGUARD, target, Species.HUMAN, +10)
            self.add_score(talker, Side.WEREWOLVES, target, Species.HUMAN, -5)
            self.add_score(talker, Side.WEREWOLVES, target, Role.WEREWOLF, +5)

    # 投票した発言を反映
    # 後で実装する→そんなに重要でない
    def talk_voted(self, game_info: GameInfo, game_setting: GameSetting, talker: Agent, target: Agent) -> None:
        # latest_vote_list で参照できるので意味がないかも
        pass
    # --------------- 新プロトコルでの発言に対応する ---------------