package org.sagebionetworks.repo.web.controller;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Note: This class if from: http://tedyoung.me/2011/02/14/spring-mvc-integration-testing-controllers/
 * A Spring {@link ContextLoader} that establishes a mock Servlet environment and {@link WebApplicationContext}
 * so that Spring MVC stacks can be tested from within JUnit.
 */
public class MockWebApplicationContextLoader extends AbstractContextLoader {
        /**
         * The configuration defined in the {@link MockWebApplication} annotation.
         */
        private MockWebApplication configuration;
        
        @Override
        @SuppressWarnings("serial")
        public ApplicationContext loadContext(String... locations) throws Exception {
                // Establish the servlet context and config based on the test class's MockWebApplication annotation.
                final MockServletContext servletContext = new MockServletContext(configuration.webapp(), new FileSystemResourceLoader());
                final MockServletConfig servletConfig = new MockServletConfig(servletContext, configuration.name());
                
                // Create a WebApplicationContext and initialize it with the xml and servlet configuration.
                final XmlWebApplicationContext webApplicationContext = new XmlWebApplicationContext();
                servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
                webApplicationContext.setServletConfig(servletConfig);
                webApplicationContext.setConfigLocations(locations);
                
                // Create a DispatcherServlet that uses the previously established WebApplicationContext.
                final DispatcherServlet dispatcherServlet = new DispatcherServlet() {
                        @Override
                        protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
                                return webApplicationContext;
                        }
                };
                
                // Add the DispatcherServlet (and anything else you want) to the context.
                // Note: this doesn't happen until refresh is called below.
                webApplicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
                        @Override
                        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                                beanFactory.registerResolvableDependency(DispatcherServlet.class, dispatcherServlet);
                                // Register any other beans here, including a ViewResolver if you are using JSPs.
                        }
                });
                 
                // Have the context notify the servlet every time it is refreshed.
                webApplicationContext.addApplicationListener(new SourceFilteringListener(webApplicationContext, new ApplicationListener<ContextRefreshedEvent>() {
                        @Override
                        public void onApplicationEvent(ContextRefreshedEvent event) {
                                dispatcherServlet.onApplicationEvent(event);
                        }
                }));
                
                // Prepare the context.
                webApplicationContext.refresh();
                webApplicationContext.registerShutdownHook();
                
                // Initialize the servlet.
                dispatcherServlet.setContextConfigLocation("");
                dispatcherServlet.init(servletConfig);
                
                return webApplicationContext;
        }
        
        /**
         * One of these two methods will get called before {@link #loadContext(String...)}.
         * We just use this chance to extract the configuration.
         */
        @Override
        protected String[] generateDefaultLocations(Class<?> clazz) {
                extractConfiguration(clazz);            
                return super.generateDefaultLocations(clazz);
        }
        
        /**
         * One of these two methods will get called before {@link #loadContext(String...)}.
         * We just use this chance to extract the configuration.
         */
        @Override
        protected String[] modifyLocations(Class<?> clazz, String... locations) {
                extractConfiguration(clazz);
                return super.modifyLocations(clazz, locations);
        }
        
        private void extractConfiguration(Class<?> clazz) {
                configuration = AnnotationUtils.findAnnotation(clazz, MockWebApplication.class);
                if (configuration == null)
                        throw new IllegalArgumentException("Test class " + clazz.getName() + " must be annotated @MockWebApplication.");
        }
        
        @Override
        protected String getResourceSuffix() {
                return "-context.xml";
        }

}

