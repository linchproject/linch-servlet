package com.linchproject.servlet;

import com.linchproject.http.SessionService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Georg Schmidl
 */
public class ServletSessionService implements SessionService {

    private static String USER_ID_KEY = ServletSessionService.class.getName() + "-userId";

    @Override
    public String getUserId() {
        HttpServletRequest request =  ServletThreadLocal.getRequest();
        return (String) request.getSession().getAttribute(USER_ID_KEY);
    }

    @Override
    public void setUserId(String userId) {
        HttpServletRequest request =  ServletThreadLocal.getRequest();
        request.getSession().setAttribute(USER_ID_KEY, userId);
    }
}
