"""Insert the actually measured benchmark CSV into the English analysis."""
import csv
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
rows = list(csv.DictReader((root / 'tests/student/benchmark.csv').open(encoding='utf-8-sig')))
table = ['|Case|n|m|Seed|Algorithm|Run 1 (us)|Run 2 (us)|Run 3 (us)|Median (us)|Correct|',
         '|---|---:|---:|---:|---|---:|---:|---:|---:|---|']
for row in rows:
    table.append('|' + '|'.join(row[k] for k in ['case', 'n', 'm', 'seed', 'algorithm', 'run1_us', 'run2_us', 'run3_us', 'median_us', 'correct']) + '|')
path = root / 'ANALYSIS.md'
text = path.read_text(encoding='utf-8')
assert '<!-- BENCHMARK_TABLE -->' in text
path.write_text(text.replace('<!-- BENCHMARK_TABLE -->', '\n'.join(table)), encoding='utf-8')
