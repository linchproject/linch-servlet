package com.linchproject.servlet;

import com.linchproject.core.Route;

import java.util.Map;

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
        String queryString = getQueryString(getParams().getMap());
        return this.contextPath
                + "/" + getController()
                + "/" + getAction()
                + queryString;
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
        return sb.length() > 0? "?" + sb.toString(): "";
    }

    @Override
    protected Route newRoute() {
        return new ServletRoute(this.contextPath);
    }
}
