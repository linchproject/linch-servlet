package com.linchproject.servlet;

import com.linchproject.core.Route;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Georg Schmidl
 */
public class ServletRoute extends Route {

    public String contextPath;

    public ServletRoute(String contextPath) {
        this.contextPath = contextPath;
    }

    public ServletRoute(HttpServletRequest request) {
        this(request.getContextPath());

        String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        setPath(path);
    }

    @Override
    public String getUrl() {
        return this.contextPath + getPath();

    }

    @Override
    protected Route newRoute() {
        return new ServletRoute(this.contextPath);
    }
}
