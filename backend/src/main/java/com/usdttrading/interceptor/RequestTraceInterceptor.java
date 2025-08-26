package com.usdttrading.interceptor;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 請求追蹤攔截器
 * 為每個請求生成唯一的追蹤ID
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Component
public class RequestTraceInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 從請求頭獲取追蹤ID，如果沒有則生成新的
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = IdUtil.fastSimpleUUID();
        }

        // 設置到MDC中，供日誌使用
        MDC.put(TRACE_ID_KEY, traceId);

        // 將追蹤ID添加到響應頭
        response.setHeader(TRACE_ID_HEADER, traceId);

        // 將追蹤ID存儲到請求屬性中，供後續使用
        request.setAttribute(TRACE_ID_KEY, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除MDC中的追蹤ID，避免內存洩露
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 獲取當前請求的追蹤ID
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
}