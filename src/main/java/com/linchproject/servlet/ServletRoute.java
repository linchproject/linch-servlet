package com.linchproject.servlet;

import com.linchproject.core.Route;

/**
 * @author Georg Schmidl
 */
public class ServletRoute extends Route {

    public String contextPath;

    public ServletRoute(String contextPath) {
        this.contextPath = contextPath;
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
