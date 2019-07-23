package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class VerificationFileHandleAssociationProviderTest {
	
	private VerificationDAO mockVerificationDao;
	private VerificationFileHandleAssociationProvider provider;
	private static final Long VERIFICATION_ID = 111L;
	private static final Long FH_ID = 222L;
	
	@Before
	public void before() {
		mockVerificationDao = Mockito.mock(VerificationDAO.class);
		provider = new VerificationFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "verificationDao", mockVerificationDao);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		when(mockVerificationDao.listFileHandleIds(VERIFICATION_ID)).
		thenReturn(Collections.singletonList(FH_ID));
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(FH_ID.toString(), "333"), VERIFICATION_ID.toString());
		assertEquals(Collections.singleton(FH_ID.toString()), associated);
	}

}
