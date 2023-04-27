from collections import Counter
from heapq import merge
from itertools import product
import sys

class Util:

    def debug_print(*args, **kwargs):
        # デバッグログが要らない場合は次の行をコメントアウトする
        print(*args, **kwargs)
        pass

    def error_print(*args, **kwargs):
        # エラーログが要らない場合は次の行をコメントアウトする
        print(*args, **kwargs, file=sys.stderr)
        pass

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
