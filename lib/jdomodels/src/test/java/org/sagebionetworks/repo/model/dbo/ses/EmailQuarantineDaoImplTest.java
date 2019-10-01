package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EmailQuarantineDaoImplTest {

	@Autowired
	private EmailQuarantineDao dao;

	private String testEmail = "testemail@test.com";
	private String sesMessageId = UUID.randomUUID().toString();
	private Long defaultTimeout = 60 * 1000L;

	@BeforeEach
	public void before() {
		dao.clearAll();
	}

	@AfterEach
	public void after() {
		dao.clearAll();
	}

	@Test
	public void testAddToQuarantineWithInvalidTimeout() {
		String email = testEmail;
		QuarantineReason reason = QuarantineReason.PERMANENT_BOUNCE;
		Long timeout = 0L;

		QuarantinedEmail quarantinedEmail = new QuarantinedEmail(email, reason);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail, timeout);
		});

		Long negativeTimeout = -1L;

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail, negativeTimeout);
		});
	}

	@Test
	public void testAddToQuarantineWithNoMessageId() {
		QuarantinedEmail expected = getTestQuarantinedEmail();
		expected.withSesMessageId(null);

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, defaultTimeout);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getExpiresOn());

		assertEquals(expected, result);
	}

	@Test
	public void testAddToQuarantineWithExpiration() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, defaultTimeout);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getUpdatedOn().plusMillis(defaultTimeout));

		assertEquals(expected, result);
	}

	@Test
	public void testAddToQuarantineWithNoExpiration() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, null);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}

	@Test
	public void testAddToQuarantineRemovingExpiration() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		QuarantinedEmail result = dao.addToQuarantine(expected, defaultTimeout);

		assertNotNull(result.getExpiresOn());

		// Call under test
		result = dao.addToQuarantine(expected, null);

		assertNull(result.getExpiresOn());
	}

	@Test
	public void testAddToQuarantineAddingExpiration() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, null);

		assertNull(result.getExpiresOn());

		result = dao.addToQuarantine(expected, defaultTimeout);

		assertNotNull(result.getExpiresOn());
	}

	@Test
	public void testAddToQuarantineUpdatingExpiration() throws Exception {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, defaultTimeout);

		Instant expiresOn = result.getExpiresOn();

		Thread.sleep(100);

		result = dao.addToQuarantine(expected, defaultTimeout);

		assertTrue(result.getExpiresOn().isAfter(expiresOn));
	}

	@Test
	public void testAddToQuarantineRefreshUpdatedOn() throws Exception {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected, defaultTimeout);

		Instant createdOn = result.getCreatedOn();
		Instant updatedOn = result.getUpdatedOn();

		Thread.sleep(100);

		result = dao.addToQuarantine(expected, defaultTimeout);

		assertEquals(createdOn, result.getCreatedOn());
		assertTrue(result.getUpdatedOn().isAfter(updatedOn));
	}

	@Test
	public void addToQuarantineBatchEmpty() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch().withReason(QuarantineReason.PERMANENT_BOUNCE);

		// Call under test
		dao.addToQuarantine(batch);
	}

	@Test
	public void addToQuarantineBatchWithNoExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch().withReason(QuarantineReason.PERMANENT_BOUNCE);

		batch.add(testEmail);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		QuarantinedEmail expected = batch.get(0);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}

	@Test
	public void addToQuarantineBatchWithExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch().withReason(QuarantineReason.PERMANENT_BOUNCE)
				.withExpirationTimeout(defaultTimeout);

		batch.add(testEmail);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		QuarantinedEmail expected = batch.get(0);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getUpdatedOn().plusMillis(defaultTimeout));

		assertEquals(expected, result);
	}

	@Test
	public void addToQuarantineBatchRemovingExpiration() {

		dao.addToQuarantine(getTestQuarantinedEmail(), defaultTimeout);

		QuarantinedEmailBatch batch = new QuarantinedEmailBatch().withReason(QuarantineReason.PERMANENT_BOUNCE).withExpirationTimeout(null);

		batch.add(testEmail);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		QuarantinedEmail expected = batch.get(0);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(null);

		assertEquals(expected, result);
	}

	@Test
	public void testRemoveFromQuarantineWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = null;
			// Call under test
			dao.removeFromQuarantine(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "";
			// Call under test
			dao.removeFromQuarantine(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "    ";
			// Call under test
			dao.removeFromQuarantine(testEmail);
		});
	}

	@Test
	public void testRemoveFromQuarantineWithNonExisting() {

		// Call under test
		boolean result = dao.removeFromQuarantine(testEmail);

		assertFalse(result);
		assertFalse(dao.getQuarantinedEmail(testEmail).isPresent());
	}

	@Test
	public void testRemoveFromQuarantineWithExisting() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		dao.addToQuarantine(expected, defaultTimeout);

		String toKeepEmail = System.currentTimeMillis() + testEmail;

		QuarantinedEmail toKeep = getTestQuarantinedEmail(toKeepEmail, QuarantineReason.OTHER);

		dao.addToQuarantine(toKeep, defaultTimeout);

		// Call under test
		boolean result = dao.removeFromQuarantine(testEmail);

		assertTrue(result);
		assertFalse(dao.getQuarantinedEmail(testEmail).isPresent());
		assertTrue(dao.getQuarantinedEmail(toKeepEmail).isPresent());
	}

	@Test
	public void testGetQuarantinedEmailWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = null;
			// Call under test
			dao.getQuarantinedEmail(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "";
			// Call under test
			dao.getQuarantinedEmail(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "    ";
			// Call under test
			dao.getQuarantinedEmail(testEmail);
		});
	}

	@Test
	public void testGetQuarantineEmailWithNonExisting() {

		QuarantinedEmail other = getTestQuarantinedEmail(System.currentTimeMillis() + testEmail, QuarantineReason.OTHER);

		dao.addToQuarantine(other, defaultTimeout);

		// Call under test
		Optional<QuarantinedEmail> result = dao.getQuarantinedEmail(testEmail);

		assertFalse(result.isPresent());

	}

	@Test
	public void testGetQuarantineEmailWithExisting() {

		QuarantinedEmail expected = getTestQuarantinedEmail();
		QuarantinedEmail other = getTestQuarantinedEmail(System.currentTimeMillis() + testEmail, QuarantineReason.OTHER);

		dao.addToQuarantine(other, defaultTimeout);

		expected = dao.addToQuarantine(expected, defaultTimeout);

		// Call under test
		Optional<QuarantinedEmail> result = dao.getQuarantinedEmail(testEmail);

		assertEquals(expected, result.get());

	}

	private QuarantinedEmail getTestQuarantinedEmail() {
		return getTestQuarantinedEmail(testEmail, QuarantineReason.PERMANENT_BOUNCE);
	}

	private QuarantinedEmail getTestQuarantinedEmail(String email, QuarantineReason reason) {
		return new QuarantinedEmail(email, reason).withSesMessageId(sesMessageId);
	}

}
