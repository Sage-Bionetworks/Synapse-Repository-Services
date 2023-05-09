package org.sagebionetworks.repo.model.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersionParser;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;

public class LockContextTest {

	@Test
	public void testConstructorWithNullType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new LockContext(null, IdAndVersionParser.parseIdAndVersion("syn123.1"));
		}).getMessage();
		assertEquals("type is required.", message);
	}

	@Test
	public void testConstructorWithNullObjectId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new LockContext(ContextType.Query, null);
		}).getMessage();
		assertEquals("objectId is required.", message);
	}

	@Test
	public void tetGetDisplayString() {
		assertEquals("Applying an update to table: 'syn123.1' ...",
				new LockContext(ContextType.TableUpdate, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Creating a snapshot of table: 'syn123.1' ...",
				new LockContext(ContextType.TableSnapshot, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Creating a snapshot of view: 'syn123.1' ...",
				new LockContext(ContextType.ViewSnapshot, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Rebuliding table index: 'syn123.1' ...",
				new LockContext(ContextType.BuildTableIndex, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Rebuilding view inxex: 'syn123.1' ...",
				new LockContext(ContextType.BuildViewIndex, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Updating view inxex: 'syn123.1' ...",
				new LockContext(ContextType.UpdatingViewIndex, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
		assertEquals("Querying table/view: 'syn123.1' ...",
				new LockContext(ContextType.Query, IdAndVersionParser.parseIdAndVersion("syn123.1")).toDisplayString());
		assertEquals("Rebuilding materialized view: 'syn123.1' ...",
				new LockContext(ContextType.BuildMaterializedView, IdAndVersionParser.parseIdAndVersion("syn123.1"))
						.toDisplayString());
	}

	@Test
	public void testSerializeAndDeserialize() {
		LockContext context = new LockContext(ContextType.Query, IdAndVersionParser.parseIdAndVersion("syn123.1"));
		String serialized = context.serializeToString();
		LockContext clone = LockContext.deserialize(serialized);
		assertEquals(context, clone);
	}

	@Test
	public void testDeserializeNull() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			LockContext.deserialize(null);
		}).getMessage();
		assertEquals("Serialized string cannot be null is required.", message);
	}

	@Test
	public void testDeserializeInvalid() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			LockContext.deserialize("wrong");
		}).getMessage();
		assertEquals("Invalid serialized string: 'wrong'", message);
	}

	@Test
	public void testToWaitingOnMessage() {
		LockContext waitingOn = new LockContext(ContextType.Query, IdAndVersionParser.parseIdAndVersion("syn123.1"));
		LockContext current = new LockContext(ContextType.TableUpdate,
				IdAndVersionParser.parseIdAndVersion("syn123.1"));
		// call under test
		assertEquals("[TableUpdate, syn123.1] waiting on [Query, syn123.1]", current.toWaitingOnMessage(waitingOn));
	}

	@Test
	public void testToWaitingOnMessageWithNullWaiting() {
		LockContext waitingOn = null;
		LockContext current = new LockContext(ContextType.TableUpdate,
				IdAndVersionParser.parseIdAndVersion("syn123.1"));
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			current.toWaitingOnMessage(waitingOn);
		}).getMessage();
		assertEquals("waitingOn is required.", message);
	}
}
