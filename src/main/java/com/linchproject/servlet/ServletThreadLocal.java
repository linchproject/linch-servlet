package com.linchproject.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Georg Schmidl
 */
public class ServletThreadLocal {

    private static final ThreadLocal<HttpServletRequest> request = new ThreadLocal<HttpServletRequest>();
    private static final ThreadLocal<HttpServletResponse> response = new ThreadLocal<HttpServletResponse>();

    public static HttpServletRequest getRequest() {
        return request.get();
    }

    public static HttpServletResponse getResponse() {
        return response.get();
    }

    public static void setRequest(HttpServletRequest httpServletRequest) {
        request.set(httpServletRequest);
    }

    public static void setResponse(HttpServletResponse httpServletResponse) {
        response.set(httpServletResponse);
    }

    public static void removeRequest() {
        request.remove();
    }

    public static void removeResponse() {
        response.remove();
    }

}
