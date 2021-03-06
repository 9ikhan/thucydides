package net.thucydides.core.reflection;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Load classes from a given package.
 */
public class ClassFinder {

    private final ClassLoader classLoader;
    private final Class annotation;

    private ClassFinder(ClassLoader classLoader, Class annotation) {
        this.classLoader = classLoader;
        this.annotation = annotation;
    }

    private ClassFinder(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public static ClassFinder loadClasses() {
        return new ClassFinder(getDefaultClassLoader());
    }

    public ClassFinder annotatedWith(Class annotation) {
        return new ClassFinder(this.classLoader, annotation);
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     */
    public List<Class<?>> fromPackage(String packageName) {

        return filtered(getClasses(packageName));
    }

    private List<Class<?>> filtered(Collection<Class<?>> classes) {
        List<Class<?>> matchingClasses = new ArrayList<Class<?>>();

        for (Class clazz : classes) {
            if (matchesConstraints(clazz)) {
                matchingClasses.add(clazz);
            }
        }
        return matchingClasses;
    }

    private boolean matchesConstraints(Class clazz) {
        if (annotation == null) {
            return true;
        } else {
            return (clazz.getAnnotation(annotation) != null);
        }
    }

    private static ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static boolean isNotAnInnerClass(String className) {
        return (!className.contains("$"));
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     * Adapted from http://snippets.dzone.com/posts/show/4831 and extended to support use of JAR files
     * @param packageName The base package
     * @return The classes
     */
    public static List<Class<?>> getClasses(String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            assert classLoader != null;
            String path = packageName.replace('.', '/');
            Enumeration resources = classLoader.getResources(path);
            List<URI> dirs = Lists.newArrayList();
            while (resources.hasMoreElements()) {
                URL resource = (URL) resources.nextElement();
                dirs.add(resource.toURI());
            }
            Set<String> classes = Sets.newTreeSet();
            for (URI directory : dirs) {
                classes.addAll(findClasses(directory, packageName));
            }
            List<Class<?>> classList = Lists.newArrayList();
            for (String className : classes) {
                try {
                    if (className.startsWith(packageName) && isNotAnInnerClass(className)) {
                        classList.add(Class.forName(className));
                    }
                } catch (Throwable e) {
                    System.out.println("Could not load class " + className);
                    //throw new RuntimeException("Could not load class", e);
                }
            }
            return classList;
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs. * Adapted from http://snippets.dzone.com/posts/show/4831 and extended to support use of JAR files * @param directory The base directory * @param packageName The package name for classes found inside the base directory * @return The classes * @throws ClassNotFoundException
     */
    private static TreeSet<String> findClasses(URI directory, String packageName) throws Exception {
        final String scheme = directory.getScheme();
        final String schemeSpecificPart = directory.getSchemeSpecificPart();

        if (scheme.equals("jar") && schemeSpecificPart.contains("!")) {
            return findClassesInJar(directory);
        } else if (scheme.equals("file")) {
            return findClassesInFileSystemDirectory(directory, packageName);
        }

        throw new IllegalArgumentException(
                "cannot handle URI with scheme [" + scheme + "]" +
                "; received directory=[" + directory + "], packageName=[" + packageName + "]"
        );
    }

    private static TreeSet<String> findClassesInJar(URI jarDirectory) throws Exception {
        final String schemeSpecificPart = jarDirectory.getSchemeSpecificPart();

        TreeSet<String> classes = Sets.newTreeSet();

        String[] split = schemeSpecificPart.split("!");
        URL jar = new URL(split[0]);
        ZipInputStream zip = new ZipInputStream(jar.openStream());
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().endsWith(".class")) {
                String className = classNameFor(entry);
                if (isNotAnInnerClass(className)) {
                    classes.add(className);
                }
            }
        }

        return classes;
    }

    private static TreeSet<String> findClassesInFileSystemDirectory(URI fileSystemDirectory, String packageName) throws Exception {
        TreeSet<String> classes = Sets.newTreeSet();

        File dir = new File(fileSystemDirectory);
        if (!dir.exists()) {
            return classes;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(
                        findClassesInFileSystemDirectory(file.getAbsoluteFile().toURI(), packageName + "." + file.getName())
                );
            } else if (file.getName().endsWith(".class")) {
                classes.add(
                        packageName + '.' + file.getName().substring(0, file.getName().length() - 6)
                );
            }
        }

        return classes;
    }

    private static String classNameFor(ZipEntry entry) {
        return entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
    }
}

