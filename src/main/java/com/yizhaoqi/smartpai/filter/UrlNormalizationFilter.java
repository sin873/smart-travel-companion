package com.yizhaoqi.smartpai.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * URL 规范化过滤器
 * 处理小程序端请求 URL 中可能出现的双斜杠问题
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UrlNormalizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            // 获取原始请求 URI
            String requestURI = httpRequest.getRequestURI();
            
            // 如果包含双斜杠，进行替换（保留开头的//）
            if (requestURI.contains("//")) {
                // 将多个连续的/替换为单个/
                String normalizedURI = requestURI.replaceAll("/+", "/");
                
                // 包装请求，返回规范化后的 URI
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getRequestURI() {
                        return normalizedURI;
                    }
                    
                    @Override
                    public StringBuffer getRequestURL() {
                        StringBuffer url = super.getRequestURL();
                        return new StringBuffer(url.substring(0, url.length() - requestURI.length()) + normalizedURI);
                    }
                };
                
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
