package com.linchproject.servlet;

import com.linchproject.apps.App;
import com.linchproject.apps.AppRegistry;
import com.linchproject.core.Injector;
import com.linchproject.core.Invoker;
import com.linchproject.core.Result;
import com.linchproject.core.Route;
import com.linchproject.ioc.Container;

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

    public enum Environment {
        DEV, PROD
    }

    private static final String APP_PROPERTIES = "app.properties";
    private static final String CONTROLLER_SUB_PACKAGE = "controllers";

    private static Environment environment = Environment.PROD;

    static {
        String environmentProperty = System.getProperty("com.linchproject.environment");
        if (environmentProperty != null && "development".equals(environmentProperty)) {
            environment = Environment.DEV;
        }
    }

    private ClassLoader classLoader;
    private AppRegistry appRegistry;

    private App mainApp;

    private Container container;
    private Invoker invoker;

    @Override
    public void init() throws ServletException {
        classLoader = getClass().getClassLoader();

        appRegistry = new AppRegistry();
        appRegistry.loadFromClassPath();

        try {
            mainApp = App.load(classLoader, APP_PROPERTIES);
        } catch (IOException e) {
            throw new ServletException("missing " + APP_PROPERTIES, e);
        }

        if (Environment.PROD.equals(environment)) {
            container = createContainer(classLoader);
            invoker = createInvoker(classLoader, container);
        }
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
        ServletThreadLocal.setRequest(request);
        ServletThreadLocal.setResponse(response);

        String appPackage = mainApp.get("package");
        String controllersPackage = appPackage != null? appPackage + "." + CONTROLLER_SUB_PACKAGE : CONTROLLER_SUB_PACKAGE;

        Route route = new ServletRoute(request);
        route.setControllerPackage(controllersPackage);

        Container container;
        Invoker invoker;

        if (Environment.DEV.equals(environment)) {
            DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(classLoader, appPackage);
            container = createContainer(dynamicClassLoader);
            invoker = createInvoker(dynamicClassLoader, container);
        } else {
            container = this.container;
            invoker = this.invoker;
        }

        Result result = invoker.invoke(route);
        ReplierFactory.getReplier(result).reply(response);

        if (Environment.DEV.equals(environment)) {
            container.clear();
        }

        ServletThreadLocal.removeRequest();
        ServletThreadLocal.removeResponse();
    }

    private Container createContainer(ClassLoader classLoader) throws ServletException {
        Container container = new Container();

        container.add("app", mainApp);
        container.add("sessionService", ServletSessionService.class);
        container.add("cookieService", ServletCookieService.class);

        for (App app : appRegistry.getApps()) {
            for (Map.Entry<String, String> entry : app.getMap("component.").entrySet()) {
                Class<?> componentClass;
                try {
                    componentClass = classLoader.loadClass(entry.getValue());
                } catch (ClassNotFoundException e) {
                    throw new ServletException("class not found for component " + entry.getKey(), e);
                }
                container.add(entry.getKey(), componentClass);
            }
        }
        return container;
    }

    private Invoker createInvoker(ClassLoader classLoader, final Container container) throws ServletException {
        return new Invoker(classLoader, new Injector() {
            @Override
            public void inject(Object object) {
                container.inject(object);
            }
        });
    }

    @Override
    public void destroy() {
        if (container != null) {
            container.clear();
        }
    }
}
