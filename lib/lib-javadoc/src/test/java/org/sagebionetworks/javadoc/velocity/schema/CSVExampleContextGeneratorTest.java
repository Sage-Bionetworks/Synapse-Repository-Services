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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

public class CSVExampleContextGeneratorTest {

	
	private static RootDoc rootDoc;
	private static ClassDoc exampleClassDoc;
	private static Map<String, MethodDoc> methodMap;
	@BeforeClass
	public static void beforeClass(){
		// Lookup the test files.
		rootDoc = JavaDocTestUtil.buildRootDoc("CSVExample.java");
		assertNotNull(rootDoc);
        Iterator<ClassDoc> exmples = FilterUtils.csvExampleIterator(rootDoc.classes());
        assertNotNull(exmples);
        assertTrue(exmples.hasNext());
        exampleClassDoc = exmples.next();
        assertNotNull(exampleClassDoc);
	}
	
	@Test
	public void testGenerateContext() throws Exception{
		ContextFactoryImpl factory = new ContextFactoryImpl();
		CSVExampleContextGenerator generator = new CSVExampleContextGenerator();
		List<ClassContext> contextList = generator.generateContext(factory, rootDoc);
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
