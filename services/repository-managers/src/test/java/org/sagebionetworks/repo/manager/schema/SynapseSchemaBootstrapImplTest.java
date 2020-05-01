package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class SynapseSchemaBootstrapImplTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Mock
	private UserManager mockUserManager;
	
	@Mock
	SchemaTranslator mockTranslator;

	@InjectMocks
	private SynapseSchemaBootstrapImpl bootstrap;
	
	ObjectSchemaImpl objectSchema;
	
	@BeforeEach
	public void before() {

	}

	@Test
	public void testLoadAllSchemasAndReferences() {
		// One is a leaf
		ObjectSchemaImpl one = new ObjectSchemaImpl(TYPE.STRING);
		one.setId("one");
		
		ObjectSchemaImpl refToOne = new ObjectSchemaImpl();
		refToOne.setRef(one.getId());
		
		// two depends on one
		ObjectSchemaImpl two = new ObjectSchemaImpl(TYPE.ARRAY);
		two.setId("two");
		two.setItems(refToOne);
		
		ObjectSchemaImpl refToTwo = new ObjectSchemaImpl();
		refToTwo.setRef(two.getId());
		
		// Three depends on two
		ObjectSchemaImpl three = new ObjectSchemaImpl(TYPE.OBJECT);
		three.setId("three");
		three.setImplements(new ObjectSchemaImpl[] {refToTwo});
		
		// For depends on one
		ObjectSchemaImpl four = new ObjectSchemaImpl(TYPE.OBJECT);
		four.setId("four");
		four.setImplements(new ObjectSchemaImpl[] {refToOne});
		
		when(mockTranslator.loadSchemaFromClasspath(one.getId())).thenReturn(one);
		when(mockTranslator.loadSchemaFromClasspath(two.getId())).thenReturn(two);
		when(mockTranslator.loadSchemaFromClasspath(three.getId())).thenReturn(three);
		when(mockTranslator.loadSchemaFromClasspath(four.getId())).thenReturn(four);
		
		// loading three and four should trigger the load of all four.
		List<String> rootIds = Lists.newArrayList(three.getId(), four.getId());
		// call under test
		List<ObjectSchemaImpl> results = bootstrap.loadAllSchemasAndReferences(rootIds);
		assertNotNull(results);
		assertEquals(4, results.size());
		// Each dependency must come before the schema which depends on it.
		assertEquals(one, results.get(0));
		assertEquals(two, results.get(1));	
		assertEquals(three, results.get(2));	
		assertEquals(four, results.get(3));	
	}

}
