package com.linchproject.servlet;

import com.linchproject.core.Injector;
import com.linchproject.core.Invoker;
import com.linchproject.core.Result;
import com.linchproject.core.Route;
import com.linchproject.core.results.Error;
import com.linchproject.dev.DynamicClassLoader;
import com.linchproject.ioc.Container;
import com.linchproject.servlet.services.ServletCookieService;
import com.linchproject.servlet.services.ServletLocaleService;
import com.linchproject.servlet.services.ServletSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * @author Georg Schmidl
 */
public class DispatcherServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

    private static final String CONTROLLER_SUB_PACKAGE = "controllers";

    private ClassLoader classLoader;
    private DataSource dataSource;

    private Container container;
    private InvokerWrapper invokerWrapper;

    private Properties appProperties;
    private Properties dbProperties;
    private List<Properties> componentPropertiesList;

    @Override
    public void init() throws ServletException {
        log.info("initializing");
        classLoader = getClass().getClassLoader();

        loadAppProperties();
        loadDbProperties();
        loadComponentProperties();

        if (appProperties == null) {
            throw new ServletException("linch-app.properties is missing");
        }

        dataSource = getDataSource();

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

        String appPackage = appProperties.getProperty("package");
        String controllersPackage = appPackage != null? appPackage + "." + CONTROLLER_SUB_PACKAGE : CONTROLLER_SUB_PACKAGE;

        Route route = new ServletRoute(request);
        route.setControllerPackage(controllersPackage);

        Result result = invokerWrapper.invoke(route);

        ReplierFactory.getReplier(result).reply(response);

        ServletThreadLocal.removeRequest();
        ServletThreadLocal.removeResponse();
    }

    private Container createContainer(ClassLoader classLoader) throws ServletException {
        log.info("creating IOC container");
        Container container = new Container();

        if (dataSource != null) {
            container.add("dataSource", dataSource);
        }
        container.add("classLoader", classLoader);
        container.add("sessionService", ServletSessionService.class);
        container.add("cookieService", ServletCookieService.class);
        container.add("localeService", ServletLocaleService.class);

        if (componentPropertiesList != null) {
            for (Properties componentProperties : componentPropertiesList) {
                Enumeration<?> enumeration = componentProperties.propertyNames();
                while (enumeration.hasMoreElements()) {
                    String componentName = (String) enumeration.nextElement();
                    String componentClassName = componentProperties.getProperty(componentName);

                    Class<?> componentClass;
                    try {
                        componentClass = classLoader.loadClass(componentClassName);
                    } catch (ClassNotFoundException e) {
                        throw new ServletException("error loading class for component " + componentName, e);
                    }
                    log.debug("adding component {} to container ", componentClassName);
                    container.add(componentName, componentClass);
                }
            }
        }
        return container;
    }

    private DataSource getDataSource() throws ServletException {
        log.info("loading dataSource");
        DataSource dataSource = null;

        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/linch");
        } catch (NamingException e) {
            log.warn("no resource found with name 'jdbc/linch'");
        }

        return dataSource;
    }

    @Override
    public void destroy() {
        if (container != null) {
            container.clear();
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

            if (!(result instanceof Error)) {
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
            ClassLoader dynamicClassLoader = new DynamicClassLoader(classLoader, appProperties.getProperty("package"));
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
        log.info("creating invoker");
        return new Invoker(classLoader, new Injector() {
            @Override
            public void inject(Object object) {
                container.inject(object);
            }
        });
    }

    private void loadAppProperties() throws ServletException {
        log.info("loading ap.properties");
        appProperties = loadProperties(classLoader, "app.properties");
    }

    private void loadDbProperties() throws ServletException {
        log.info("loading db.properties");
        dbProperties = loadProperties(classLoader, "db.properties");
    }

    private void loadComponentProperties() throws ServletException {
        log.info("loading components.properties");
        componentPropertiesList = new ArrayList<Properties>();

        URLClassLoader contextClassLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
        for (URL url : contextClassLoader.getURLs()) {
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url });
            log.debug("loading components.properties from {}", url.toString());
            Properties componentProperties = loadProperties(urlClassLoader, "components.properties");
            if (componentProperties != null) {
                componentPropertiesList.add(componentProperties);
            }
        }
    }

    private Properties loadProperties(ClassLoader classLoader, String propertiesFileName) throws ServletException {
        Properties properties = null;
        InputStream inputStream = classLoader.getResourceAsStream(propertiesFileName);
        if (inputStream != null) {
            try {
                properties = new Properties();
                properties.load(inputStream);
            } catch (IOException e) {
                throw new ServletException(e.getMessage(), e);
            }
        }
        return properties;
    }
}
