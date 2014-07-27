package com.linchproject.servlet;

import com.linchproject.core.Route;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Georg Schmidl
 */
public class ServletRoute extends Route {

    private HttpServletRequest request;

    public ServletRoute(HttpServletRequest request) {
        this.request = request;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!request.getParameterMap().isEmpty()) {
            path += "?" + getQueryString(request.getParameterMap());
        }
        setPath(path);
    }

    public ServletRoute(HttpServletRequest request, String controllerPackage) {
        this(request);
        setControllerPackage(controllerPackage);
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
    protected Route newRoute() {
        return new ServletRoute(this.request);
    }
}
