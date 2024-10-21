package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTermsOfServiceAgreementMigrationDaoTest {
	
	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOTermsOfServiceAgreementMigrationDao dao;
	
	@Autowired
	private JdbcTemplate migrationJdbcTemplate;

	private Long userId;
	
	@BeforeEach
	public void before() {
		authDAO.clearTermsOfServiceData();
		userGroupDAO.truncateAll();
		userId = userGroupDAO.create(new UserGroup().setIsIndividual(true));
	}
	
	@AfterEach
	public void after() {
		authDAO.clearTermsOfServiceData();
		userGroupDAO.truncateAll();
	}
	
	@Test
	public void testGetUsersWithoutAgreement() {
		Long userWithAgreement = userGroupDAO.create(new UserGroup().setIsIndividual(true));
			
		Instant now = Instant.now();
		
		authDAO.addTermsOfServiceAgreement(userWithAgreement, "0.0.0", Date.from(now.minus(30, ChronoUnit.DAYS)));
		authDAO.addTermsOfServiceAgreement(userWithAgreement, "1.0.0", Date.from(now.minus(1, ChronoUnit.DAYS)));
		
		List<Long> userIds = List.of(userId, userWithAgreement);
		
		List<UserGroup> expected = List.of(
			new UserGroup().setId(userId.toString())
		);
		
		assertEquals(expected, dao.getUsersWithoutAgreement(migrationJdbcTemplate, userIds));		
	}
	
	@Test
	public void testBatchAddTermsOfServiceAgreement() {
		Long anotherUserId = userGroupDAO.create(new UserGroup().setIsIndividual(true));
			
		Instant now = Instant.now();
		
		TermsOfServiceAgreement one = new TermsOfServiceAgreement().setUserId(userId).setAgreedOn(Date.from(now.minus(1, ChronoUnit.DAYS))).setVersion("1.0.0");
		TermsOfServiceAgreement two = new TermsOfServiceAgreement().setUserId(anotherUserId).setAgreedOn(Date.from(now.minus(30, ChronoUnit.DAYS))).setVersion("0.0.0");
		
		dao.batchAddTermsOfServiceAgreement(migrationJdbcTemplate, List.of(one, two));
		
		assertEquals(Optional.of(one), authDAO.getLatestTermsOfServiceAgreement(userId));
		assertEquals(Optional.of(two), authDAO.getLatestTermsOfServiceAgreement(anotherUserId));
	}
	

}
