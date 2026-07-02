package com.healthapp.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex, HttpServletResponse response) {
        if (ex.getCode() == 401) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex.getCode() == 403) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        } else if (ex.getCode() == 404) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public Result<Void> handleBadRequest(Exception ex, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return Result.fail(400, "请求参数不正确");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception ex, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Result.fail(500, "服务异常：" + ex.getMessage());
    }
}
