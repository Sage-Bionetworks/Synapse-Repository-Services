package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertFalse;
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
import org.sagebionetworks.repo.model.principal.EmailQuarantineReason;
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
	public void addToQuarantineBatchEmpty() {
		// Call under test
		dao.addToQuarantine(QuarantinedEmailBatch.EMPTY_BATCH);
	}

	@Test
	public void addToQuarantineBatchWithNoExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.add(new QuarantinedEmail(testEmail, EmailQuarantineReason.PERMANENT_BOUNCE));

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		QuarantinedEmail expected = batch.get(0);

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}
	
	@Test
	public void addToQuarantineWithReasonDetails() {
		QuarantinedEmail expected = getTestQuarantinedEmail()
				.withReasonDetails("Some details");
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.add(expected);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}
	
	@Test
	public void addToQuarantineWithoutMessageId() {
		QuarantinedEmail expected = getTestQuarantinedEmail()
				.withSesMessageId(null);
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.add(expected);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}

	@Test
	public void addToQuarantineBatchWithExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout)
				.add(new QuarantinedEmail(testEmail, EmailQuarantineReason.PERMANENT_BOUNCE));

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
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout)
				.add(getTestQuarantinedEmail());

		dao.addToQuarantine(batch);
		
		batch.withExpirationTimeout(null);
		
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
	public void addToQuarantineBatchRefreshingUpdatedOn() throws Exception {

		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout)
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		QuarantinedEmail quarantinedEmail = dao.getQuarantinedEmail(testEmail).get();
		
		Instant createdOn = quarantinedEmail.getCreatedOn();
		Instant updatedOn = quarantinedEmail.getUpdatedOn();
		
		Thread.sleep(100);

		// Call under test
		dao.addToQuarantine(batch);

		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		assertEquals(createdOn, result.getCreatedOn());
		assertTrue(result.getUpdatedOn().isAfter(updatedOn));
	}
	
	@Test
	public void addToQuarantineBatchMultiple() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch();
		
		String email1 = testEmail;
		String email2 = UUID.randomUUID() + testEmail;
		EmailQuarantineReason reason = EmailQuarantineReason.PERMANENT_BOUNCE;

		batch.add(getTestQuarantinedEmail(email1, reason));
		batch.add(getTestQuarantinedEmail(email2, reason));
		
		// Call under test
		dao.addToQuarantine(batch);

		assertTrue(dao.getQuarantinedEmail(email1).isPresent());
		assertTrue(dao.getQuarantinedEmail(email2).isPresent());
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
		String toKeepEmail = System.currentTimeMillis() + testEmail;
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout)
				.add(getTestQuarantinedEmail())
				.add(getTestQuarantinedEmail(toKeepEmail, EmailQuarantineReason.OTHER));
		
		dao.addToQuarantine(batch);

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
	public void testGetQuarantineEmailWithExisting() {

		QuarantinedEmail expected = getTestQuarantinedEmail();
		QuarantinedEmail other = getTestQuarantinedEmail(System.currentTimeMillis() + testEmail, EmailQuarantineReason.OTHER);
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout);

		batch.add(expected);
		batch.add(other);

		dao.addToQuarantine(batch);

		// Call under test
		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();
		
		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getExpiresOn());

		assertEquals(expected, result);

	}
	
	@Test
	public void testGetQuarantineEmailWithExpired() throws Exception {
		Long timeout = 50L;
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(timeout)
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		Thread.sleep(timeout * 2);
		
		// Call under test
		Optional<QuarantinedEmail> result = dao.getQuarantinedEmail(testEmail);

		assertFalse(result.isPresent());
	}
	
	@Test
	public void testGetQuarantineEmailWithNoExpiration() throws Exception {
		QuarantinedEmail expected = getTestQuarantinedEmail();
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		// Call under test
		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail).get();

		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getExpiresOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetQuarantineEmailWithoutExpirationCheck() throws Exception {
		QuarantinedEmail expected = getTestQuarantinedEmail();
		Long timeout = 50L;
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(timeout)
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		Thread.sleep(timeout * 2);
		
		// Call under test
		QuarantinedEmail result = dao.getQuarantinedEmail(testEmail, false).get();
		
		expected.withCreatedOn(result.getCreatedOn());
		expected.withUpdatedOn(result.getUpdatedOn());
		expected.withExpiresOn(result.getExpiresOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testIsQuarantinedWithoutExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		// Call under test
		
		assertTrue(dao.isQuarantined(testEmail));
	}
	
	@Test
	public void testIsQuarantinedWithExpiration() {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(defaultTimeout)
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		// Call under test
		
		assertTrue(dao.isQuarantined(testEmail));
	}
	
	@Test
	public void testIsQuarantinedWithExpirationExpired() throws Exception {
		Long timeout = 50L;
		
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withExpirationTimeout(timeout)
				.add(getTestQuarantinedEmail());
		
		dao.addToQuarantine(batch);
		
		Thread.sleep(timeout * 2);
		
		// Call under test
		
		assertFalse(dao.isQuarantined(testEmail));
	}

	private QuarantinedEmail getTestQuarantinedEmail() {
		return getTestQuarantinedEmail(testEmail, EmailQuarantineReason.PERMANENT_BOUNCE);
	}

	private QuarantinedEmail getTestQuarantinedEmail(String email, EmailQuarantineReason reason) {
		return new QuarantinedEmail(email, reason)
				.withSesMessageId(sesMessageId);
	}

}
