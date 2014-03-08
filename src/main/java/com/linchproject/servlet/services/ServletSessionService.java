package com.linchproject.servlet.services;

import com.linchproject.http.SessionService;
import com.linchproject.servlet.ServletThreadLocal;

/**
 * @author Georg Schmidl
 */
public class ServletSessionService implements SessionService {

    @Override
    public String getValue(String key) {
        return (String) ServletThreadLocal.getRequest().getSession().getAttribute(key);
    }

    @Override
    public void setValue(String key, String value) {
        ServletThreadLocal.getRequest().getSession().setAttribute(key, value);
    }
}
