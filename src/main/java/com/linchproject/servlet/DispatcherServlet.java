package com.linchproject.servlet;

import com.linchproject.apps.App;
import com.linchproject.apps.AppRegistry;
import com.linchproject.core.Injector;
import com.linchproject.core.Invoker;
import com.linchproject.core.Result;
import com.linchproject.core.Route;
import com.linchproject.core.results.Error;
import com.linchproject.dev.DynamicClassLoader;
import com.linchproject.ioc.Container;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Map;

/**
 * @author Georg Schmidl
 */
public class DispatcherServlet extends HttpServlet {

    private static final String APP_PROPERTIES = "app.properties";
    private static final String CONTROLLER_SUB_PACKAGE = "controllers";

    private ClassLoader classLoader;
    private AppRegistry appRegistry;
    private ComboPooledDataSource dataSource;

    private App mainApp;

    private Container container;
    private InvokerWrapper invokerWrapper;

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

        dataSource = createDataSource();

        String devProperty = System.getProperty("com.linchproject.dev");
        if (devProperty != null && "true".equals(devProperty)) {
            invokerWrapper = new DevelopmentInvokerWrapper();
        } else {
            invokerWrapper = new ProductionInvokerWrapper();
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

        Result result = invokerWrapper.invoke(route);

        ReplierFactory.getReplier(result).reply(response);

        ServletThreadLocal.removeRequest();
        ServletThreadLocal.removeResponse();
    }

    private Container createContainer(ClassLoader classLoader) throws ServletException {
        Container container = new Container();

        container.add("app", mainApp);
        container.add("dataSource", dataSource);
        container.add("classLoader", classLoader);
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

    private ComboPooledDataSource createDataSource() throws ServletException {
        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        try {
            comboPooledDataSource.setDriverClass(mainApp.get("jdbc.driver"));
            comboPooledDataSource.setJdbcUrl(mainApp.get("jdbc.url"));
            comboPooledDataSource.setUser(mainApp.get("jdbc.user"));
            comboPooledDataSource.setPassword(mainApp.get("jdbc.password"));
        } catch (PropertyVetoException e) {
            throw new ServletException("error creating datasource", e);
        }
        return comboPooledDataSource;
    }

    @Override
    public void destroy() {
        if (container != null) {
            container.clear();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public interface InvokerWrapper {
        public Result invoke(Route route) throws ServletException;
    }

    public class ProductionInvokerWrapper implements InvokerWrapper {

        private Invoker invoker;

        public ProductionInvokerWrapper() throws ServletException {
            DispatcherServlet.this.container = createContainer(classLoader);
            this.invoker = createInvoker(classLoader, container);
        }

        @Override
        public Result invoke(Route route) {
            container.begin();

            Result result = invoker.invoke(route);

            if (!(result instanceof com.linchproject.core.results.Error)) {
                container.commit();
            } else {
                container.rollback();
            }

            return result;
        }
    }

    public class DevelopmentInvokerWrapper implements InvokerWrapper {

        @Override
        public Result invoke(Route route) throws ServletException {
            ClassLoader dynamicClassLoader = new DynamicClassLoader(classLoader, mainApp.get("package"));
            Container container = createContainer(dynamicClassLoader);
            Invoker invoker = createInvoker(dynamicClassLoader, container);

            container.begin();

            Result result = invoker.invoke(route);

            if (!(result instanceof Error)) {
                container.commit();
            } else {
                container.rollback();
            }

            container.clear();
            return result;
        }
    }

    private Invoker createInvoker(ClassLoader classLoader, final Container container) throws ServletException {
        return new Invoker(classLoader, new Injector() {
            @Override
            public void inject(Object object) {
                container.inject(object);
            }
        });
    }
}
