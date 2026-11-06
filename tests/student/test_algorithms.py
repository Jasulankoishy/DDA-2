"""Independent small oracles and full-domain stress tests. Run from repository root."""
import itertools
import json
import pathlib
import random
import subprocess
import sys
from functools import lru_cache

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from run import build, execute
from public_validate import check


def occurrences(text, pattern):
    return [i for i in range(len(text) - len(pattern) + 1) if text.startswith(pattern, i)]


def lcs_oracle(a, b):
    best = 0
    for mask in range(1 << len(a)):
        s = ''.join(c for i, c in enumerate(a) if mask >> i & 1)
        it = iter(b)
        if all(any(v == c for v in it) for c in s):
            best = max(best, len(s))
    return {'length': best}


@lru_cache(None)
def merge_oracle(sizes):
    if len(sizes) <= 1:
        return 0
    return min(sizes[i] + sizes[i + 1] + merge_oracle(
        sizes[:i] + (sizes[i] + sizes[i + 1],) + sizes[i + 2:])
        for i in range(len(sizes) - 1))


def tree_oracle(weights, edges):
    return max(sum(w for i, w in enumerate(weights) if mask >> i & 1)
               for mask in range(1 << len(weights))
               if all(not (mask >> u & 1 and mask >> v & 1) for u, v in edges))


def suffix_oracle(text):
    sa = sorted(range(len(text)), key=lambda i: text[i:])
    lcp = [0] * len(text)
    for j in range(1, len(sa)):
        a, b = sa[j - 1], sa[j]
        while b + lcp[j] < len(text) and a + lcp[j] < len(text) and text[a + lcp[j]] == text[b + lcp[j]]:
            lcp[j] += 1
    best = max(range(len(sa)), key=lambda i: lcp[i], default=0)
    return {'sa': sa, 'lcp': lcp, 'repeated': text[sa[best]:sa[best] + lcp[best]] if sa else ''}


def main():
    rng = random.Random(2537)
    cases = []

    def add(name, request, expected):
        cases.append({'name': name, 'input': request, 'output': expected})

    for a, b in [('', ''), ('', 'abc'), ('abc', ''), ('ab', 'ba'), ('tea_time', 'team')]:
        add('lcs-boundary', dict(task='lcs', a=a, b=b), lcs_oracle(a, b))
    for _ in range(80):
        a = ''.join(rng.choices('ab !', k=rng.randrange(9)))
        b = ''.join(rng.choices('ab !', k=rng.randrange(9)))
        add('lcs-exhaustive-subsequences', dict(task='lcs', a=a, b=b), lcs_oracle(a, b))
    for sizes in [[], [7], [0] * 7, [1, 100, 1], [10**9] * 7] + [rng.choices(range(9), k=rng.randrange(8)) for _ in range(70)]:
        add('merge-enumerate-chronological-choices', dict(task='merge', sizes=sizes), {'cost': merge_oracle(tuple(sizes))})
    for n in range(12):
        for _ in range(6):
            weights = rng.choices(range(-5, 8), k=n)
            edges = [[v, rng.randrange(v)] for v in range(1, n)]
            add('tree-enumerate-subsets', dict(task='tree', n=n, weights=weights, edges=edges), {'weight': tree_oracle(weights, edges)})
    pairs = [('', ''), ('abc', ''), ('', 'a'), ('a', 'aa'), ('aaaaa', 'aaa'), ('abdj', 'ab'), ('~ !~ !', '~ !')]
    alphabet = ''.join(chr(i) for i in range(32, 127))
    pairs += [(''.join(rng.choices(alphabet, k=rng.randrange(70))), ''.join(rng.choices(alphabet, k=rng.randrange(12)))) for _ in range(100)]
    pairs += [(''.join(rng.choices('ab', k=60)), ''.join(rng.choices('ab', k=6))) for _ in range(60)]
    for text, pattern in pairs:
        for task in ['naive', 'kmp', 'rk']:
            add('search-independent-startswith', dict(task=task, text=text, pattern=pattern), {'positions': occurrences(text, pattern)})
    for text in ['', 'x', 'abcdef', 'aaaaaa', 'banana', 'ababcdcd'] + [''.join(rng.choices('ab !~', k=rng.randrange(50))) for _ in range(100)]:
        add('suffix-direct-suffix-sort-and-lcp', dict(task='suffix', text=text), suffix_oracle(text))
    dictionaries = [([], ['', 'abc']), (['he', 'she', 'he'], ['sheshe', 'sheshe', '']), (['a', 'aa', 'aaa'], ['aaaaa']), (['x'], [])]
    for _ in range(70):
        patterns = [''.join(rng.choices('ab !', k=rng.randrange(1, 6))) for _ in range(rng.randrange(15))]
        texts = [''.join(rng.choices('ab !', k=rng.randrange(50))) for _ in range(rng.randrange(5))]
        dictionaries.append((patterns, texts))
    for patterns, texts in dictionaries:
        add('aho-independent-search-per-pattern', dict(task='aho', patterns=patterns, texts=texts), {'counts': [[len(occurrences(t, p)) for p in patterns] for t in texts]})

    # Exact known optima at the published limits; no expensive large oracle.
    add('lcs-max', dict(task='lcs', a='a' * 1000, b='a' * 1000), {'length': 1000})
    # Equal unit leaves: optimal external path length at depths h and h+1.
    h, n = 7, 200
    add('merge-max-64bit', dict(task='merge', sizes=[10**9] * n), {'cost': (n * (h + 2) - 2 ** (h + 1)) * 10**9})
    for name, edges, expected in [
        ('chain', [[i - 1, i] for i in range(1, 100000)], 50000 * 10**9),
        ('star', [[0, i] for i in range(1, 100000)], 99999 * 10**9)]:
        add('tree-max-' + name, dict(task='tree', n=100000, weights=[10**9] * 100000, edges=edges), {'weight': expected})
    add('tree-negative', dict(task='tree', n=3, weights=[-1, -2, -3], edges=[[0, 1], [1, 2]]), {'weight': 0})
    for task, n, m in [('naive', 3000, 1500), ('kmp', 200000, 100000), ('rk', 10000, 2000)]:
        for pattern in ['a' * m, 'a' * (m - 1) + 'b']:
            add(task + '-max', dict(task=task, text='a' * n, pattern=pattern), {'positions': list(range(n - m + 1)) if pattern[-1] == 'a' else []})
        add(task + '-empty-max', dict(task=task, text='x' * n, pattern=''), {'positions': list(range(n + 1))})
    n = 50000
    add('suffix-max-equal', dict(task='suffix', text='a' * n), {'sa': list(range(n - 1, -1, -1)), 'lcp': list(range(n)), 'repeated': 'a' * (n - 1)})
    add('aho-max-dense-duplicates', dict(task='aho', patterns=['a' * 10] * 10000, texts=['a' * 100000] * 2), {'counts': [[99991] * 10000] * 2})
    # 100001 trie states, 100 independent texts; propagation and memory limits.
    patterns = [''.join(rng.choices(alphabet, k=10)) for _ in range(10000)]
    add('aho-max-distinct', dict(task='aho', patterns=patterns, texts=[''] * 100), {'counts': [[0] * 10000] * 100})
    # Keep the small reproducible fixture file; stress data is generated on demand.
    small = [c for c in cases if '-max' not in c['name']]
    (ROOT / 'tests/student/fixtures.json').write_text(json.dumps(small, indent=2), encoding='utf-8')
    command = build('Java')
    count = 0
    # Separate batches keep retained input/output bounded; every batch mixes tasks.
    for start in range(0, len(cases), 100):
        batch = cases[start:start + 100]
        answers = execute(command, [c['input'] for c in batch])
        for case, answer in zip(batch, answers):
            assert check(case['input'], answer, case['output']), (case['name'], str(answer)[:500])
            count += 1
    reset = cases[:12] + cases[-3:] + cases[:12]
    answers = execute(command, [c['input'] for c in reset])
    assert all(check(c['input'], y, c['output']) for c, y in zip(reset, answers))
    count += len(reset)
    summary = {'seed': 2537, 'validated_requests': count, 'generated_cases': len(cases), 'result': 'PASS', 'large_domains': 'All eight commands, exact values and witnesses checked'}
    (ROOT / 'tests/student/results.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(summary))


if __name__ == '__main__':
    main()
