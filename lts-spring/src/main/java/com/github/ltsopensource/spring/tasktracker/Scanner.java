package com.github.ltsopensource.spring.tasktracker;

import com.github.ltsopensource.core.logger.Logger;
import com.github.ltsopensource.core.logger.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * @author Robert HG (254963746@qq.com) on 10/20/15.
 */
public class Scanner implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scanner.class);

    private String[] annotationPackages;

    public void setBasePackage(String annotationPackage) {                                                  // 可以申明一个bean,并制定扫描的路径
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : Pattern.compile("\\s*[,]+\\s*").split(annotationPackage);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException { // bean实例化时会调用这个接口方法(1) - 这个是顺序步骤编号.
        // 这里其实就是让Spring扫描指定的包路径,扫描好了,bean的definition就都有了
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = Class.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner");
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class}).newInstance(beanFactory, true);
                // add filter
                Class<?> filterClass = Class.forName("org.springframework.core.type.filter.AnnotationTypeFilter");
                Object filter = filterClass.getConstructor(Class.class).newInstance(LTS.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", Class.forName("org.springframework.core.type.filter.TypeFilter"));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                Method scan = scannerClass.getMethod("scan", String[].class);
                scan.invoke(scanner, new Object[]{annotationPackages});
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {         // (2) bean实例化之前
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {    // (3) bean实例化之后

        Class<?> clazz = bean.getClass();

        if (!isMatchPackage(clazz)) {                   // 过滤包路径
            return bean;
        }

        if (!clazz.isAnnotationPresent(LTS.class)) {    // 判断是否标记了LTS注解
            return bean;
        }

        JobRunnerHolder.addLTSBean(bean);               // 标记了注解, 需要将bean加到lts缓存的Map中, 建立任务类型和处理bean之间的映射关系.

        return bean;                                    // bean原封不变的返回了
    }

    @Override
    public void destroy() throws Exception {

    }

    private boolean isMatchPackage(Class<?> clazz) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        String beanClassName = clazz.getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
