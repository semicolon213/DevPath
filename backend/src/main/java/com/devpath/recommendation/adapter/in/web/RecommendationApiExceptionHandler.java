package com.devpath.recommendation.adapter.in.web;

import com.devpath.learning.adapter.in.web.LearningRoadmapController;
import com.devpath.recommendation.application.RecommendationNotFoundException;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes={RecommendationController.class,LearningRoadmapController.class})
public class RecommendationApiExceptionHandler {
    @ExceptionHandler(RecommendationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RecommendationNotFoundException exception,HttpServletRequest request){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of("RESOURCE_NOT_FOUND","The recommendation or roadmap was not found.",RequestIds.resolve(request)));}
}
