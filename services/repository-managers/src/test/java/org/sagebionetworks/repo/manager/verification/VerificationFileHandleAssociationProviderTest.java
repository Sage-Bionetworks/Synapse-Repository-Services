package org.sagebionetworks.repo.manager.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class VerificationFileHandleAssociationProviderTest {
	
	@Mock
	private VerificationDAO mockVerificationDao;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
		
	@InjectMocks
	private VerificationFileHandleAssociationProvider provider;
	
	private static final Long VERIFICATION_ID = 111L;
	private static final Long FH_ID = 222L;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		when(mockVerificationDao.listFileHandleIds(VERIFICATION_ID)).
		thenReturn(Collections.singletonList(FH_ID));
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(Arrays.asList(FH_ID.toString(), "333"), VERIFICATION_ID.toString());
		assertEquals(Collections.singleton(FH_ID.toString()), associated);
	}
	
	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.VERIFICATION_SUBMISSION, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.VerificationSubmission, provider.getAssociateType());
	}

}
