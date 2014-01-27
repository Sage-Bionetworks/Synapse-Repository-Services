package org.sagebionetworks.bridge.manager.participantdata;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sagebionetworks.bridge.manager.community.MockitoTestBase;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;

import com.google.common.collect.Lists;

public class ParticipantDataIdMappingManagerImplTest extends MockitoTestBase {

	@Mock
	private BridgeParticipantDAO participantDAO;
	@Mock
	private BridgeUserParticipantMappingDAO userParticipantMappingDAO;
	@Mock
	private Random random;

	@Before
	public void doBefore() {
		initMockito();
	}

	@Test
	public void randomCollision() throws Exception {
		ParticipantDataIdMappingManager mappingManager = new ParticipantDataIdMappingManagerImpl(participantDAO, userParticipantMappingDAO,
				random);

		when(random.nextLong()).thenReturn(12L).thenReturn(13L);
		doThrow(new DatastoreException()).when(participantDAO).create(12L);

		when(userParticipantMappingDAO.getParticipantIdsForUser(1009L)).thenReturn(Lists.newArrayList("2"));

		UserInfo user = new UserInfo(false, 1009L);
		mappingManager.createNewParticipantForUser(user);

		verify(random, times(2)).nextLong();
		verify(participantDAO, times(2)).create(anyLong());
		verify(userParticipantMappingDAO).getParticipantIdsForUser(1009L);
		verify(userParticipantMappingDAO).setParticipantIdsForUser(1009L, Lists.newArrayList("2", "13"));
	}

	@Test(expected = DatastoreException.class)
	public void tooManyCollisions() throws Exception {
		ParticipantDataIdMappingManager mappingManager = new ParticipantDataIdMappingManagerImpl(participantDAO, userParticipantMappingDAO,
				random);

		when(random.nextLong()).thenReturn(12L);
		doThrow(new DatastoreException()).when(participantDAO).create(12L);

		when(userParticipantMappingDAO.getParticipantIdsForUser(1009L)).thenReturn(Lists.newArrayList("2"));

		UserInfo user = new UserInfo(false, 1009L);
		try {
			mappingManager.createNewParticipantForUser(user);
		} finally {
			verify(random, times(10)).nextLong();
			verify(participantDAO, times(10)).create(anyLong());
			verifyZeroInteractions(userParticipantMappingDAO);
		}
	}
}
