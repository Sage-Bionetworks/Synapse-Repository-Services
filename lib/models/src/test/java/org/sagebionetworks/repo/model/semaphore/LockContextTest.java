package org.sagebionetworks.repo.model.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersionParser;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;

public class LockContextTest {

	@Test
	public void testSerializeAndDeserialize() {
		LockContext context = new LockContext(ContextType.Query, IdAndVersionParser.parseIdAndVersion("syn123.1"));
		String serialized = context.serializeToString();
		LockContext clone = LockContext.deserialize(serialized);
		assertEquals(context, clone);
	}

	@Test
	public void testDeserializeNull() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			LockContext.deserialize(null);
		}).getMessage();
		assertEquals("Serialized string cannot be null is required.", message);
	}
	
	@Test
	public void testDeserializeInvalid() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			LockContext.deserialize("wrong");
		}).getMessage();
		assertEquals("Invalid serialized string: 'wrong'", message);
	}
}
