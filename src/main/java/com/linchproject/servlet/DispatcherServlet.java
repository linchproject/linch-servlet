package com.linchproject.servlet;

import com.linchproject.core.*;
import com.linchproject.core.results.Error;
import com.linchproject.core.results.Redirect;
import com.linchproject.core.results.Success;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Georg Schmidl
 */
public class DispatcherServlet extends HttpServlet {

    private Invoker invoker;

    @Override
    public void init(ServletConfig config) throws ServletException {
        String controllerPackage = config.getInitParameter("controllerPackage");
        String rendererClassName = config.getInitParameter("rendererClassName");

        Class<?> rendererClass;
        try {
            rendererClass = getClass().getClassLoader().loadClass(rendererClassName);
        } catch (ClassNotFoundException e) {
            throw new ServletException("cannot find renderer " + rendererClassName, e);
        }

        Container container = Container.getInstance();
        container.add("renderer", rendererClass);

        this.invoker = new Invoker(getClass().getClassLoader(), controllerPackage);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    protected void dispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Route route = getRoute(req);

        Result result = invoker.invoke(route);

        apply(result, req, resp);
    }

    protected Route getRoute(HttpServletRequest req) {
        Route route = new ServletRoute(req.getContextPath());

        String uri = req.getRequestURI().substring(req.getContextPath().length());
        String[] uriSplit = uri.split("/");
        if (uriSplit.length > 0 && uriSplit[0].length() > 0) {
            route.setController(uriSplit[0]);
        }
        if (uriSplit.length > 1 && uriSplit[1].length() > 0) {
            route.setAction(uriSplit[1]);
        }

        route.setParams(new Params(req.getParameterMap()));
        return route;
    }

    protected void apply(Result result, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (result instanceof Success) {
            Success success = (Success) result;

            resp.setContentType("text/html;charset=utf-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(success.getContent());

        } else if (result instanceof Redirect) {
            Redirect redirect = (Redirect) result;
            Route route = redirect.getRoute();

            resp.sendRedirect(route.getUrl());
        } else if (result instanceof Error) {
            Error error = (Error) result;

            resp.setContentType("text/html;charset=utf-8");

            String content = "<h1>" + error.getMessage() + "</h1>\n";
            if (error.getException() != null) {
                content += renderException(error.getException());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

            resp.getWriter().println(content);
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
