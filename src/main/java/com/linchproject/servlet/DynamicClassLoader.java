package com.linchproject.servlet;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author Georg Schmidl
 */
public class DynamicClassLoader extends ClassLoader {

    private String rootPackage;

    private Map<String, Class<?>> compiledClasses = new HashMap<String, Class<?>>();

    public DynamicClassLoader(ClassLoader classLoader){
        this(classLoader, null);
    }

    public DynamicClassLoader(ClassLoader classLoader, String rootPackage){
        super(classLoader);
        this.rootPackage = rootPackage;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = compiledClasses.get(name);

        if (clazz == null) {
            try {
                if(rootPackage == null || name.startsWith(rootPackage + ".")){
                    if (!name.contains("$")) {
                        compileClass(name);
                    }

                    String classFileName = name.replace('.', File.separatorChar) + ".class";
                    InputStream inputStream = getClassFileAsStream(classFileName);
                    if (inputStream != null) {
                        byte[] classBytes = getBytes(inputStream);
                        clazz = defineClass(name, classBytes, 0, classBytes.length);
                    }
                }
            }
            catch(Exception e){
                // ignore
            }

            if (clazz == null) {
                clazz = super.loadClass(name);
            }

            compiledClasses.put(name, clazz);
        }

        return clazz;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        File file = new File("src" + File.separator + "main" + File.separator + "resources", name);

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            inputStream = super.getResourceAsStream(name);
        }
        return inputStream;
    }

    protected static InputStream getClassFileAsStream(String classFileName) {
        InputStream inputStream = null;
        try {
            String targetDirectory = "target" + File.separator + "classes";
            inputStream = new FileInputStream(targetDirectory + File.separator + classFileName);
        } catch (FileNotFoundException e) {
           // ignore
        }
        return inputStream;
    }

    protected static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    protected static synchronized Boolean compileClass(String classFileName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        String targetDirectory = "target" + File.separator + "classes";
        String sourceDirectory = "src" + File.separator + "main" + File.separator + "java";
        String sourceFileName = classFileName.replace(".", File.separator) + ".java";

        URLClassLoader loader = ((URLClassLoader) Thread.currentThread().getContextClassLoader());
        URL[] urls = loader.getURLs();

        List<String> options = new ArrayList<String>();
        options.add("-classpath");
        options.add(getClasspath(urls));
        options.add("-d");
        options.add(targetDirectory);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> javaFileObjects =
                fileManager.getJavaFileObjects(sourceDirectory + File.separator + sourceFileName);

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, null, options, null, javaFileObjects);

        return task.call();
    }

    protected static String getClasspath(URL[] urls) {
        StringBuilder sb = new StringBuilder();
        for (URL url : urls) {
            sb.append(url.getPath());
            sb.append(File.pathSeparator);
        }
        return sb.toString();
    }
}
