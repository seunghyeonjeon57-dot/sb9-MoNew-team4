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
    // 짧은 쪽을 내부 루프 대상으로 두어 배열 길이를 min(m,n)+1 로 최소화
    if (a.length() < b.length()) {
      String tmp = a;
      a = b;
      b = tmp;
    }
    int n = a.length();
    int m = b.length();

    int[] prev = new int[m + 1];
    int[] curr = new int[m + 1];
    for (int j = 0; j <= m; j++) {
      prev[j] = j;
    }

    for (int i = 1; i <= n; i++) {
      curr[0] = i;
      for (int j = 1; j <= m; j++) {
        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
        curr[j] = Math.min(
            Math.min(curr[j - 1] + 1, prev[j] + 1),
            prev[j - 1] + cost
        );
      }
      int[] tmp = prev;
      prev = curr;
      curr = tmp;
    }
    return prev[m];
  }
}
