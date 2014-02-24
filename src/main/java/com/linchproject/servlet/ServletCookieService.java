package com.linchproject.servlet;

import com.linchproject.http.CookieService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Georg Schmidl
 */
public class ServletCookieService implements CookieService {

    @Override
    public void addCookie(String name, String value, int maxAge) {
        HttpServletResponse response = ServletThreadLocal.getResponse();

        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    @Override
    public void removeCookie(String name) {
        addCookie(name, null, 0);
    }

    @Override
    public String getCookieValue(String name) {
        HttpServletRequest request =  ServletThreadLocal.getRequest();

        String value = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    value = cookie.getValue();
                    break;
                }
            }
        }
        return value;
    }
}
