package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class UserStatusDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private UserStatusDao userStatusDao;

	private Long userId;
	private List<Long> createdUsers = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		userId = createUser();
	}

	@AfterEach
	public void tearDown() {
		for (Long id : createdUsers) {
			userGroupDAO.delete(id.toString());
		}
		createdUsers.clear();
	}

	private Long createUser() {
		Long id = userGroupDAO.create(new UserGroup().setIsIndividual(true).setRealmId(AuthorizationConstants.DEFAULT_REALM_ID));
		createdUsers.add(id);
		return id;
	}

	@Test
	public void testGetAndSetLastSeenOn() {
		assertEquals(Optional.empty(), userStatusDao.getLastSeenOn(userId));

		Date lastSeenOn = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

		userStatusDao.setLastSeenOn(List.of(userId), lastSeenOn);

		assertEquals(Optional.of(lastSeenOn), userStatusDao.getLastSeenOn(userId));

		lastSeenOn = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

		userStatusDao.setLastSeenOn(List.of(userId), lastSeenOn);

		assertEquals(Optional.of(lastSeenOn), userStatusDao.getLastSeenOn(userId));
	}

	@Test
	public void testSetLastSeenOnWithMultipleUsers() {
		Long userId2 = createUser();
		Date lastSeenOn = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

		// call under test
		userStatusDao.setLastSeenOn(List.of(userId, userId2), lastSeenOn);

		assertEquals(Optional.of(lastSeenOn), userStatusDao.getLastSeenOn(userId));
		assertEquals(Optional.of(lastSeenOn), userStatusDao.getLastSeenOn(userId2));
	}

	@Test
	public void testGetAndSetDisabled() {
		assertFalse(userStatusDao.isDisabled(userId));

		userStatusDao.setDisabled(userId, true);

		assertTrue(userStatusDao.isDisabled(userId));

		userStatusDao.setDisabled(userId, false);

		assertFalse(userStatusDao.isDisabled(userId));
	}

	@Test
	public void testEnableUser() {
		// Set realistic disabled status: inactive, warned, then disabled
		Date lastSeenOn = Date.from(Instant.now().minus(181, ChronoUnit.DAYS));
		userStatusDao.setLastSeenOn(List.of(userId), lastSeenOn);
		userStatusDao.setDisableWarningSentOn(List.of(userId));
		userStatusDao.setDisabled(userId, true);

		assertTrue(userStatusDao.isDisabled(userId));

		Instant instantNow = Instant.now();

		// call under test
		userStatusDao.enableUser(userId);

		assertFalse(userStatusDao.isDisabled(userId));
		Date updatedLastSeenOn = userStatusDao.getLastSeenOn(userId).orElseThrow();
		assertTrue(updatedLastSeenOn.after(Date.from(instantNow.minus(1, ChronoUnit.MINUTES))),
				"LAST_SEEN_ON should be updated to approximately now");

		// Verify DISABLE_WARNING_SENT_ON was cleared: backdate LAST_SEEN_ON into the warning
		// window — the user should reappear in the warn batch, proving the warning flag was reset
		Date warningThreshold = Date.from(Instant.now().minus(356, ChronoUnit.DAYS));
		Date disableThreshold = Date.from(Instant.now().minus(370, ChronoUnit.DAYS));
		userStatusDao.setLastSeenOn(List.of(userId), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));
		assertEquals(List.of(userId), userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10));
	}

	@Test
	public void testGetInactiveUsersBatch() {
		Long userId2 = createUser();
		Long userId3 = createUser();
		Long userId4 = createUser();
		Date threshold = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));

		// userId: no LAST_SEEN_ON — excluded
		// userId2: recently active — excluded
		userStatusDao.setLastSeenOn(List.of(userId2), Date.from(Instant.now().minus(30, ChronoUnit.DAYS)));
		// userId3: inactive but disabled — excluded
		userStatusDao.setLastSeenOn(List.of(userId3), Date.from(Instant.now().minus(31, ChronoUnit.DAYS)));
		userStatusDao.setDisabled(userId3, true);
		// userId4: inactive and enabled — included
		userStatusDao.setLastSeenOn(List.of(userId4), Date.from(Instant.now().minus(31, ChronoUnit.DAYS)));

		// call under test
		assertEquals(List.of(userId4), userStatusDao.getInactiveUsersBatch(threshold, 10));
	}

	@Test
	public void testGetInactiveUsersBatchRespectsBatchSize() {
		Long userId2 = createUser();
		Long userId3 = createUser();
		Date lastSeenOnThreshold = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));
		Date inactiveDate = Date.from(Instant.now().minus(31, ChronoUnit.DAYS));

		userStatusDao.setLastSeenOn(List.of(userId, userId2, userId3), inactiveDate);

		// call under test: batchSize limits results
		assertEquals(1, userStatusDao.getInactiveUsersBatch(lastSeenOnThreshold, 1).size());

		// call under test: batchSize large enough to return all
		assertEquals(new HashSet<>(List.of(userId, userId2, userId3)),
				new HashSet<>(userStatusDao.getInactiveUsersBatch(lastSeenOnThreshold, 10)));
	}

	@Test
	public void testGetInactiveUsersToWarnBatch() {
		Long userId2 = createUser();
		Long userId3 = createUser();
		Long userId4 = createUser();
		Long userId5 = createUser();
		Long userId6 = createUser();
		Date warningThreshold = Date.from(Instant.now().minus(356, ChronoUnit.DAYS));
		Date disableThreshold = Date.from(Instant.now().minus(370, ChronoUnit.DAYS));

		// userId: no LAST_SEEN_ON — excluded
		// userId2: recently active — excluded
		userStatusDao.setLastSeenOn(List.of(userId2), Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
		// userId3: past the disable threshold — excluded
		userStatusDao.setLastSeenOn(List.of(userId3), Date.from(Instant.now().minus(371, ChronoUnit.DAYS)));
		// userId4: in warn window but disabled — excluded
		userStatusDao.setLastSeenOn(List.of(userId4), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));
		userStatusDao.setDisabled(userId4, true);
		// userId5: in warn window but already warned — excluded
		userStatusDao.setLastSeenOn(List.of(userId5), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));
		userStatusDao.setDisableWarningSentOn(List.of(userId5));
		// userId6: in warn window, enabled, not warned — included
		userStatusDao.setLastSeenOn(List.of(userId6), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));

		// call under test
		assertEquals(List.of(userId6), userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10));

		// after marking userId6 as warned, it should no longer appear
		userStatusDao.setDisableWarningSentOn(List.of(userId6));
		assertTrue(userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10).isEmpty());

		// setLastSeenOn clears DISABLE_WARNING_SENT_ON on userId5: give them recent activity then backdate
		userStatusDao.setLastSeenOn(List.of(userId5), Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
		userStatusDao.setLastSeenOn(List.of(userId5), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));
		// userId5 reappears because the warning flag was cleared
		assertEquals(List.of(userId5), userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10));
	}

	@Test
	public void testGetInactiveUsersToWarnBatchRespectsBatchSize() {
		Long userId2 = createUser();
		Long userId3 = createUser();
		Date warningThreshold = Date.from(Instant.now().minus(356, ChronoUnit.DAYS));
		Date disableThreshold = Date.from(Instant.now().minus(370, ChronoUnit.DAYS));
		Date inactiveDate = Date.from(Instant.now().minus(357, ChronoUnit.DAYS));

		userStatusDao.setLastSeenOn(List.of(userId, userId2, userId3), inactiveDate);

		// call under test: batchSize limits results
		assertEquals(1, userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 1).size());

		// call under test: batchSize large enough to return all
		assertEquals(new HashSet<>(List.of(userId, userId2, userId3)),
				new HashSet<>(userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10)));
	}

	@Test
	public void testEnableUserWithNoExistingRow() {
		// No setLastSeenOn or setDisabled called — no USER_STATUS row exists yet
		Instant instantNow = Instant.now();

		// call under test
		userStatusDao.enableUser(userId);

		assertFalse(userStatusDao.isDisabled(userId));
		Date updatedLastSeenOn = userStatusDao.getLastSeenOn(userId).orElseThrow();
		assertTrue(updatedLastSeenOn.after(Date.from(instantNow.minus(1, ChronoUnit.MINUTES))),
				"LAST_SEEN_ON should be set to approximately now");
	}

	@Test
	public void testSetDisableWarningSentOnWithMultipleUsers() {
		Long userId2 = createUser();
		Date warningThreshold = Date.from(Instant.now().minus(356, ChronoUnit.DAYS));
		Date disableThreshold = Date.from(Instant.now().minus(370, ChronoUnit.DAYS));

		userStatusDao.setLastSeenOn(List.of(userId, userId2), Date.from(Instant.now().minus(357, ChronoUnit.DAYS)));

		assertEquals(new HashSet<>(List.of(userId, userId2)),
				new HashSet<>(userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10)));

		// call under test
		userStatusDao.setDisableWarningSentOn(List.of(userId, userId2));

		assertTrue(userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, 10).isEmpty());
	}
}
