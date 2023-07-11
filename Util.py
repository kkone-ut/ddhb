from collections import Counter
from heapq import merge
from itertools import product
import sys
from collections import deque, defaultdict

from aiwolf import Role, GameInfo, Agent, Role
from aiwolf.constant import AGENT_NONE
import time
import traceback
from typing import Dict, DefaultDict, List
import library.timeout_decorator as timeout_decorator

class Util:

    exit_on_error = False
    local = False
    need_traceback = True

    rtoi = {Role.VILLAGER: 0, Role.SEER: 1, Role.POSSESSED: 2, Role.WEREWOLF: 3, Role.MEDIUM: 4, Role.BODYGUARD: 5}
    debug_mode = True
    time_start = Dict[str, float]

    game_count: int = 0
    win_count: DefaultDict[Agent, int] = {}
    win_rate: DefaultDict[Agent, float] = {}
    sum_score: float = 0.0

    @staticmethod
    def init():
        Util.time_start = {}
        Util.game_count = 0
        Util.win_count = defaultdict(int)
        Util.win_rate = defaultdict(float)
        Util.sum_score = 0

    @staticmethod
    def debug_print(*args, **kwargs):
        # if type(args[0]) == str and ("exec_time" in args[0] or "len(self.assignments)" in args[0]):
        #     return
        if Util.debug_mode:
            print(*args, **kwargs)

    @staticmethod
    def error_print(*args, **kwargs):
        print(*args, **kwargs, file=sys.stderr)
        if Util.local and Util.exit_on_error:
            if Util.need_traceback:
                traceback.print_stack()
            exit(1)

    @staticmethod
    def start_timer(func_name):
        Util.time_start[func_name] = time.time()
    
    @staticmethod
    def end_timer(func_name, time_threshold=0):
        time_end = time.time()
        time_exec = round((time_end - Util.time_start[func_name]) * 1000, 1)
        if time_exec >= time_threshold:
            if time_threshold == 0:
                Util.debug_print("exec_time:\t", func_name, time_exec)
            else:
                Util.error_print("exec_time:\t", func_name, time_exec)
    
    @staticmethod
    def timeout(func_name, time_threshold):
        time_now = time.time()
        time_exec = round((time_now - Util.time_start[func_name]) * 1000, 1)
        return time_exec >= time_threshold
    
    @staticmethod
    def exec_with_timeout(func, timeout, *args, **kwargs):

        @timeout_decorator.timeout(timeout / 1000)
        def _exec_with_timeout():
            return func(*args, **kwargs)
        
        try:
            return _exec_with_timeout()
        except timeout_decorator.TimeoutError:
            Util.error_print("TimeoutError:\t", func.__name__, timeout, "ms")
            return None
    
    @staticmethod
    def update_win_rate(game_info: GameInfo, villager_win: bool):
        for agent, role in game_info.role_map.items():
            is_villager_side = role in [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
            win = villager_win if is_villager_side else not villager_win
            if win:
                Util.win_count[agent] += 1
            Util.win_rate[agent] = Util.win_count[agent] / Util.game_count
        for agent in game_info.agent_list:
            Util.debug_print("win_rate:\t", agent, Util.win_rate[agent])


    @staticmethod
    def get_strong_agent(agent_list: List[Agent], threshold: float = 0.0) -> Agent:
        rate = threshold
        strong_agent = AGENT_NONE
        for agent in agent_list:
            if Util.win_rate[agent] > rate:
                rate = Util.win_rate[agent]
                strong_agent = agent

        return strong_agent


    @staticmethod
    def unique_permutations_stack(lst, fixed_positions=None):
        if fixed_positions is None:
            fixed_positions = {}

        counter = Counter(lst)

        for pos, val in fixed_positions.items():
            counter[val] -= 1

        unique_elems = list(counter.keys())
        counts = list(counter.values())
        n = len(lst)

        stack = deque([([], counts, 0)])
        
        while stack:
            current_perm, remaining_counts, current_length = stack.pop()

            if current_length == n:
                yield tuple(current_perm)
                continue

            if current_length in fixed_positions:
                stack.append((current_perm + [fixed_positions[current_length]], remaining_counts, current_length + 1))
            else:
                for idx, (elem, count) in reversed(list(enumerate(zip(unique_elems, remaining_counts)))):
                    if count > 0:
                        new_remaining_counts = remaining_counts.copy()
                        new_remaining_counts[idx] -= 1
                        stack.append((current_perm + [elem], new_remaining_counts, current_length + 1))

    @staticmethod
    def get_unique_permutations_queue(ls, fixed_positions=set()):
        # キューを使って unique_permutations を実装する
        # ただし、fixed_positions で指定した位置を固定する
        
        permutation = [None] * len(ls)
        is_used = [False] * len(ls)
        stack = deque()
        for a in set(ls):
            stack.append(a)

        while len(stack) > 0:
            a = stack.pop()

    @staticmethod
    def get_unique_permutations(ls, fixed_positions=set()):

        def _dfs(ls, permutation, is_used, i):
            if i == len(ls):
                yield tuple(ls)
                return

            if len(fixed_positions) > 0 and i in fixed_positions:
                permutation[i] = ls[i]
                is_used[i] = True
                yield from _dfs(ls, permutation, is_used, i + 1)
                return

            is_selected = set()
            for j in range(len(ls)):
                if not is_used[j] and ls[j] not in is_selected:
                    is_selected.add(ls[j])
                    is_used[j] = True
                    permutation[i] = ls[j]
                    yield from _dfs(ls, permutation, is_used, i + 1)
                    permutation[i] = None
                    is_used[j] = False
        
        return _dfs(ls, [None] * len(ls), [False] * len(ls), 0)

    # 基本的には set(itertools.permutations) と同じ
    # ただし、fixed_positions で指定した位置に固定値を入れることができる
    @staticmethod
    def unique_permutations(lst, fixed_positions=None):
        if fixed_positions is None:
            fixed_positions = {}

        counter = Counter(lst)
        
        for pos, val in fixed_positions.items():
            counter[val] -= 1
        
        unique_elems = list(counter.keys())
        counts = list(counter.values())
        n = len(lst)
        
        def _unique_permutations(current_perm, remaining_counts, current_length):
            if current_length == n:
                yield tuple(current_perm)
                return

            if current_length in fixed_positions:
                yield from _unique_permutations(current_perm + [fixed_positions[current_length]], remaining_counts, current_length + 1)
            else:
                for idx, (elem, count) in enumerate(zip(unique_elems, remaining_counts)):
                    if count > 0:
                        remaining_counts[idx] -= 1
                        yield from _unique_permutations(current_perm + [elem], remaining_counts, current_length + 1)
                        remaining_counts[idx] += 1

        return _unique_permutations([], counts, 0)
