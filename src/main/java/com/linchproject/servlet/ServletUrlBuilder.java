package com.linchproject.servlet;

import com.linchproject.core.Route;
import com.linchproject.core.UrlBuilder;

import java.util.Map;

/**
 * @author Georg Schmidl
 */
public class ServletUrlBuilder implements UrlBuilder {

    private String contextPath;

    public ServletUrlBuilder(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String buildUrl(Route route) {
        String queryString = getQueryString(route.getParams().getMap());
        return this.contextPath
                + "/" + route.getController()
                + "/" + route.getAction()
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
}
