package com.fmi.global.apiPayload.exception;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.ErrorReasonDTO;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            String name = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.put(name, v.getMessage());
        });

        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, ErrorStatus._BAD_REQUEST, request, errors);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage =
                    Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(
                    fieldName,
                    errorMessage,
                    (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
        });

        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, ErrorStatus._BAD_REQUEST, request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        Throwable cause = e.getCause();
        if (cause instanceof GeneralException generalException) {
            ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
            return handleExceptionInternal(e, errorReasonHttpStatus, null, request);
        }
        ErrorReasonDTO errorReason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();
        return handleExceptionInternal(e, errorReason, null, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        log.error("Unhandled exception", e);
        // 내부 상세 메시지는 노출하지 않음
        return handleExceptionInternalFalse(
                e,
                ErrorStatus._INTERNAL_SERVER_ERROR,
                HttpHeaders.EMPTY,
                ErrorStatus._INTERNAL_SERVER_ERROR.getHttpStatus(),
                request,
                null);
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> onThrowException(GeneralException generalException, HttpServletRequest request) {
        ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
        return handleExceptionInternal(generalException, errorReasonHttpStatus, null, request);
    }

    @ExceptionHandler(value = UsernameNotFoundException.class)
    public ResponseEntity<Object> handleUsernameNotFoundException(
            UsernameNotFoundException e, HttpServletRequest request) {
        // GeneralException이 원인인 경우 해당 에러 정보 사용
        if (e.getCause() instanceof com.fmi.global.apiPayload.exception.GeneralException) {
            com.fmi.global.apiPayload.exception.GeneralException generalException =
                    (com.fmi.global.apiPayload.exception.GeneralException) e.getCause();
            ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
            return handleExceptionInternal(e, errorReasonHttpStatus, null, request);
        }
        // 일반 UsernameNotFoundException인 경우 _USER_NOT_FOUND 사용
        ErrorReasonDTO errorReason = ErrorStatus._USER_NOT_FOUND.getReasonHttpStatus();
        return handleExceptionInternal(e, errorReason, null, request);
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("데이터베이스 제약 조건 위반 발생", e);
        // 상세 에러 메시지는 로그에만 기록하고 클라이언트에는 일반 메시지만 전달
        ErrorReasonDTO errorReason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();
        return handleExceptionInternal(e, errorReason, null, request);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception e, ErrorReasonDTO reason, HttpHeaders headers, HttpServletRequest request) {

        ApiResponse<Object> body = ApiResponse.onFailure(reason.getCode(), reason.getMessage(), null);
        //        e.printStackTrace();

        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), webRequest);
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(
            Exception e,
            ErrorStatus errorCommonStatus,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request,
            String errorPoint) {
        ApiResponse<Object> body =
                ApiResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorPoint);
        return super.handleExceptionInternal(e, body, headers, status, request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(
            Exception e,
            HttpHeaders headers,
            ErrorStatus errorCommonStatus,
            WebRequest request,
            Map<String, String> errorArgs) {
        ApiResponse<Object> body =
                ApiResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorArgs);
        return super.handleExceptionInternal(e, body, headers, errorCommonStatus.getHttpStatus(), request);
    }
}
