import random
from collections import defaultdict
from typing import DefaultDict, Dict, List

import numpy as np
from ActionLogger import Action, ActionLogger
from Assignment import Assignment
from const import CONTENT_SKIP
from RealPossessedDetector import RealPossessedDetector
from RolePredictor import RolePredictor
from ScoreMatrix import ScoreMatrix
from Util import Util

from aiwolf import (AbstractPlayer, Agent, ComingoutContentBuilder, Content,
                    EstimateContentBuilder, GameInfo, GameSetting, Judge,
                    Operator, RequestContentBuilder, Role, Species, Status,
                    Talk, Topic, VoteContentBuilder)
from aiwolf.constant import AGENT_ANY, AGENT_NONE
from aiwolf.vote import Vote


# 村人
class ddhbVillager(AbstractPlayer):
    """ddhb villager agent."""

    me: Agent # 自身
    """Myself."""
    vote_candidate: Agent # 投票候補
    """Candidate for voting."""
    game_info: GameInfo # ゲーム情報
    """Information about current game."""
    game_setting: GameSetting # ゲーム設定
    """Settings of current game."""
    comingout_map: DefaultDict[Agent, Role] # CO辞書
    """Mapping between an agent and the role it claims that it is."""
    divination_reports: List[Judge] # 占い結果
    """Time series of divination reports."""
    identification_reports: List[Judge] # 霊媒結果
    """Time series of identification reports."""
    talk_list_head: int # talkのインデックス
    """Index of the talk to be analysed next."""
    will_vote_reports: DefaultDict[Agent, Agent] # 投票宣言
    talk_list_all: List[Talk] # 全talkリスト
    talk_turn: int # talkのターン
    role_predictor: RolePredictor # role_predictor

    def __init__(self) -> None:
        """Initialize a new instance of ddhbVillager."""
        self.me = AGENT_NONE
        self.vote_candidate = AGENT_NONE
        self.game_info = None  # type: ignore
        self.comingout_map = defaultdict(lambda: Role.UNC)
        self.divination_reports = []
        self.identification_reports = []
        self.talk_list_head = 0
        self.will_vote_reports = defaultdict(lambda: AGENT_NONE)
        self.talk_list_all = []
        self.talk_turn = 0
        self.role_predictor = None
        self.N = -1
        self.M = -1
        self.agent_idx_0based = -1
        # フルオープンしたかどうか
        self.doFO = False

    # エージェントが生存しているか
    def is_alive(self, agent: Agent) -> bool:
        """Return whether the agent is alive.
        Args:
            agent: The agent.
        Returns:
            True if the agent is alive, otherwise false.
        """
        return self.game_info.status_map[agent] == Status.ALIVE

    # 自分以外のエージェントリスト
    def get_others(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of agents excluding myself from the given list of agents.
        Args:
            agent_list: The list of agent.
        Returns:
            A list of agents excluding myself from agent_list.
        """
        return [a for a in agent_list if a != self.me]

    # 生存するエージェントリスト
    def get_alive(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of alive agents contained in the given list of agents.
        Args:
            agent_list: The list of agents.
        Returns:
            A list of alive agents contained in agent_list.
        """
        return [a for a in agent_list if self.is_alive(a)]

    # 自分以外の生存するエージェントリスト
    def get_alive_others(self, agent_list: List[Agent]) -> List[Agent]:
        """Return a list of alive agents that is contained in the given list of agents
        and is not equal to myself.
        Args:
            agent_list: The list of agents.
        Returns:
            A list of alie agents that is contained in agent_list
            and is not equal to mysef.
        """
        return self.get_alive(self.get_others(agent_list))

    # ランダムセレクト
    def random_select(self, agent_list: List[Agent]) -> Agent:
        """Return one agent randomly chosen from the given list of agents.
        Args:
            agent_list: The list of agents.
        Returns:
            A agent randomly chosen from agent_list.
        """
        return random.choice(agent_list) if agent_list else AGENT_NONE

    def get_co_players(self, agent_list: List[Agent], role: Role = Role.ANY) -> List[Agent]:
        """Return a list of agents who have claimed the given role.
        Args:
            agent_list: The list of agents.
            role: The role. If Role.ANY, return all agents who have claimed any role.
        Returns:
            A list of agents who have claimed the given role.
        """
        return [a for a in agent_list if (role == Role.ANY and self.comingout_map[a] != Role.UNC) or self.comingout_map[a] == role]

    def get_counterparts(self, agent_list: List[Agent], agent: Agent) -> List[Agent]:
        """Return a list of agents who have claimed the same role as the given agent.
        Args:
            agent_list: The list of agents.
            agent: The agent.
        Returns:
            A list of agents who have claimed the same role as the given agent.
            The given agent is not included in the returned list.
        """
        role: Role = self.comingout_map[agent]
        if agent == AGENT_NONE or role == Role.UNC:
            return []
        return self.get_co_players([a for a in agent_list if a != agent], role)

    def convert_to_agentids(self, agent_list: List[Agent]) -> List[int]:
        return [f"Agent[{a.agent_idx}]" for a in agent_list]

    @property
    def alive_comingout_map(self) -> DefaultDict[Agent, Role]:
        return {a: r for a, r in self.comingout_map.items() if self.is_alive(a) and r != Role.UNC}

    @property
    def alive_comingout_map_str(self) -> DefaultDict[str, str]:
        return {a.agent_idx: r.value for a, r in self.alive_comingout_map.items() if self.is_alive(a) and r != Role.UNC}

    @property
    def will_vote_reports_str(self) -> Dict[str, str]:
        return {a.agent_idx: t.agent_idx for a, t in self.will_vote_reports.items()}

    def agent_to_index(self, agent_list: List[Agent]) -> List[int]:
        return [a.agent_idx for a in agent_list]

    def vote_to_dict(self, vote_list: List[Vote]) -> Dict[int, int]:
        return {v.agent.agent_idx: v.target.agent_idx for v in vote_list}

    def vote_cnt(self, vote_list: List[Vote]) -> Dict[Agent, int]:
        count: DefaultDict[Agent, int] = defaultdict(int)
        vote_dict = self.vote_to_dict(vote_list)
        for talker, target in vote_dict.items():
            count[target] += 1
        return count

    def vote_print(self, agent_int: DefaultDict[Agent, int]) -> None:
        return {a.agent_idx: i for a, i in agent_int.items()}

    # include_listから、exclude_listを除いた中で、最も処刑されそうなエージェントを返す
    # 注意：include_listのエージェントが、投票対象に含まれていない場合、自分を返す
    # デフォルトのinclude_listは自分を除いている
    # 注意：is_Low_HPで判定する時は、include_listを指定して自分を含める
    def chooseMostlikelyExecuted(self, include_list: List[Agent] = None, exclude_list: List[Agent] = None) -> Agent:
        if include_list is None:
            include_list = self.get_alive_others(self.game_info.agent_list)
        count: DefaultDict[Agent, int] = defaultdict(int)
        for talker, target in self.will_vote_reports.items():
            if target not in include_list:
                continue
            if exclude_list is not None and target in exclude_list:
                continue
            if self.is_alive(talker) and self.is_alive(target):
                count[target] += 1
        if self.vote_candidate != AGENT_NONE and self.vote_candidate in include_list:
            count[self.vote_candidate] += 1
        count_num = {a.agent_idx: t for a, t in count.items()}
        Util.debug_print("count2:\t", count_num)
        ret_agent: Agent = max(count.items(), key=lambda x: x[1])[0] if count else AGENT_NONE
        return ret_agent

    # include_listから、exclude_listを除いた中で、最も処刑されそうなエージェントを返す
    # 投票宣言と投票先の一致率を反映する
    def chooseMostlikelyExecuted2(self, include_list: List[Agent] = None, exclude_list: List[Agent] = None) -> Agent:
        if include_list is None:
            include_list = self.get_alive_others(self.game_info.agent_list)
        count: DefaultDict[Agent, float] = defaultdict(float)
        for talker, target in self.will_vote_reports.items():
            if target not in include_list:
                continue
            if exclude_list is not None and target in exclude_list:
                continue
            if self.is_alive(talker) and self.is_alive(target):
                vote_count = Util.vote_count[talker]
                vote_match_count = Util.vote_match_count[talker]
                if vote_count > 0:
                    count[target] += vote_match_count / vote_count
        if self.vote_candidate != AGENT_NONE and self.vote_candidate in include_list:
            count[self.vote_candidate] += 1.0
        ret_agent: Agent = max(count.items(), key=lambda x: x[1])[0] if count else AGENT_NONE
        Util.debug_print("executed2:\t", {a.agent_idx: np.round(t, 3) for a, t in count.items()})
        return ret_agent

    # HPが低いかどうか
    def is_Low_HP(self) -> bool:
        # 投票意思：投票が生存者の50%以上で、自分が最も処刑されそうな場合
        alive_cnt = len(self.game_info.alive_agent_list)
        if alive_cnt == 0:
            return False
        will_vote_cnt = len(self.will_vote_reports)
        rate = will_vote_cnt / alive_cnt
        alive_agents: List[Agent] = self.get_alive(self.game_info.agent_list)
        if rate >= 0.5 and self.chooseMostlikelyExecuted2(include_list=alive_agents) == self.me:
            Util.debug_print("is_Low_HP: will_vote")
            return True
        # 前日投票：前日投票の20%以上がが自分に入っている場合
        # latest_vote_listは、day_startで[]となっているため、前日の投票はvote_listに入っている
        vote_cnt = 0
        vote_list = self.game_info.vote_list
        vote_len = len(vote_list)
        if vote_len == 0:
            return False
        for vote in vote_list:
            if vote.target == self.me:
                vote_cnt += 1
        rate = vote_cnt / vote_len
        if rate >= 0.2:
            Util.debug_print("is_Low_HP: latest_vote")
            return True
        return False

    # 同数投票の時に自分の捨て票を変更する：最大投票以外のエージェントに投票している場合、投票先を変更する
    def changeVote(self, vote_list: List[Vote], role: Role, mostlikely=True) -> Agent:
        count: DefaultDict[Agent, int] = defaultdict(int)
        count_num: DefaultDict[str, int] = defaultdict(int)
        my_target: Agent = AGENT_NONE
        new_target: Agent = AGENT_NONE
        for vote in vote_list:
            agent = vote.agent
            target = vote.target
            no = str(target.agent_idx)
            if agent == self.me:
                my_target = target
            count[target] += 1
            count_num[no] += 1
        Util.debug_print('count_num:\t', count_num)
        # 最大投票数を取得
        max_vote = max(count_num.values())
        max_voted_agents: List[Agent] = []
        for agent, num in count.items():
            if num == max_vote and agent != self.me:
                max_voted_agents.append(agent)
        max_voted_agents_num = [a.agent_idx for a in max_voted_agents]
        Util.debug_print('max_voted_agents:\t', max_voted_agents_num)
        # 最大投票数のエージェントが複数人の場合
        if max_voted_agents:
            if mostlikely:
                new_target = self.role_predictor.chooseMostLikely(role, max_voted_agents)
            else:
                new_target = self.role_predictor.chooseLeastLikely(role, max_voted_agents)
        if new_target == AGENT_NONE:
            new_target = my_target
        Util.debug_print('vote_candidate:\t', my_target, '→', new_target)
        return new_target if new_target != AGENT_NONE else self.me

    # 初期化
    def initialize(self, game_info: GameInfo, game_setting: GameSetting) -> None:
        self.game_info = game_info
        self.game_setting = game_setting
        self.me = game_info.me
        # Clear fields not to bring in information from the last game.
        self.comingout_map.clear()
        self.divination_reports.clear()
        self.identification_reports.clear()
        self.talk_list_head = 0
        # 統計
        Util.game_count += 1

        self.will_vote_reports.clear()
        self.talk_list_all = []
        self.talk_turn = 0
        self.score_matrix = ScoreMatrix(game_info, game_setting, self)
        self.role_predictor = RolePredictor(game_info, game_setting, self, self.score_matrix)
        self.N = game_setting.player_num
        self.M = len(game_info.existing_role_list)
        self.agent_idx_0based = self.me.agent_idx - 1

        ActionLogger.initialize(game_info, game_setting)

        # Util.debug_print("game:\t", Util.game_count)
        Util.debug_print("game:\t", Util.game_count - 1)
        Util.debug_print("my role:\t", game_info.my_role)
        Util.debug_print("my idx:\t", self.me)

    # 昼スタート
    def day_start(self) -> None:
        self.talk_list_head = 0
        self.vote_candidate = AGENT_NONE
        day: int = self.game_info.day
        if day >= 2:
            vote_list: List[Vote] = self.game_info.vote_list
            Util.debug_print('vote_list:', self.vote_to_dict(vote_list))
            Util.debug_print('will_vote_reports:', self.will_vote_reports_str)
            for v in vote_list:
                self.score_matrix.vote(self.game_info, self.game_setting, v.agent, v.target, v.day)
                va = v.agent
                vt = v.target
                if va in self.will_vote_reports:
                    Util.vote_count[va] += 1
                    if vt == self.will_vote_reports[va]:
                        Util.vote_match_count[va] += 1
            Util.debug_print("vote_count:\t", self.vote_print(Util.vote_count))
            Util.debug_print("vote_match_count:\t", self.vote_print(Util.vote_match_count))
        self.will_vote_reports.clear()

        Util.debug_print("")
        Util.debug_print("DayStart:\t", self.game_info.day)
        Util.debug_print("生存者数:\t", len(self.game_info.alive_agent_list))

        Util.debug_print("Executed:\t", self.game_info.executed_agent)
        if self.game_info.executed_agent == self.me:
            Util.debug_print("---------- 処刑された ----------")
        # self.game_info.last_dead_agent_list は昨夜殺されたエージェントのリスト
        # (self.game_info.executed_agent が昨夜処刑されたエージェント)
        killed: List[Agent] = self.game_info.last_dead_agent_list
        if len(killed) > 0:
            self.score_matrix.killed(self.game_info, self.game_setting, killed[0])
            Util.debug_print("Killed:\t", self.game_info.last_dead_agent_list[0])
            if self.game_info.last_dead_agent_list[0] == self.me:
                Util.debug_print("---------- 噛まれた ----------")
            # 本来複数人殺されることはないが、念のためkilled()は呼び出した上でエラーログを出しておく
            if len(killed) > 1:
                Util.error_print("Killed:\t", *self.game_info.last_dead_agent_list)
        else:
            Util.debug_print("Killed:\t", AGENT_NONE)
        # 噛まれていない違和感を反映
        self.score_matrix.Nth_day_start(self.game_info, self.game_setting)

    # ゲーム情報の更新
    # talk-listの処理
    def update(self, game_info: GameInfo) -> None:
        self.game_info = game_info  # Update game information.
        self.score_matrix.update(game_info)
        Util.start_timer("Villager.update")
        timeout = 20
        for i in range(self.talk_list_head, len(game_info.talk_list)):  # Analyze talks that have not been analyzed yet.
            tk: Talk = game_info.talk_list[i]  # The talk to be analyzed.
            day: int = tk.day
            turn: int = tk.turn
            talker: Agent = tk.agent
            self.talk_list_all.append(tk)
            if talker == self.me:  # Skip my talk.
                continue
            if RealPossessedDetector.should_skip(self, tk):
                continue
            # 内容に応じて更新していく
            content: Content = Content.compile(tk.text)

            # Util.debug_print("Topic:\t", talker, content.topic)

            # content.target が不要な場合デフォルトの AGENT_ANY が入っている
            # 不正な場合はここで弾く
            if content.target == AGENT_NONE or (content.target != AGENT_ANY and content.target.agent_idx > self.N):
                Util.debug_print("Invalid target:\t", content.target)
                continue
            # target が不要なトピック以外で AGENT_ANY が入っている場合は弾く
            if content.target == AGENT_ANY:
                if content.topic not in [Topic.Over, Topic.Skip, Topic.OPERATOR, Topic.AGREE, Topic.DISAGREE, Topic.DUMMY]:
                    Util.debug_print("Invalid target:\t", content.topic, content.target)
                    continue

            if content.topic == Topic.COMINGOUT:
                if content.role in self.game_info.existing_role_list: # Role.UNC 対策
                    self.comingout_map[talker] = content.role
                    self.score_matrix.talk_co(self.game_info, self.game_setting, talker, content.role, day, turn)
                Util.debug_print("CO:\t", talker, content.role)
            elif content.topic == Topic.DIVINED:
                self.score_matrix.talk_divined(self.game_info, self.game_setting, talker, content.target, content.result, day, turn)
                self.divination_reports.append(Judge(talker, day, content.target, content.result))
                Util.debug_print("DIVINED:\t", talker, content.target, content.result)
            elif content.topic == Topic.IDENTIFIED:
                self.score_matrix.talk_identified(self.game_info, self.game_setting, talker, content.target, content.result, day, turn)
                self.identification_reports.append(Judge(talker, day, content.target, content.result))
                Util.debug_print("IDENTIFIED:\t", talker, content.target, content.result)
            elif content.topic == Topic.VOTE:
                # 古い投票先が上書きされる前にスコアを更新 (2回以上投票宣言している場合に信頼度を下げるため)
                self.score_matrix.talk_will_vote(self.game_info, self.game_setting, talker, content.target, day, turn)
                # 投票先を保存
                self.will_vote_reports[talker] = content.target
            elif content.topic == Topic.VOTED:
                self.score_matrix.talk_voted(self.game_info, self.game_setting, talker, content.target, day, turn)
            elif content.topic == Topic.GUARDED:
                self.score_matrix.talk_guarded(self.game_info, self.game_setting, talker, content.target, day, turn)
                Util.debug_print("GUARDED:\t", talker, content.target)
            elif content.topic == Topic.ESTIMATE:
                if content.role == Role.WEREWOLF:
                    self.score_matrix.talk_will_vote(self.game_info, self.game_setting, talker, content.target, day, turn)
                    self.will_vote_reports[talker] = content.target
                elif content.role == Role.VILLAGER:
                    self.score_matrix.talk_estimate(self.game_info, self.game_setting, talker, content.target, content.role, day, turn)
            elif content.topic == Topic.OPERATOR and content.operator == Operator.REQUEST and content.content_list[0].topic == Topic.VOTE:
                self.score_matrix.talk_will_vote(self.game_info, self.game_setting, talker, content.content_list[0].target, day, turn)
                self.will_vote_reports[talker] = content.content_list[0].target

            action: Action = ActionLogger.update(game_info, tk, content, self)
            score = ActionLogger.get_score(day, turn, talker, action)
            self.score_matrix.apply_action_learning(talker, score)

            if action in [Action.DIVINED_CONTRADICT, Action.DIVINED_WITHOUT_CO, Action.IDENTIFIED_WITHOUT_CO, Action.IDENTIFIED_WITHOUT_CO_TO_COUNTERPART, Action.IDENTIFIED_TO_ALIVE]:
                Util.debug_print("Action:\t", talker, action)

            self.talk_list_head += 1

            if Util.timeout("Villager.update", timeout):
                break

    # CO、投票宣言
    def talk(self) -> Content:
        day: int = self.game_info.day
        turn: int = self.talk_turn
        self.vote_candidate = self.vote()
        # ---------- 5人村 ----------
        if self.N == 5:
            if day == 1:
                if turn == 1:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(ComingoutContentBuilder(AGENT_ANY, Role.ANY))))
                elif 2 <= turn <= 8:
                    rnd = random.randint(0, 2)
                    if rnd == 0:
                        return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
                    elif rnd == 1:
                        return Content(VoteContentBuilder(self.vote_candidate))
                    else:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
                else:
                    return CONTENT_SKIP
            elif day >= 2:
                # 2日目：狂人COを認知→狂人がいるか判定→いる場合、人狼CO
                agent_possessed: Agent = self.role_predictor.chooseMostLikely(Role.POSSESSED, self.game_info.agent_list, 0.4)
                if agent_possessed != AGENT_NONE:
                    alive_possessed = self.is_alive(agent_possessed)
                    if turn == 1 and alive_possessed:
                        return Content(ComingoutContentBuilder(self.me, Role.WEREWOLF))
                if 1 <= turn <= 6:
                    rnd = random.randint(0, 2)
                    if rnd == 0:
                        return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
                    elif rnd == 1:
                        return Content(VoteContentBuilder(self.vote_candidate))
                    else:
                        return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
                else:
                    return CONTENT_SKIP
            else:
                return CONTENT_SKIP
        # ---------- 15人村 ----------
        elif self.N == 15:
            if day == 1 and turn == 1:
                return Content(RequestContentBuilder(AGENT_ANY, Content(ComingoutContentBuilder(AGENT_ANY, Role.ANY))))
            # ----- ESTIMATE, VOTE, REQUEST -----
            elif turn <= 7:
                rnd = random.randint(0, 2)
                if rnd == 0:
                    return Content(EstimateContentBuilder(self.vote_candidate, Role.WEREWOLF))
                elif rnd == 1:
                    return Content(VoteContentBuilder(self.vote_candidate))
                else:
                    return Content(RequestContentBuilder(AGENT_ANY, Content(VoteContentBuilder(self.vote_candidate))))
            else:
                return CONTENT_SKIP
        return CONTENT_SKIP

    # 投票対象
    def vote(self) -> Agent:
        #  ---------- 同数投票の処理 ----------
        latest_vote_list = self.game_info.latest_vote_list
        if latest_vote_list:
            self.vote_candidate = self.changeVote(latest_vote_list, Role.WEREWOLF)
            return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me
        # 投票候補
        vote_candidates: List[Agent] = self.get_alive_others(self.game_info.agent_list)
        # ---------- 5人村 ----------
        if self.N == 5:
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ---------- 15人村 ----------
        elif self.N == 15:
            # 投票対象の優先順位：偽占い→人狼っぽいエージェント
            fake_seers: List[Agent] = [j.agent for j in self.divination_reports if j.target == self.me and j.result == Species.WEREWOLF]
            alive_fake_seers: List[Agent] = self.get_alive_others(fake_seers)
            if alive_fake_seers:
                Util.debug_print("alive_fake_seers:\t", self.agent_to_index(alive_fake_seers))
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, alive_fake_seers)
            else:
                Util.debug_print("vote_candidates:\t", self.agent_to_index(vote_candidates))
                self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        # ----- 投票ミスを防ぐ -----
        if self.vote_candidate == AGENT_NONE or self.vote_candidate == self.me:
            Util.debug_print("vote_candidates: AGENT_NONE or self.me")
            self.vote_candidate = self.role_predictor.chooseMostLikely(Role.WEREWOLF, vote_candidates)
        return self.vote_candidate if self.vote_candidate != AGENT_NONE else self.me

    def attack(self) -> Agent:
        raise NotImplementedError()

    def divine(self) -> Agent:
        raise NotImplementedError()

    def guard(self) -> Agent:
        raise NotImplementedError()

    def whisper(self) -> Content:
        raise NotImplementedError()

    def finish(self) -> None:
        vote_list: List[Vote] = self.game_info.vote_list
        Util.debug_print('vote_list:', self.vote_to_dict(vote_list))
        Util.debug_print('will_vote_reports:', self.will_vote_reports_str)
        for v in vote_list:
            self.score_matrix.vote(self.game_info, self.game_setting, v.agent, v.target, v.day)
            va = v.agent
            vt = v.target
            if va in self.will_vote_reports:
                Util.vote_count[va] += 1
                if vt == self.will_vote_reports[va]:
                    Util.vote_match_count[va] += 1
        Util.debug_print("vote_count:\t", self.vote_print(Util.vote_count))
        Util.debug_print("vote_match_count:\t", self.vote_print(Util.vote_match_count))
        # 自分が人狼陣営で人狼が生存しているか、自分が村人陣営で人狼が生存していない場合に勝利
        alive_wolves = [a for a in self.game_info.alive_agent_list if self.game_info.role_map[a] == Role.WEREWOLF]
        villagers_win = (len(alive_wolves) == 0)
        is_villagers_side = self.game_info.my_role in [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
        Util.update_win_rate(self.game_info, villagers_win)

        Util.debug_print("")
        Util.debug_print("win:\t", is_villagers_side == villagers_win)
        Util.debug_print("win_rate:\t", Util.win_count[self.me], "/", Util.game_count, " = ", Util.win_rate[self.me])
        Util.debug_print("")

        ActionLogger.finish(self.game_info)
        self.score_matrix.finish(self.game_info)

        if (len(self.role_predictor.assignments) == 0):
            Util.debug_print("No assignments")
            return

        # 確率を表示
        p = self.role_predictor.getProbAll()
        Util.debug_print("", end="\t")
        for r in self.game_info.existing_role_list:
            Util.debug_print(r.name, end="\t")
        Util.debug_print("")
        for a in self.game_info.agent_list:
            Util.debug_print(a, end="\t")
            for r in self.game_info.existing_role_list:
                if p[a][r] > 0.005:
                    Util.debug_print(round(p[a][r], 2), end="\t")
                else:
                    Util.debug_print("-", end="\t")
            Util.debug_print("")
        Util.debug_print("")

        # COを表示
        for a, r in self.comingout_map.items():
            if r != Role.UNC:
                Util.debug_print("CO:\t", a, r)
        Util.debug_print("")

        # 実際の割り当てと予測の割り当てを比較
        assignment = []
        for a, r in self.game_info.role_map.items():
            assignment.append(r)
        actual_assignment = Assignment(self.game_info, self.game_setting, self, assignment)
        predicted_assignment = self.role_predictor.assignments[-1]
        Util.debug_print("\t", "1, 2, 3, 4, 5, 6, 7, 8, 9, A, B, C, D, E, F")
        Util.debug_print("actual:\t", actual_assignment)
        Util.debug_print("predicted:\t", predicted_assignment)

        # # 一致率を計算
        # score = 0
        # for i in range(self.N):
        #     if predicted_assignment[i] == actual_assignment[i]:
        #         score += 1
        #     elif predicted_assignment[i] == Role.MEDIUM or actual_assignment[i] == Role.MEDIUM or predicted_assignment[i] == Role.SEER or actual_assignment[i] == Role.SEER:
        #         Util.debug_print("")
        #         for r in self.game_info.existing_role_list:
        #             Util.debug_print(self.game_info.agent_list[i], "\t", r, "\t", round(self.role_predictor.getProb(i, r), 2))

        # Util.sum_score += score
        # Util.debug_print("")
        # Util.debug_print("score:\t", score, "/", self.N)
        # Util.debug_print("rate:\t", Util.sum_score, "/", Util.game_count, "=", round(Util.sum_score / Util.game_count, 2))
        # Util.debug_print("")

        # # 実際の割り当てが予測の割り当てに含まれていたのか
        # Util.debug_print("in role_predictor.assignments:\t", actual_assignment in self.role_predictor.assignments)
        # Util.debug_print("in role_predictor.assignments(set):\t", actual_assignment in self.role_predictor.assignments_set)
        # Util.debug_print("len(role_predictor.assignments(set):\t", len(self.role_predictor.assignments_set))
        # Util.debug_print("")

        # # もし含まれていないなら、含まれていたときのスコアを表示
        # if actual_assignment not in self.role_predictor.assignments:
        #     actual_assignment.evaluate(self.score_matrix)

        # 予測の割り当てのスコアを表示 (デバッグモード)
        predicted_assignment.evaluate(self.score_matrix, debug=True)
        Util.debug_print("")
        Util.debug_print("best score:\t", round(predicted_assignment.score, 4))
        Util.debug_print("")

        # 実際の割り当てのスコアを表示 (デバッグモード)
        actual_assignment.evaluate(self.score_matrix, debug=True)
        Util.debug_print("")
        Util.debug_print("actual score:\t", round(actual_assignment.score, 4))

        # # 最下位の割り当てのスコアを表示
        # Util.debug_print("")
        # Util.debug_print("worst score:\t", round(self.role_predictor.assignments[0].score, 4))
        # Util.debug_print("")

        # COしていない人から占い師、霊媒師、狩人が選ばれてはいないかのチェック
        for a in self.game_info.agent_list:
            if predicted_assignment[a] in [Role.SEER, Role.MEDIUM, Role.BODYGUARD] and predicted_assignment[a] != self.comingout_map[a]:
                Util.debug_print(a, "CO", self.comingout_map[a] if a in self.comingout_map else Role.UNC, "but assigned", predicted_assignment[a])
        Util.debug_print("")

        # for a in self.get_co_players(self.game_info.agent_list):
        #     Util.debug_print(a, self.comingout_map[a], self.convert_to_agentids(self.get_counterparts(self.game_info.agent_list, a)))
