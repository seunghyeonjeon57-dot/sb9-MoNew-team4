package com.example.monew.domain.interest.entity;

import com.example.monew.domain.interest.exception.InvalidInterestArgumentException;
import com.example.monew.global.base.BaseEntity;
import com.example.monew.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(name = "name", nullable = false, unique = true, length = 50)
  private String name;

  @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InterestKeyword> keywords = new ArrayList<>();

  @Column(name = "subscriber_count", nullable = false)
  private long subscriberCount = 0L;

  @Builder
  public Interest(String name, List<String> keywords) {
    if (!StringUtils.hasText(name)) {
      throw new InvalidInterestArgumentException(
          ErrorCode.INTEREST_NAME_BLANK,
          Map.of("name", String.valueOf(name)));
    }
    if (keywords == null || keywords.isEmpty()) {
      throw new InvalidInterestArgumentException(
          ErrorCode.INTEREST_KEYWORDS_EMPTY,
          Map.of("keywordsSize", keywords == null ? 0 : keywords.size()));
    }
    this.name = name;
    replaceKeywords(keywords);
  }

  public void replaceKeywords(List<String> newKeywords) {
    this.keywords.clear();
    for (String value : newKeywords) {
      this.keywords.add(new InterestKeyword(this, value));
    }
  }

}
