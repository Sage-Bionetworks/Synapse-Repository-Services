package org.sagebionetworks.javadoc.velocity.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.javadoc.JavaDocTestUtil;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ContextFactoryImpl;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ExecutableElement;
import jdk.javadoc.doclet.DocletEnvironment;

public class CSVExampleContextGeneratorTest {

	
	private static DocletEnvironment DocletEnvironment;
	private static TypeElement exampleTypeElement;
	private static Map<String, ExecutableElement> methodMap;
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Lookup the test files.
		DocletEnvironment = JavaDocTestUtil.buildDocletEnvironment("CSVExample.java");
		assertNotNull(DocletEnvironment);
        Iterator<TypeElement> exmples = FilterUtils.csvExampleIterator(DocletEnvironment.classes());
        assertNotNull(exmples);
        assertTrue(exmples.hasNext());
        exampleTypeElement = exmples.next();
        assertNotNull(exampleTypeElement);
	}
	
	@Test
	public void testGenerateContext() throws Exception{
		ContextFactoryImpl factory = new ContextFactoryImpl();
		CSVExampleContextGenerator generator = new CSVExampleContextGenerator();
		List<ClassContext> contextList = generator.generateContext(factory, DocletEnvironment);
		assertNotNull(contextList);
		assertEquals(1, contextList.size());
		ClassContext context = contextList.get(0);
		assertNotNull(context.getContext());
		assertEquals("org.sagebionetworks.samples.CSVExample", context.getClassName());
		assertEquals("csvExampleTemplate.html", context.getTemplateName());
		List<Category> categories = (List<Category>) context.getContext().get("categories");
		assertNotNull(categories);
		assertEquals(2, categories.size());
	}
	
}
