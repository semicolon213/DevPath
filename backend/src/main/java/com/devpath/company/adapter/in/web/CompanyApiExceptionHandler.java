package com.devpath.company.adapter.in.web;
import com.devpath.company.application.CompanyNotFoundException;
import com.devpath.shared.api.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(assignableTypes=CompanyCatalogController.class) public class CompanyApiExceptionHandler {
 @ExceptionHandler(CompanyNotFoundException.class) ResponseEntity<ApiErrorResponse> missing(RuntimeException e,HttpServletRequest r){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of("RESOURCE_NOT_FOUND","The company profile was not found.",RequestIds.resolve(r)));}
 @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<ApiErrorResponse> invalid(RuntimeException e,HttpServletRequest r){return ResponseEntity.badRequest().body(ApiErrorResponse.of("VALIDATION_ERROR","The company identifier is invalid.",RequestIds.resolve(r)));}
}
