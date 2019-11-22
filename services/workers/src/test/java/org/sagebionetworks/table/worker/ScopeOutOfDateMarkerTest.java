package org.sagebionetworks.table.worker;

import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class ScopeOutOfDateMarkerTest {

	@Mock
	Clock mockClock;
	@Mock
	TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexManager mockIndexManager;
	@Mock
	ProgressCallback mockCallback;

	@InjectMocks
	ScopeOutOfDateMarker marker;
	
	long now;
	ChangeMessage change;
	
	@BeforeEach
	public void beforeEach() {
		now = 1000 * 60 * 100;
		
		long changeTime = (now - ScopeOutOfDateMarker.MAX_MESSAGE_AGE_MS) + 1;
		change = new ChangeMessage();
		change.setObjectType(ObjectType.ENTITY);
		change.setObjectId("syn123");
		change.setTimestamp(new Date(changeTime));
	}

	@Test
	public void testRunRecentEntity() throws Exception {
		when(mockConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockClock.currentTimeMillis()).thenReturn(now);

		// call under test
		marker.run(mockCallback, change);

		verify(mockIndexManager).markEntityScopeOutOfDate(change.getObjectId());
	}
	

	@Test
	public void testRunRecentNonEntity() throws Exception {
		// non-entity
		change.setObjectType(ObjectType.ACTIVITY);
		
		// call under test
		marker.run(mockCallback, change);

		verify(mockIndexManager, never()).markEntityScopeOutOfDate(anyString());

	}
	
	@Test
	public void testRunOldEntity() throws Exception {
		// old change
		long changeTime = (now - ScopeOutOfDateMarker.MAX_MESSAGE_AGE_MS);
		change.setTimestamp(new Date(changeTime));
		
		when(mockClock.currentTimeMillis()).thenReturn(now);

		// call under test
		marker.run(mockCallback, change);

		verify(mockIndexManager, never()).markEntityScopeOutOfDate(anyString());

	}

}
