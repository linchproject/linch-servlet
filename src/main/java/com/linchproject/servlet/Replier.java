package com.linchproject.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Georg Schmidl
 */
public interface Replier {

    void reply(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException;
}
