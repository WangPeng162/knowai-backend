package com.knowai.knowaibackend.exception;

import com.knowai.knowaibackend.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> business(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> validException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, msg);
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> exception(Exception e) {
        // 先识别大模型调用错误（额度不足 / API Key 等），给出友好提示
        String aiError = extractAiError(e);
        if (aiError != null) {
            log.warn("大模型调用失败: {}", aiError);
            return Result.fail(aiError);
        }
        log.error("系统异常", e);
        return Result.fail(500, "服务器内部错误");
    }

    /**
     * 识别大模型调用错误，返回友好提示；非大模型错误返回 null。
     * 遍历异常链（cause），因为额度不足的原始异常可能被
     * "文档向量化失败" / "文档解析失败" 等 RuntimeException 包装多层。
     */
    private String extractAiError(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                // 额度不足 / 账户欠费
                if (lower.contains("arrearage") || lower.contains("insufficient")
                        || lower.contains("quota") || lower.contains("out_of_service")
                        || lower.contains("100011")) {
                    return "大模型额度不足或账户欠费，请前往阿里云百炼（DashScope）检查余额并充值";
                }
                // API Key 无效或已过期
                if (lower.contains("invalidapikey") || lower.contains("api key")) {
                    return "大模型 API Key 无效或已过期，请检查 DASHSCOPE_API_KEY 配置";
                }
            }
            t = t.getCause();
        }
        return null;
    }
}
