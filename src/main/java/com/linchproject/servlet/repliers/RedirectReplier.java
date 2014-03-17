package com.linchproject.servlet.repliers;

import com.linchproject.core.Route;
import com.linchproject.core.results.Redirect;
import com.linchproject.servlet.Replier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Georg Schmidl
 */
public class RedirectReplier implements Replier {

    private Redirect redirect;

    public RedirectReplier(Redirect redirect) {
        this.redirect = redirect;
    }

    @Override
    public void reply(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException {
        Route route = redirect.getRoute();
        response.sendRedirect(route.getUrl());
    }
}
