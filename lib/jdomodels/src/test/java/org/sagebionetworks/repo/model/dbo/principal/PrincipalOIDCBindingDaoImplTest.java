package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalOIDCBindingDaoImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private PrincipalAliasDAO aliasDao;
	
	@Autowired
	private PrincipalOIDCBindingDao dao;
	
	private PrincipalAlias alias;
	
	@BeforeEach
	public void before() {
		dao.truncateAll();
		Long principalId = userGroupDao.create(new UserGroup().setCreationDate(new Date()).setIsIndividual(true));
		alias = aliasDao.bindAliasToPrincipal(new PrincipalAlias().setAlias(UUID.randomUUID().toString() + "@gmail.com").setType(AliasType.USER_EMAIL).setPrincipalId(principalId));
	}

	@AfterEach
	public void after() {
		userGroupDao.delete(alias.getPrincipalId().toString());
		dao.truncateAll();
	}
	
	@Test
	public void testBindPrincipalToSubjectAndFind() {
		
		String subject = "subject";
		
		// Call under test
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		PrincipalOidcBinding binding = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).get();
		
		assertEquals(alias.getPrincipalId(), binding.getUserId());
		assertEquals(alias.getAliasId(), binding.getAliasId());
	}
	
	@Test
	public void testBindPrincipalToSubjectWithExisting() {
		
		String subject = "subject";
		
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		// Call under test, should be a noop
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
	}
		
	@Test
	public void testFindBindingForSubjectWithNoMatchCase() {
		
		String subject = "subject";
		
		// Call under test
		Optional<PrincipalOidcBinding> result = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		assertFalse(result.isPresent());
		
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		subject = subject.toUpperCase();
		result = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		// Call under test
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testDeleteBinding() {
		
		String subject = "subject";
		
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		PrincipalOidcBinding binding = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).get();
		
		// Call under test
		dao.deleteBinding(binding.getBindingId());
		
		assertFalse(dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).isPresent());
	}
	
	@Test
	public void testSetBindingAlias() {
		String subject = "subject";
		
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
		
		PrincipalOidcBinding binding = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).get();
		
		Long newAliasId = aliasDao.bindAliasToPrincipal(new PrincipalAlias().setAlias(UUID.randomUUID().toString()).setType(AliasType.USER_NAME).setPrincipalId(alias.getPrincipalId())).getAliasId();
		
		// Call under test
		dao.setBindingAlias(binding.getBindingId(), newAliasId);
		
		binding = dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).get();
		
		assertEquals(newAliasId, binding.getAliasId());
	}
	
	@Test
	public void testClearBindings() {
		
		String subject = "subject";
		
		dao.bindPrincipalToSubject(alias.getPrincipalId(), alias.getAliasId(), OAuthProvider.GOOGLE_OAUTH_2_0, subject);
				
		// Call under test
		dao.clearBindings(alias.getPrincipalId());
		
		assertFalse(dao.findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, subject).isPresent());
	}

}
