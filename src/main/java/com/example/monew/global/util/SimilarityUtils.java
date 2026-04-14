package com.example.monew.global.util;

public final class SimilarityUtils {

  public static final double SIMILAR_THRESHOLD = 0.80;

  private SimilarityUtils() {
  }

  public static double similarity(String a, String b) {
    if (a == null || b == null) {
      return 0.0;
    }
    if (a.isEmpty() && b.isEmpty()) {
      return 1.0;
    }
    int distance = levenshtein(a, b);
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 1.0;
    }
    return 1.0 - ((double) distance / maxLen);
  }

  public static boolean isSimilar(String a, String b) {
    return similarity(a, b) >= SIMILAR_THRESHOLD;
  }

  private static int levenshtein(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(
            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
            dp[i - 1][j - 1] + cost
        );
      }
    }
    return dp[a.length()][b.length()];
  }
}
