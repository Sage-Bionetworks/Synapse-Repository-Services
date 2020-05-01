package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class RequiredScopeAnnotationTest {

	@Test
	public void testCoverage() throws Exception {

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		// Find controllers
		provider.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
		// Find controllers in the given package (or subpackages)
		Set<BeanDefinition> beans = provider.findCandidateComponents("org.sagebionetworks");
		List<String> missingRequiredScope = new ArrayList<String>();
		for (BeanDefinition bd : beans) {
			String className = bd.getBeanClassName();
			Class<?> clazz = Class.forName(className);
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.getAnnotation(RequiredScope.class)==null && method.getAnnotation(RequestMapping.class)!=null) {
					missingRequiredScope.add(className+"."+method.getName()+"()");
				}
			}
		}
		if (!missingRequiredScope.isEmpty()) {
			fail("The following "+
				missingRequiredScope.size()+
				" services are missing the required RequiredScope annotation:\n"+
				String.join("\n", missingRequiredScope));
		}
	}

}
