package com.linchproject.servlet.services;

import com.linchproject.http.CookieService;
import com.linchproject.servlet.ServletThreadLocal;

import javax.servlet.http.Cookie;

/**
 * @author Georg Schmidl
 */
public class ServletCookieService implements CookieService {

    @Override
    public void addCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        ServletThreadLocal.getResponse().addCookie(cookie);
    }

    @Override
    public void removeCookie(String name) {
        addCookie(name, null, 0);
    }

    @Override
    public String getCookieValue(String name) {
        String value = null;

        Cookie[] cookies = ServletThreadLocal.getRequest().getCookies();
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
