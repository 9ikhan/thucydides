package net.thucydides.junit.finder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.thucydides.core.reflection.ClassFinder;
import net.thucydides.junit.annotations.UseTestDataFrom;
import net.thucydides.junit.runners.ThucydidesRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The TestFinder class lets you find the Thucydides tests or test methods underneath a given package.
 * <p>You instantiate a TestFinder by providing the top-level package where the tests live.</p>
 * <p>You can then find the list of Thucydides test classes using getNormalTestClasses(), getDataDrivenTestClasses(),
 * and getAllTestClasses() (which returns both normal and data-driven tests).</p>
 * <p>You may also need to retrieve the list of test methods for a particular category of class. You can do this using the
 * getTestMethodsFrom() method, e.g.
 * <pre>new TestFinder("my.package").getTestMethodsFrom().normalTestClasses()</pre>
 * </p>
 */
public abstract class TestFinder {

    protected final String rootPackage;

    /**
     * Create a new test finder instance that will look for tests in the packages underneath the given root package.
     */
    protected TestFinder(final String rootPackage) {
        this.rootPackage = rootPackage;
    }

    public static TestFinderBuilderFactory thatFinds() {
        return new TestFinderBuilderFactory();
    }
    
    public abstract List<Class<?>> getClasses();

    public abstract int countTestMethods();

    public TestMethodFinder findTestMethods() {
        return new TestMethodFinder(this);
    }

    protected List<Class<?>> getAllTestClasses() {
        return ClassFinder.loadClasses().annotatedWith(RunWith.class).fromPackage(rootPackage);
    }

    protected Set<Class<?>> getNormalTestClasses() {
        Set<Class<?>> normalTestClasses = Sets.newHashSet();
        for(Class<?> testClass : getAllTestClasses()) {
            if (normalThucydidesTest(testClass)) {
                normalTestClasses.add(testClass);
            }
        }
        return normalTestClasses;
    }

    protected List<Class<?>> getDataDrivenTestClasses() {
        return ClassFinder.loadClasses().annotatedWith(UseTestDataFrom.class).fromPackage(rootPackage);
    }

    protected List<Class<?>> sorted(List<Class<?>> classes) {
        Collections.sort(classes, byClassName());
        return classes;
    }

    private boolean normalThucydidesTest(Class<?> testClass) {
        RunWith runWith = testClass.getAnnotation(RunWith.class);
        return ((runWith != null) && (runWith.value() == ThucydidesRunner.class));
    }

    public List<Method> getAllTestMethods() {
        return findMethodsFrom(getClasses());
    }

    private List<Method> findMethodsFrom(List<Class<?>> testClasses) {
        List<Method> methods = Lists.newArrayList();

        for (Class<?> testClass : testClasses) {
            addEachMatchingTestMethodFrom(testClass).to(methods);
        }
        Collections.sort(methods, byName());
        return methods;
    }

    private Comparator<Method> byName() {
        return new Comparator<Method>() {

            public int compare(Method method, Method method1) {
                return method.getName().compareTo(method1.getName());
            }
        };
    }

    private static class TestMethodSearchBuilder {
        private final Class<?> testClass;

        private TestMethodSearchBuilder(Class<?> testClass) {
            this.testClass = testClass;
        }


        public void to(List<Method> methods) {
            for(Method method : testClass.getMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    methods.add(method);
                }
            }
        }
    }

    private TestMethodSearchBuilder addEachMatchingTestMethodFrom(Class<?> testClass) {
        return new TestMethodSearchBuilder(testClass);
    }

    private Comparator<Class<?>> byClassName() {
        return new Comparator<Class<?>>() {

            public int compare(Class<?> class1, Class<?> class2) {
                return class1.getName().compareTo(class2.getName());
            }
        };
    }


}
