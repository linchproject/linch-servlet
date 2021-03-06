package com.linchproject.servlet.repliers;

import com.linchproject.core.results.Error;
import com.linchproject.servlet.Environment;
import com.linchproject.servlet.Replier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Georg Schmidl
 */
public class ErrorReplier implements Replier {

    private Error error;

    public ErrorReplier(Error error) {
        this.error = error;
    }

    @Override
    public void reply(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException {
        response.setContentType("text/html;charset=utf-8");

        if (Environment.DEV) {
            String content = "<h1>" + error.getMessage() + "</h1>\n";
            if (error.getException() != null) {
                content += renderException(error.getException());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

            response.getWriter().println(content);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected String renderException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return stackTrace
                .replace(System.getProperty("line.separator"), "<br/>\n")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
    }
}
