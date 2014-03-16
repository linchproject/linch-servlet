package com.linchproject.servlet;

/**
 * @author Georg Schmidl
 */
public class Environment {

    private static final String DEV_PROPERTY = System.getProperty("com.linchproject.dev");

    public static final boolean DEV = DEV_PROPERTY != null && "true".equals(DEV_PROPERTY);
}
