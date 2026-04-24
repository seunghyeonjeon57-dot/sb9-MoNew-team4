package com.example.monew.domain.activityManagement.controller;


import com.example.monew.domain.activityManagement.dto.UserActivityDto;
import com.example.monew.domain.activityManagement.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name="사용자 활동 내역 관리",description = "사용자 활동 내역 관련 API")
@RestController
@RequestMapping("/api/user-activities")
@RequiredArgsConstructor
public class ActivityController {

  private final ActivityService activityService;

  @Operation(summary = "사용자 활동 내역 조회", description = "특정 사용자의 기본 정보와 최근 활동 내역을 묶어서 반환합니다.")
  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityDto> getUserActivity(
      @PathVariable("userId") UUID userId
  ) {
    UserActivityDto response = activityService.getUserActivity(userId);
    return ResponseEntity.ok(response);
  }

}
