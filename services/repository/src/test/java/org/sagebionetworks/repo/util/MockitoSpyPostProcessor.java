package org.sagebionetworks.repo.util;

import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

public class MockitoSpyPostProcessor implements BeanPostProcessor, Ordered {

    private Set<Object> spyNames;

    public void setSpyNames(Set<Object> spyNames) {
        this.spyNames = spyNames;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (spyNames != null && spyNames.contains(beanName)) {
            return Mockito.spy(bean);
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}