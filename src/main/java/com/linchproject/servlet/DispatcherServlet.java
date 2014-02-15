package com.linchproject.servlet;

import com.linchproject.core.Container;
import com.linchproject.core.Invoker;
import com.linchproject.core.Result;
import com.linchproject.core.Route;
import com.linchproject.core.results.Binary;
import com.linchproject.core.results.Error;
import com.linchproject.core.results.Redirect;
import com.linchproject.core.results.Success;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author Georg Schmidl
 */
public class DispatcherServlet extends HttpServlet {

    private static final String APP_PROPERTIES = "app.properties";
    private static final String CONTROLLERS_PACKAGE = "controllers";

    private Invoker invoker;

    @Override
    public void init(ServletConfig config) throws ServletException {
        ClassLoader classLoader = getClass().getClassLoader();

        Properties appConfig = new Properties();
        try {
            appConfig.load(classLoader.getResourceAsStream(APP_PROPERTIES));
        } catch (IOException e) {
            throw new ServletException(APP_PROPERTIES + " missing", e);
        }

        String appPackage = appConfig.getProperty("package");


        Container container = new Container();

        Enumeration<?> enumeration = appConfig.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();

            if (key.startsWith("component.")) {
                String componentKey = key.substring(key.indexOf(".") + 1, key.length());
                Class<?> componentClass;
                try {
                    componentClass = classLoader.loadClass(appConfig.getProperty(key));
                } catch (ClassNotFoundException e) {
                    throw new ServletException("class not found for component " + componentKey, e);
                }
                container.add(componentKey, componentClass);
            }
        }

        String controllersPackage = appPackage != null? appPackage + "." + CONTROLLERS_PACKAGE : CONTROLLERS_PACKAGE;
        this.invoker = new Invoker(classLoader, controllersPackage, container);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        dispatch(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        dispatch(request, response);
    }

    protected void dispatch(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Route route = getRoute(request);

        Result result = invoker.invoke(route);

        apply(result, request, response);
    }

    protected Route getRoute(HttpServletRequest request) {
        Route route = new ServletRoute(request.getContextPath());

        String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        route.setPath(path);

        return route;
    }

    protected void apply(Result result, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (result instanceof Success) {
            Success success = (Success) result;

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(success.getContent());

        } else if (result instanceof Binary) {
            InputStream inputStream = ((Binary) result).getInputStream();
            OutputStream outputStream = response.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

        } else if (result instanceof Redirect) {
            Redirect redirect = (Redirect) result;
            Route route = redirect.getRoute();

            response.sendRedirect(route.getUrl());
        } else if (result instanceof Error) {
            Error error = (Error) result;

            response.setContentType("text/html;charset=utf-8");

            String content = "<h1>" + error.getMessage() + "</h1>\n";
            if (error.getException() != null) {
                content += renderException(error.getException());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

            response.getWriter().println(content);
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
