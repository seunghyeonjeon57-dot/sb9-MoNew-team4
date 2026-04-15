package com.example.monew.global.util;

public final class SimilarityUtils {

  private SimilarityUtils() {}

  public static double similarity(String a, String b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("문자열은 null일 수 없습니다.");
    }
    if (a.equals(b)) {
      return 1.0;
    }
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 1.0;
    }
    int distance = levenshtein(a, b);
    return 1.0 - ((double) distance / maxLen);
  }

  private static int levenshtein(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
    for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
        dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
      }
    }
    return dp[a.length()][b.length()];
  }
}
