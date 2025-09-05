package org.sagebionetworks.repo.model.jdo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;

/**
 * Basic test for converting between JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtilsTest {

	UnmodifiableXStream TEST_X_STREAM = UnmodifiableXStream.builder().allowTypes(TestObject.class).build();


	@Test
	public void decompressedObject_nullBytes() throws IOException {
		assertNull(JDOSecondaryPropertyUtils.decompressObject(TEST_X_STREAM,null));
	}


	@Test
	public void compressObject_nullBytes() throws IOException {
		assertNull(JDOSecondaryPropertyUtils.compressObject(TEST_X_STREAM,null));
	}

	@Test
	public void roundTrip() throws IOException {
		TestObject nested = new TestObject(1,"2", 3L, null);
		TestObject test = new TestObject(4,"5", 6L, nested);

		byte[] bytes = JDOSecondaryPropertyUtils.compressObject(TEST_X_STREAM, test);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		assertEquals(test, JDOSecondaryPropertyUtils.decompressObject(TEST_X_STREAM, bytes));
	}

	@Test
	public void testWriteAndReadJsonToEntityList() {
		List<EntityRef> items = Arrays.asList(new EntityRef().setEntityId("syn111").setVersionNumber(1L),
				new EntityRef().setEntityId("syn222").setVersionNumber(2L));
		// Call under test
		String json = JDOSecondaryPropertyUtils.writeEntityListToJson(items);
		assertNotNull(json);
		// call under test
		List<EntityRef> clone = JDOSecondaryPropertyUtils.readJsonToEntityList(json, EntityRef.class);
		assertEquals(items, clone);
	}
	
	@Test
	public void testWriteEntityListToJsonWithNull() {
		assertNull(JDOSecondaryPropertyUtils.writeEntityListToJson(null));
	}
	
	@Test
	public void testReadJsonToEntityListWithNull() {
		assertNull(JDOSecondaryPropertyUtils.readJsonToEntityList(null, EntityRef.class));
	}
	
	@Test
	public void testReadJsonToEntityListWithInvalidJson() {
		assertThrows(IllegalArgumentException.class, ()->{
			JDOSecondaryPropertyUtils.readJsonToEntityList("invalid json", EntityRef.class);
		});
	}
	
	@Test
	public void testWriteAndReadJsonToStringList() {
		List<String> items = List.of("one", "two", "three");
		
		String json = JDOSecondaryPropertyUtils.writeStringListToJson(items);
		
		assertEquals("[\"one\",\"two\",\"three\"]", json);
		
		List<String> clone = JDOSecondaryPropertyUtils.readJsonToStringList(json);
		
		assertEquals(items, clone);
	}
	
	@Test
	public void testWriteJsonToStringListWithNull() {
		List<String> items = null;
		
		assertNull(JDOSecondaryPropertyUtils.writeStringListToJson(items));

	}
	
	@Test
	public void testReadJsonToStringListWithNull() {
		String json = null;
		
		assertNull(JDOSecondaryPropertyUtils.readJsonToStringList(json));
	}

	static class TestObject{
		int a;
		String b;
		long c;
		TestObject other;

		public TestObject(int a, String b, long c, TestObject other) {
			this.a = a;
			this.b = b;
			this.c = c;
			this.other = other;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestObject that = (TestObject) o;
			return a == that.a &&
					c == that.c &&
					Objects.equals(b, that.b) &&
					Objects.equals(other, that.other);
		}

		@Override
		public int hashCode() {
			return Objects.hash(a, b, c, other);
		}
	}
}
