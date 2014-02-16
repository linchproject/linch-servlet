package com.linchproject.servlet;

import java.io.IOException;
import java.util.*;

/**
 * @author Georg Schmidl
 */
public class AppConfig {

    private Properties properties;

    protected AppConfig(Properties properties) {
        this.properties = properties;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public List<String> getList(String key) {
        List<String> list = new ArrayList<String>();
        for (String value: get(key).split(",")) {
            list.add(value.trim());
        }
        return list;
    }

    public Map<String, String> getMap(String keyPrefix) {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();

            if (key.startsWith(keyPrefix)) {
                String subKey = key.substring(key.indexOf(".") + 1, key.length());
                map.put(subKey, get(key));
            }
        }
        return map;
    }

    public static AppConfig load(ClassLoader classLoader, String path) throws IOException {
        Properties properties = new Properties();
        properties.load(classLoader.getResourceAsStream(path));
        return new AppConfig(properties);
    }
}
