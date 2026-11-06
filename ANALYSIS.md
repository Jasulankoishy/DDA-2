# Assignment 4 — Dynamic Programming and String Processing

**Student:** Zhasulan Koishybay  
**Group:** 2537-SE  
**Selected language:** Java

## Environment and reproducible commands

Measured on Windows 11 (build 26300), Intel Core i5-11400H @ 2.70 GHz,
OpenJDK 25.0.1+8-27, Python 3.13.0. Compilation targets Java 17 using
`javac --release 17`. RAM capacity could not be read through CIM in this
environment; the launcher and benchmark use a maximum Java heap of 384 MiB.
No hardware capacity or timing values have been estimated.

Run these commands from the repository root after putting a JDK 17+ `bin`
directory and Python 3.12+ on PATH:

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input examples/lcs.jsonl
python tests/student/test_algorithms.py
javac --release 17 -encoding UTF-8 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark > tests/student/benchmark.csv
```

On the measured Windows host the JDK was not initially on PATH. The session
used this PowerShell prefix (it changes only the current process environment):

```powershell
$env:PATH = 'C:\Users\User\.jdks\openjdk-25.0.1\bin;' + $env:PATH
```

Only `Java/source/Algorithms.java` implements the solution; the supplied JSON
parser, launcher and other language templates are preserved. Every call owns
its state, so different commands and repeated requests are independent.
The supplied transport uses UTF-8 JSON Lines and emits no diagnostic stdout.
Indices are zero-based. Accumulated merge costs, tree weights and Aho counts
use Java `long`; lengths, positions and ranks fit in `int`.

## 1. LCS

`dp[i][j]` is the maximum common subsequence length of prefixes `a[0..i)`
and `b[0..j)`. The empty row and column are zero. If the last characters
match, `dp[i][j]=dp[i-1][j-1]+1`; otherwise it is the maximum of
`dp[i-1][j]` and `dp[i][j-1]`. Rows and columns increase so all dependencies
already exist.

When endpoints differ, any common subsequence omits at least one endpoint,
so one of the two shorter-prefix optima is sufficient. With matching
endpoints, appending this character to an optimum of the shorter prefixes
achieves the recurrence; an optimum can be chosen to use these endpoints.
Induction on prefix lengths proves the table optimal.

Reconstruction starts at `(n,m)`. Equal characters contribute a character and
move diagonally; a mismatch follows a neighbour preserving the optimum.
The collected reverse sequence is reversed once. Ties prefer moving up;
the specification permits alternative optimal witnesses.

Time and auxiliary storage are `O((n+1)(m+1))`; input storage is `O(n+m)`
and output is `O(length+1)`. `tea_time/team` detects accidental substring
logic; `ab/ba` checks that a nonunique witness remains valid. Empty inputs
return zero and the empty string.

## 2. Ordered adjacent-block merging

`dp[l][r]` is the minimum cost for merging the closed original interval
`[l,r]` into one block. Diagonal states cost zero. Prefix sums give the
interval weight in constant time. Every final merge has a split `k` with
`l <= k < r`; its cost is `dp[l][k]+dp[k+1][r]+sum(l..r)`.
Intervals are processed by increasing length.

Every legal sequence has such a last split and its two subplans cannot
cost less than their DP optima. Conversely, concatenating optimal subplans
and the final merge is legal and attains that candidate. Taking the minimum
therefore gives exactly the optimum by induction on interval length.

The split table stores the first best `k`. Reconstruction recursively emits
the left subplan, right subplan, then `[l,k,r]`, ensuring the two blocks
exist when used. Recursion depth is at most 200; the deep-tree algorithm
below deliberately does not use recursion. With zero or one block the
answer is cost zero and no merges.

For `n>=1`, time is `O(n^3)`, auxiliary storage `O(n^2)` including tables
and the `O(n)` reconstruction stack; input and output are `O(n)`.
The empty case takes `O(1)`. `1,100,1` detects illegal nonadjacent merging;
zero blocks and all-zero sizes check ties. Two hundred weights of `10^9`
give `1544000000000`, detecting 32-bit overflow. The test runner replays
every emitted merge, recomputes cost and requires exactly `n-1` merges.

## 3. Maximum-weight independent set on a tree

The tree is rooted at vertex zero. An iterative traversal records parents
and an order with parents before children. Reversing that order is a valid
postorder. `take[v]` is the best weight in the subtree when `v` is selected;
`skip[v]` is the best when it is excluded:

```
take[v] = weights[v] + sum(skip[u])
skip[v] = sum(max(take[u], skip[u]))
```

The sums range over children. A leaf has `take=weight` and `skip=0`.
There are no edges between distinct child subtrees, so after fixing `v`
their choices are independent. These equations exhaust both possibilities
and prove optimality by postorder induction. The answer is the maximum
of the two root states. Negative weights can always be skipped.

Reconstruction visits parents first. A selected parent forces its child to
skip. Otherwise take is chosen exactly when it is strictly better; ties
skip. Each vertex is examined once and no adjacent pair is selected.
All-negative inputs return the empty set. Empty trees return zero directly.

Primitive adjacency arrays, parent/order arrays, DP and decisions use
`O(n+1)` auxiliary storage and time. Input edges/weights and the selected
output also use `O(n+1)`. A chain of 100000 vertices checks stack safety;
unit-billion weights give `50000000000000` for that chain and
`99999000000000` for a star. Exhaustive subset tests validate the optimum,
uniqueness of selected IDs, total weight and independence.

## 4. Exact string searches

### Naive

Every candidate start from zero through `n-m` is tested by character
comparisons. At comparison index `j`, the previous `j` characters are known
equal. Reaching `j=m` proves a match; any mismatch disproves it. Advancing
the start by one preserves overlaps. For `m=0`, the loop naturally reports
all `n+1` boundaries; for `m>n` it performs no iteration.

Time is `O(n*m+n+1)` including output; auxiliary space is `O(1)` and
output space `O(z+1)`. Input strings occupy `O(n+m+1)`.

### KMP

`pi[i]` is the longest proper prefix of `pattern[0..i]` which is also a
suffix. Construction increases `i` and falls back from border length `j`
to `pi[j-1]` on mismatch. Those are the only possible shorter borders,
so no feasible prefix is skipped.

During scanning, `j` is the matched prefix length at the current text end.
The same fallback finds the longest prefix still compatible after a
mismatch. When `j=m`, the start is emitted and `j=pi[m-1]` retains a
possible overlap. A zero-length pattern is handled before indexing it.
The invariant proves all and only exact matches are reported in order.
Each increase of `j` can pay for a later decrease, yielding
`O(n+m+z+1)` time, `O(m+1)` auxiliary storage and `O(z+1)` output.

### Exact Rabin–Karp

The hash is `sum(code(pattern[j])*31^(m-1-j)) mod 101`, with ASCII codes.
`power=31^(m-1) mod 101`. The first window and pattern are built left to
right. Sliding removes `old*power`, normalizes the remainder into `0..100`,
multiplies by 31 and appends the new ASCII code. Inductively this equals
the polynomial hash of the current window.

Unequal hashes rule out a match. Equal hashes trigger direct character
verification, so collisions never create false matches. All starts including
overlaps are visited. Empty and too-long patterns are handled before window
construction. The `abdj/ab` test must report only `[0]`: `ab` and `dj`
both hash to 75. Random printable ASCII also exercises negative subtraction.

For `1<=m<=n`, time is `O(n+m+c*m)` with `c` equal-hash windows, and
can be quadratic. A uniform bound is `O(n*m+n+m+1)`. This rolling version
uses `O(1)` auxiliary space and `O(z+1)` output. Input is `O(n+m+1)`.
`aaaaa/aaa` expects `[0,1,2]` for all three searches; `abc/""` expects
`[0,1,2,3]`. Independent `startswith` oracles appear only in test code.

## 5. Suffix array and Kasai LCP

The array stores exactly indices `0..n-1`, with no empty suffix or sentinel
output. Initially each rank is its ASCII character. At doubling width `w`,
suffix `i` has pair `(rank[i], rank[i+w])`, using -1 when the second half
is absent. `Arrays.sort(Integer[], comparator)` orders these constant-time
pairs; new ranks assign consecutive classes to equal pairs. Each round
therefore sorts prefixes of length `2w` assuming ranks for length `w`.
After enough doubling rounds these cover the text, so the suffix order is
correct. Unique ranks permit early exit. No full suffix strings are created.

There are `O(log(n+1))` rounds, each using comparison sorting in
`O(n log(n+1))` time. The suffix construction is `O(n log^2(n+1))` time
and `O(n+1)` auxiliary space including boxed indices, rank arrays and
sorting temporary storage.

Kasai builds the inverse permutation. At text start `i`, compare the suffix
with its lexicographic predecessor, beginning at the reused lower bound
`h`. Removing one leading character from the previous common prefix loses
at most one known matching character; the predecessor at the next start
cannot require a smaller lower bound. After recording the LCP, decrease
`h` by at most one. The first ranked suffix has LCP zero and resets `h`.
Across the scan, the total number of increments of `h` is linear.
Thus Kasai is `O(n)` time and `O(n)` auxiliary storage for inverse/LCP.

The maximum adjacent LCP is a longest repeat: suffixes sharing any fixed
prefix form a contiguous lexicographic group, and that group contains an
adjacent pair sharing the prefix. Conversely every positive adjacent LCP
is a repeat at two distinct starts. Reconstruction takes a substring only
for the final answer; strict improvement keeps the first best encountered
in text-index order. Overlaps are allowed. Output is `O(n+1)`, as is input.
Empty text returns three empty outputs. A 50000-character equal string
must give descending SA and LCP `0..49999`; this detects sentinel errors,
missing overlap, and quadratic suffix materialization. Small random tests
compare both arrays against direct suffix sorting and character comparisons.

## 6. Aho–Corasick

Patterns create a trie, with transitions indexed by ASCII code minus 32.
Each dictionary position records its terminal state, so duplicates share
states but retain separate output columns. Transition rows are allocated
only for actual states through an ArrayList. A breadth-first pass constructs
failure links and fills missing transitions from the failure state. Root
missing transitions stay at root. The failure link is the longest proper
suffix which is a trie prefix; breadth-first order ensures its transitions
are ready before processing deeper nodes.

After scanning each character, the state is the longest suffix of the text
prefix represented in the trie. A visit is added there. Reverse breadth-first
order then adds every state's visits to its failure parent. Consequently,
each terminal receives all text ends at which its pattern is a suffix,
including suffix patterns of a longer match. This proves exact overlapping
counts without enumerating every dictionary-entry occurrence. Duplicate
terminal indices naturally receive equal counts. Fresh visits and root
state are created for every independent text and every request.

Let `L` be total pattern length, `S` actual trie states, `p` dictionary size,
and `q` texts. Construction takes `O(L+95*S+p)` time; transitions, failure
links, BFS queue, terminals and one visits array occupy `O(95*S+p)` auxiliary
space. Query time is `O(|text|+S+p)` per text. Input is
`O(L+sum|text|+p+q+1)` and output `O(q*(p+1)+1)`.
No inherited output lists are copied. Empty dictionaries produce one empty
row per text, and empty text arrays produce no rows. Empty texts have zero
counts. `he,she,he` on repeated `sheshe` texts checks suffix counts,
duplicates and reset; 10000 duplicate patterns on dense texts tests
aggregation without per-occurrence work. A separate 100000-total-character
dictionary with 100 empty texts tests state construction and propagation.

## Own tests and results

`tests/student/test_algorithms.py` uses Python Random seed 2537. It checks
small cases with independently written exhaustive oracles: subsequence
enumeration, chronological merge choices, vertex subsets, direct substring
checks and full small suffix sorting. The supplied validator then checks
required fields and reconstructed witnesses against independently obtained
optima. `fixtures.json` contains generated small inputs/expected results;
stress inputs are reproduced by the script rather than stored as huge files.
`results.json` records the actual final count and pass status.

|Command|Fixture/generator|Expected|Actual/check evidence|Defect detected|
|---|---|---|---|---|
|lcs|empty, `ab/ba`, `tea_time/team`, 80 random small cases|optimal subsequence; max equal length 1000|PASS; witness validated|substring logic, tie restrictions, empty indexing|
|merge|enumeration, zero sizes, `1,100,1`, max 200 equal weights|203 for `1,100,1`; 1544000000000 at max|PASS; chronological plan replayed|nonadjacent merging, overflow, wrong output order|
|tree|subset oracle; 100000 chain/star; all negative|50000000000000 chain; 99999000000000 star; zero negative|PASS; weight and independent set checked|recursion failure, forced negative selections, overflow|
|naive/kmp/rk|independent occurrence lists; empty/overlap/ASCII; max lengths|all ordered starts; empty gives n+1|PASS|lost overlaps, bounds, omitted final boundary|
|rk|`abdj/ab`|positions `[0]`|PASS|accepting collisions|
|suffix|direct small suffix/LCP oracle; max all equal|descending SA; LCP 0..49999|PASS; both arrays checked|sentinel inclusion, incorrect Kasai reuse|
|aho|direct per-pattern oracle; duplicate/suffix/reset/max tests|independent count rows, including duplicate columns|PASS; exact rows checked|stale state, lost suffix counts, duplicate explosion|
|mixed|repeated mixed batch|same answers on repeat|PASS|shared mutable request state|

Public verification passes 44 cases and the repeated 88-request mixed batch;
setup ping also passes. Final command output is retained in
`tests/student/public-checks.log` and `tests/student/student-checks.log`.
Passing these tests is evidence for the tested inputs, not a claim about
the unseen official grading results.

## Reproducible search comparison

`tests/student/SearchBenchmark.java` uses Java Random seed `2537+n` for
uniform printable ASCII (95 possible codes). The random pattern is a
length-`n/2` substring at offset `n/4`, ensuring a real match. The repetitive
case uses `a^n` and `a^(n/2)`, so every valid start matches. Three sizes
are in all three algorithms' common domain, including product bounds.

Before timing, every output is compared against an independent startsWith
oracle. Each input receives 30 warm-up calls per method in one JVM. Three
measured rounds each execute 20 complete direct method calls. Values below
are microseconds per call (batch nanoseconds divided by 20000). Compilation,
JVM startup, input generation, correctness verification and JSON transport
are excluded equally; prefix/hash preprocessing and returned list/map
allocation are included. A volatile sink consumes results. Method order
rotates between rounds. Default JIT/GC are enabled; there is no explicit GC
or claim that 30 calls eliminate compilation effects. Local system load and
JIT cause visible noise. The raw CSV and generator are committed for reruns.

|Case|n|m|Seed|Algorithm|Run 1 (us)|Run 2 (us)|Run 3 (us)|Median (us)|Correct|
|---|---:|---:|---:|---|---:|---:|---:|---:|---|
|random|500|250|3037|naive|40.075|40.315|42.155|40.315|true|
|random|500|250|3037|kmp|52.755|47.535|47.720|47.720|true|
|random|500|250|3037|rk|50.605|41.520|43.095|43.095|true|
|random|1500|750|4037|naive|28.740|26.810|27.160|27.160|true|
|random|1500|750|4037|kmp|47.545|42.630|43.555|43.555|true|
|random|1500|750|4037|rk|34.105|34.335|34.470|34.335|true|
|random|3000|1500|5537|naive|53.400|55.775|56.720|55.775|true|
|random|3000|1500|5537|kmp|86.815|93.450|116.070|93.450|true|
|random|3000|1500|5537|rk|68.745|80.070|84.060|80.070|true|
|repetitive|500|250|3037|naive|238.295|97.215|98.515|98.515|true|
|repetitive|500|250|3037|kmp|30.530|30.250|32.530|30.530|true|
|repetitive|500|250|3037|rk|231.935|242.320|237.255|237.255|true|
|repetitive|1500|750|4037|naive|784.510|781.175|796.685|784.510|true|
|repetitive|1500|750|4037|kmp|10.970|11.540|10.800|10.970|true|
|repetitive|1500|750|4037|rk|1292.820|1290.065|1289.025|1290.065|true|
|repetitive|3000|1500|5537|naive|3238.710|3171.275|3133.475|3171.275|true|
|repetitive|3000|1500|5537|kmp|20.715|28.240|21.060|21.060|true|
|repetitive|3000|1500|5537|rk|5048.915|5065.380|4994.565|5048.915|true|

Random mismatches are usually rejected after very few character comparisons,
so naive can be faster at these small sizes despite its worse upper bound.
Dense repetitive matches require long verification at each start for both
naive and exact RK; KMP reuses prefix information and performs much less work.
The n=500 to n=1500 decreases in some measurements illustrate warm-up/JIT
noise, not improved asymptotic complexity. Medians cannot prove bounds.

With modulus 101, unequal windows collide frequently enough to matter; every
equal-hash window still needs verification. In the repetitive case every
window is an actual match, so verification dominates even without accidental
collisions. These results describe this deliberate collision laboratory,
not every production fingerprint design or RK implementation.

## Sources

- Supplied `docs/Assignment_4_EN.md` and `docs/guide/CONTRACTS.md`: required
  algorithms, indexing, bounds, output fields and collision parameters.
- Supplied `docs/guide/TESTING.md`: boundary and scale coverage checklist.
- Supplied `Java/source/Main.java`, `Json.java`, `tools/run.py` and
  `tools/public_validate.py`: unchanged transport, build launcher and
  structural/witness validation support.
- Java standard library: collections, `Arrays.sort`, `StringBuilder`,
  `System.nanoTime` and deterministic `Random`; no algorithm library or
  built-in search replaces the submitted algorithms. Test-only direct
  comparisons serve as independent oracles.

Verification performed in this working session is recorded by the logs and
results above. The student should inspect the implementation and reproduce
the commands before submitting the exact pushed commit URL in Moodle.
