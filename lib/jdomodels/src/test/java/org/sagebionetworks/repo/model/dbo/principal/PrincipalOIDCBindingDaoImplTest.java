package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalOIDCBindingDaoImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private PrincipalOIDCBindingDao dao;
	
	private Long principalId;
	
	@BeforeEach
	public void before() {
		dao.truncateAll();
		principalId = userGroupDao.create(new UserGroup().setCreationDate(new Date()).setIsIndividual(true));
	}

	@AfterEach
	public void after() {
		userGroupDao.delete(principalId.toString());
		dao.truncateAll();
	}
	
	@Test
	public void testBindPrincipalToSubjectAndFind() {
		
		String subject = "subject";
		
		// Call under test
		dao.bindPrincipalToSubject(principalId, OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		assertEquals(principalId, dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).get());
	}
	
	@Test
	public void testBindPrincipalToSubjectWithExisting() {
		
		String subject = "subject";
		
		dao.bindPrincipalToSubject(principalId, OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.bindPrincipalToSubject(principalId, OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		});
	}
		
	@Test
	public void testFindBindingForSubjectWithNoMatchCase() {
		
		String subject = "subject";
		
		// Call under test
		Optional<Long> result = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		assertFalse(result.isPresent());
		
		dao.bindPrincipalToSubject(principalId, OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		subject = subject.toUpperCase();
		result = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		// Call under test
		assertFalse(result.isPresent());
	}

}
