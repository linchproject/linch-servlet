package com.linchproject.servlet;

import com.linchproject.core.Route;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Georg Schmidl
 */
public class ServletRoute extends Route {

    private static String USER_ID_KEY = ServletRoute.class.getSimpleName() + "-user-id";

    public HttpServletRequest request;

    public ServletRoute(HttpServletRequest request) {
        this.request = request;

        String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
        if (!request.getParameterMap().isEmpty()) {
            path += "?" + getQueryString(request.getParameterMap());
        }
        setPath(path);
    }

    private String getQueryString(Map<String, String[]> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(key);
                sb.append("=");
                sb.append(value);
            }
        }
        return sb.toString();
    }

    @Override
    public String getUrl() {
        return this.request.getContextPath() + getPath();

    }

    @Override
    public String getUserId() {
        return (String) request.getSession().getAttribute(USER_ID_KEY);
    }

    @Override
    public void setUserId(String userId) {
        request.getSession().setAttribute(USER_ID_KEY, userId);
    }

    @Override
    protected Route newRoute() {
        return new ServletRoute(request);
    }
}
