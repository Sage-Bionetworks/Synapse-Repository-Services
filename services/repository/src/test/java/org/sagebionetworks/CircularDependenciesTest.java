package org.sagebionetworks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class CircularDependenciesTest {

	/**
	 * This is a test to detect where we have circular dependencies. It is currently
	 * disabled because we have many circular dependencies. It will take multiple
	 * iterations to find and fix all issues.
	 */
	@Test
	public void testCircularReferences() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setAllowCircularReferences(false);
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
		xmlReader.loadBeanDefinitions(new ClassPathResource("test-context.xml"));
		// call under test
		ctx.refresh();
	}
}
