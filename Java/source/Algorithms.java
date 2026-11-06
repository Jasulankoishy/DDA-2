import java.util.*;

/** Zhasulan Koishybay, 2537-SE. */
public final class Algorithms {
    static Map<String,Object> lcs(Map<String,Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        StringBuilder answer = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                answer.append(a.charAt(--i));
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        return Map.of("length", dp[n][m], "sequence", answer.reverse().toString());
    }

    static Map<String,Object> merge(Map<String,Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<List<Integer>> plan = new ArrayList<>();
        if (n <= 1) return Map.of("cost", 0L, "plan", plan);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int l = 0; l + length <= n; l++) {
                int r = l + length - 1;
                dp[l][r] = Long.MAX_VALUE;
                for (int k = l; k < r; k++) {
                    long cost = dp[l][k] + dp[k + 1][r] + prefix[r + 1] - prefix[l];
                    if (cost < dp[l][r]) {
                        dp[l][r] = cost;
                        split[l][r] = k;
                    }
                }
            }
        }
        mergePlan(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    private static void mergePlan(int l, int r, int[][] split, List<List<Integer>> plan) {
        if (l == r) return;
        int k = split[l][r];
        mergePlan(l, k, split, plan);
        mergePlan(k + 1, r, split, plan);
        plan.add(List.of(l, k, r));
    }

    static Map<String,Object> tree(Map<String,Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);
        List<?> weights = (List<?>) input.get("weights"), edges = (List<?>) input.get("edges");
        int[] degree = new int[n];
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            degree[((Number) edge.get(0)).intValue()]++;
            degree[((Number) edge.get(1)).intValue()]++;
        }
        int[][] adj = new int[n][];
        for (int v = 0; v < n; v++) adj[v] = new int[degree[v]];
        Arrays.fill(degree, 0);
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            int u = ((Number) edge.get(0)).intValue(), v = ((Number) edge.get(1)).intValue();
            adj[u][degree[u]++] = v;
            adj[v][degree[v]++] = u;
        }
        int[] parent = new int[n], order = new int[n];
        Arrays.fill(parent, -1);
        int size = 1;
        order[0] = 0;
        for (int i = 0; i < n; i++) {
            int v = order[i];
            for (int u : adj[v]) if (u != parent[v]) {
                parent[u] = v;
                order[size++] = u;
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int v = order[i];
            take[v] = ((Number) weights.get(v)).longValue();
            for (int u : adj[v]) if (parent[u] == v) {
                take[v] += skip[u];
                skip[v] += Math.max(take[u], skip[u]);
            }
        }
        boolean[] chosen = new boolean[n];
        for (int v : order) {
            chosen[v] = (parent[v] < 0 || !chosen[parent[v]]) && take[v] > skip[v];
            if (chosen[v]) selected.add(v);
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String,Object> naive(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= text.length() - pattern.length(); i++) {
            int j = 0;
            while (j < pattern.length() && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == pattern.length()) positions.add(i);
        }
        return Map.of("positions", positions);
    }

    static Map<String,Object> kmp(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= text.length(); i++) positions.add(i);
            return Map.of("positions", positions);
        }
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int j = pi[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1];
            }
        }
        return Map.of("positions", positions);
    }

    static Map<String,Object> rk(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add(i);
            return Map.of("positions", positions);
        }
        if (m > n) return Map.of("positions", positions);
        long power = 1, target = 0, hash = 0;
        for (int i = 0; i < m; i++) {
            target = (target * 31 + pattern.charAt(i)) % 101;
            hash = (hash * 31 + text.charAt(i)) % 101;
            if (i < m - 1) power = power * 31 % 101;
        }
        for (int start = 0; start <= n - m; start++) {
            if (hash == target) {
                int j = 0;
                while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add(start);
            }
            if (start < n - m) {
                hash = (hash - text.charAt(start) * power) % 101;
                if (hash < 0) hash += 101;
                hash = (hash * 31 + text.charAt(start + m)) % 101;
            }
        }
        return Map.of("positions", positions);
    }

    static Map<String,Object> suffix(Map<String,Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        if (n == 0) return Map.of("sa", new int[0], "lcp", new int[0], "repeated", "");
        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }
        for (int width = 1; width < n; width *= 2) {
            final int gap = width;
            final int[] old = rank;
            Arrays.sort(sa, (a, b) -> {
                int first = Integer.compare(old[a], old[b]);
                if (first != 0) return first;
                return Integer.compare(a + gap < n ? old[a + gap] : -1, b + gap < n ? old[b + gap] : -1);
            });
            int[] next = new int[n];
            for (int i = 1; i < n; i++) {
                int a = sa[i - 1], b = sa[i];
                boolean different = old[a] != old[b]
                    || (a + gap < n ? old[a + gap] : -1) != (b + gap < n ? old[b + gap] : -1);
                next[b] = next[a] + (different ? 1 : 0);
            }
            rank = next;
            if (rank[sa[n - 1]] == n - 1) break;
        }
        int[] inverse = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) inverse[sa[i]] = i;
        int h = 0, bestLength = 0, bestStart = 0;
        for (int i = 0; i < n; i++) {
            int position = inverse[i];
            if (position == 0) {
                h = 0;
                continue;
            }
            int j = sa[position - 1];
            while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) h++;
            lcp[position] = h;
            if (h > bestLength) {
                bestLength = h;
                bestStart = i;
            }
            if (h > 0) h--;
        }
        return Map.of("sa", sa, "lcp", lcp, "repeated", text.substring(bestStart, bestStart + bestLength));
    }

    static Map<String,Object> aho(Map<String,Object> input) {
        List<?> patterns = (List<?>) input.get("patterns"), texts = (List<?>) input.get("texts");
        List<int[]> nodes = new ArrayList<>();
        nodes.add(new int[95]);
        int[] terminal = new int[patterns.size()];
        for (int i = 0; i < patterns.size(); i++) {
            String pattern = (String) patterns.get(i);
            int v = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int c = pattern.charAt(j) - 32;
                if (nodes.get(v)[c] == 0) {
                    nodes.get(v)[c] = nodes.size();
                    nodes.add(new int[95]);
                }
                v = nodes.get(v)[c];
            }
            terminal[i] = v;
        }
        int states = nodes.size();
        int[][] go = nodes.toArray(new int[states][]);
        int[] fail = new int[states], queue = new int[states];
        int head = 0, tail = 0;
        for (int c = 0; c < 95; c++) if (go[0][c] != 0) queue[tail++] = go[0][c];
        while (head < tail) {
            int v = queue[head++];
            for (int c = 0; c < 95; c++) {
                int u = go[v][c];
                if (u == 0) go[v][c] = go[fail[v]][c];
                else {
                    fail[u] = go[fail[v]][c];
                    queue[tail++] = u;
                }
            }
        }
        List<long[]> rows = new ArrayList<>();
        for (Object item : texts) {
            String text = (String) item;
            long[] visits = new long[states];
            int v = 0;
            for (int i = 0; i < text.length(); i++) {
                v = go[v][text.charAt(i) - 32];
                visits[v]++;
            }
            for (int i = tail - 1; i >= 0; i--) {
                int u = queue[i];
                visits[fail[u]] += visits[u];
            }
            long[] counts = new long[patterns.size()];
            for (int i = 0; i < counts.length; i++) counts[i] = visits[terminal[i]];
            rows.add(counts);
        }
        return Map.of("counts", rows);
    }
}
