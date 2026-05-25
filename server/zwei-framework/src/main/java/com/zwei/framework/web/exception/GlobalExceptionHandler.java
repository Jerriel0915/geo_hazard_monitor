package com.zwei.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.zwei.common.constant.HttpStatus;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.text.Convert;
import com.zwei.common.exception.DemoModeException;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.html.EscapeUtil;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * @author zwei
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return AjaxResult.error(HttpStatus.BAD_METHOD, e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(HttpStatus.BAD_REQUEST,
            String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        String requiredType = e.getRequiredType() == null ? "未知类型" : e.getRequiredType().getName();
        return AjaxResult.error(HttpStatus.BAD_REQUEST,
            String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), requiredType, value));
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public AjaxResult handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                    HttpServletRequest request)
    {
        log.error("请求地址'{}'缺少必填参数.", request.getRequestURI(), e);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, String.format("缺少必填参数[%s]", e.getParameterName()));
    }

    /**
     * 请求体解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                            HttpServletRequest request)
    {
        log.error("请求地址'{}'的请求体解析失败.", request.getRequestURI(), e);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, "请求体格式错误或JSON解析失败");
    }

    /**
     * 请求内容类型不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public AjaxResult handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e,
                                                               HttpServletRequest request)
    {
        log.error("请求地址'{}'的Content-Type不受支持.", request.getRequestURI(), e);
        return AjaxResult.error(HttpStatus.UNSUPPORTED_TYPE, "不支持的请求内容类型");
    }

    /**
     * 方法参数校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request)
    {
        log.error("请求地址'{}'的方法参数校验失败.", request.getRequestURI(), e);
        String message = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("; "));
        return AjaxResult.error(HttpStatus.BAD_REQUEST, StringUtils.isNotEmpty(message) ? message : "请求参数校验失败");
    }

    /**
     * Spring 6 方法级参数校验失败
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public AjaxResult handleHandlerMethodValidationException(HandlerMethodValidationException e,
                                                             HttpServletRequest request)
    {
        log.error("请求地址'{}'的方法级参数校验失败.", request.getRequestURI(), e);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, "请求参数校验失败");
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        if (isSseRequest(request))
        {
            log.debug("请求地址'{}'的SSE响应异常已忽略: {}", requestURI, e.getMessage());
            return null;
        }
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * SSE/异步请求在客户端主动断开时忽略输出异常
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e, HttpServletRequest request)
    {
        log.debug("请求地址'{}'的异步响应已断开，忽略后续输出。", request.getRequestURI());
    }

    /**
     * SSE/异步请求超时后不再写回AjaxResult，避免和event-stream响应类型冲突
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e, HttpServletRequest request)
    {
        log.debug("请求地址'{}'的异步响应已超时，忽略后续输出。", request.getRequestURI());
    }

    /**
     * 资源或接口不存在
     */
    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public AjaxResult handleNotFoundException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}'不存在: {}", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.NOT_FOUND, "请求资源不存在");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        if (isSseRequest(request))
        {
            log.debug("请求地址'{}'的SSE系统异常已忽略: {}", requestURI, e.getMessage());
            return null;
        }
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getAllErrors().isEmpty()
            ? "请求参数校验失败"
            : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError() == null
            ? "请求参数校验失败"
            : e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return AjaxResult.error("演示模式，不允许操作");
    }

    private boolean isSseRequest(HttpServletRequest request)
    {
        if (request == null)
        {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (StringUtils.isNotEmpty(accept) && accept.contains("text/event-stream"))
        {
            return true;
        }
        String requestURI = request.getRequestURI();
        return StringUtils.isNotEmpty(requestURI) && requestURI.startsWith("/api/v1/logs/stream");
    }
}
