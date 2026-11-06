import java.util.*;
import java.util.function.Function;

/** Direct algorithm calls, excluding JSON, compilation and JVM startup. */
public final class SearchBenchmark {
    static volatile Object sink;
    static final int BATCH = 20;
    static final int WARMUP = 30;

    static List<Integer> oracle(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= text.length() - pattern.length(); i++)
            if (text.startsWith(pattern, i)) positions.add(i);
        return positions;
    }

    public static void main(String[] args) {
        System.out.println("case,n,m,seed,algorithm,run1_us,run2_us,run3_us,median_us,correct");
        String[] names = {"naive", "kmp", "rk"};
        List<Function<Map<String,Object>, Map<String,Object>>> methods = List.of(Algorithms::naive, Algorithms::kmp, Algorithms::rk);
        for (String kind : List.of("random", "repetitive")) {
            for (int n : new int[]{500, 1500, 3000}) {
                int m = n / 2;
                long seed = 2537 + n;
                String text, pattern;
                if (kind.equals("random")) {
                    Random rng = new Random(seed);
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < n; i++) b.append((char) (32 + rng.nextInt(95)));
                    text = b.toString();
                    pattern = text.substring(n / 4, n / 4 + m);
                } else {
                    text = "a".repeat(n);
                    pattern = "a".repeat(m);
                }
                Map<String,Object> input = Map.of("text", text, "pattern", pattern);
                List<Integer> expected = oracle(text, pattern);
                for (Function<Map<String,Object>, Map<String,Object>> method : methods)
                    if (!method.apply(input).get("positions").equals(expected)) throw new AssertionError("Search mismatch");
                for (int i = 0; i < WARMUP; i++)
                    for (Function<Map<String,Object>, Map<String,Object>> method : methods) sink = method.apply(input);
                double[][] times = new double[3][3];
                for (int run = 0; run < 3; run++) {
                    // Rotate order between measured rounds.
                    for (int offset = 0; offset < 3; offset++) {
                        int algorithm = (offset + run) % 3;
                        long begin = System.nanoTime();
                        for (int repeat = 0; repeat < BATCH; repeat++) sink = methods.get(algorithm).apply(input);
                        times[algorithm][run] = (System.nanoTime() - begin) / (1000.0 * BATCH);
                    }
                }
                for (int a = 0; a < 3; a++) {
                    double[] sorted = times[a].clone();
                    Arrays.sort(sorted);
                    System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%.3f,%.3f,%.3f,%.3f,true%n",
                        kind, n, m, seed, names[a], times[a][0], times[a][1], times[a][2], sorted[1]);
                }
            }
        }
    }
}
