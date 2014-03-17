package com.linchproject.servlet.repliers;

import com.linchproject.core.results.Success;
import com.linchproject.servlet.Replier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Georg Schmidl
 */
public class SuccessReplier implements Replier {

    private Success success;

    public SuccessReplier(Success success) {
        this.success = success;
    }

    @Override
    public void reply(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(success.getContent());
    }
}
