package com.linchproject.servlet;

import com.linchproject.core.Container;
import com.linchproject.core.Invoker;
import com.linchproject.core.Result;
import com.linchproject.core.Route;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

        ReplierFactory.getReplier(result).reply(response);
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
}
