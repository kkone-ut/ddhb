from collections import Counter
from heapq import merge
from itertools import product
import sys
from collections import deque

from aiwolf import Role

class Util:

    rtoi = {Role.VILLAGER: 0, Role.SEER: 1, Role.POSSESSED: 2, Role.WEREWOLF: 3, Role.MEDIUM: 4, Role.BODYGUARD: 5}
    debug_mode = False

    def debug_print(*args, **kwargs):
        # デバッグログが要らない場合は次の行をコメントアウトする
        print(*args, **kwargs)
        pass

    def error_print(*args, **kwargs):
        # エラーログが要らない場合は次の行をコメントアウトする
        print(*args, **kwargs, file=sys.stderr)
        pass

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
