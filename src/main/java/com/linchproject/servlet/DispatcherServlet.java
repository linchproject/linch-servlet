package com.linchproject.servlet;

import com.linchproject.apps.AppConfig;
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
import java.util.Map;

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
        AppConfig appConfig = null;
        try {
            appConfig = AppConfig.load(classLoader, APP_PROPERTIES);
        } catch (IOException e) {
            throw new ServletException("missing " + APP_PROPERTIES, e);
        }

        String appPackage = appConfig.get("package");

        Container container = new Container();
        container.add("appConfig", appConfig);
        for (Map.Entry<String, String> entry: appConfig.getMap("component.").entrySet()) {
            Class<?> componentClass;
            try {
                componentClass = classLoader.loadClass(entry.getValue());
            } catch (ClassNotFoundException e) {
                throw new ServletException("class not found for component " + entry.getKey(), e);
            }
            container.add(entry.getKey(), componentClass);
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
        Route route = new ServletRoute(request);
        Result result = invoker.invoke(route);
        ReplierFactory.getReplier(result).reply(response);
    }
}
