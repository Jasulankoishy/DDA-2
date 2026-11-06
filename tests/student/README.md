# Student tests — Zhasulan Koishybay, 2537-SE

From the repository root, with Python 3.12+ and JDK 17+ on PATH:

```sh
python tools/run.py --setup
python tools/run.py --check
python tests/student/test_algorithms.py
javac --release 17 -encoding UTF-8 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark > tests/student/benchmark.csv
```

`test_algorithms.py` compiles the selected Java sources, generates deterministic
tests with seed 2537, validates reconstructed witnesses and writes `results.json`.
Small cases are compared with independent exhaustive enumeration or direct
search/suffix sorting. The generated `fixtures.json` contains small requests
and expected results; large cases are generated in memory. The script checks
maximum lengths, 100000-vertex chains/stars, 64-bit sums, 50000-character suffix
arrays and dense/large dictionaries. Each Java batch has a 20-second limit.

`SearchBenchmark.java` validates outputs against `String.startsWith` before
timing any method. This built-in is a test oracle only. Each input receives
30 warm-up calls per algorithm and three measured batches of 20 calls each.
Numbers are microseconds per call, include algorithm preprocessing and result
allocation, and exclude JSON transport, compilation and JVM startup. A volatile
sink consumes the results. Round order rotates to reduce order bias. Default
JIT and GC remain enabled, so these are local measurements, not a JMH study.

The benchmark compares random printable ASCII with a guaranteed substring match
and dense repetitive matches at n=500,1500,3000, m=n/2. Java Random seed is
2537+n for each random case. `benchmark.csv` records all three runs and medians.

`public-checks.log` and `student-checks.log` record the final verification.
