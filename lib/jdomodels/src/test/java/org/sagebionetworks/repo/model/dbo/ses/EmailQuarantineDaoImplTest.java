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
import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
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
	public void testAddToQuarantineWithNullEmail() {
		QuarantinedEmail quarantinedEmail = getTestQuarantinedEmail();
		quarantinedEmail.setEmail(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail);
		});
	}

	@Test
	public void testAddToQuarantineWithEmptyEmail() {
		QuarantinedEmail quarantinedEmail = getTestQuarantinedEmail();
		quarantinedEmail.setEmail("    ");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail);
		});
	}

	@Test
	public void testAddToQuarantineWithNullReason() {

		QuarantinedEmail quarantinedEmail = getTestQuarantinedEmail();
		quarantinedEmail.setReason(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail);
		});
	}

	@Test
	public void testAddToQuarantineWithPastTimeout() {

		QuarantinedEmail quarantinedEmail = getTestQuarantinedEmail();
		quarantinedEmail.setTimeout(Instant.now().minusMillis(defaultTimeout));

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.addToQuarantine(quarantinedEmail);
		});
	}

	@Test
	public void testAddToQuarantineWithNoMessageId() {
		QuarantinedEmail expected = getTestQuarantinedEmail();
		expected.setSesMessageId(null);

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected);

		expected.setCreatedOn(result.getCreatedOn());
		expected.setUpdatedOn(result.getUpdatedOn());

		assertEquals(expected, result);
	}

	@Test
	public void testAddToQuarantineWithExisting() {
		QuarantinedEmail expected = getTestQuarantinedEmail();

		dao.addToQuarantine(expected);

		QuarantineReason updatedReason = QuarantineReason.OTHER;
		expected.setReason(updatedReason);
		expected.setTimeout(null);

		// Call under test
		QuarantinedEmail result = dao.addToQuarantine(expected);

		expected.setCreatedOn(result.getCreatedOn());
		expected.setUpdatedOn(result.getUpdatedOn());

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

		dao.addToQuarantine(expected);

		String toKeepEmail = System.currentTimeMillis() + testEmail;

		QuarantinedEmail toKeep = getTestQuarantinedEmail();
		toKeep.setEmail(toKeepEmail);

		dao.addToQuarantine(toKeep);

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

		QuarantinedEmail other = getTestQuarantinedEmail();
		other.setEmail(System.currentTimeMillis() + testEmail);

		dao.addToQuarantine(other);

		// Call under test
		Optional<QuarantinedEmail> result = dao.getQuarantinedEmail(testEmail);

		assertFalse(result.isPresent());

	}

	@Test
	public void testGetQuarantineEmailWithExisting() {

		QuarantinedEmail expected = getTestQuarantinedEmail();
		QuarantinedEmail other = getTestQuarantinedEmail();
		other.setEmail(System.currentTimeMillis() + testEmail);

		dao.addToQuarantine(other);

		expected = dao.addToQuarantine(expected);

		// Call under test
		Optional<QuarantinedEmail> result = dao.getQuarantinedEmail(testEmail);

		assertEquals(expected, result.get());

	}

	QuarantinedEmail getTestQuarantinedEmail() {
		QuarantinedEmail quarantinedEmail = new QuarantinedEmail();

		quarantinedEmail.setEmail(testEmail);
		quarantinedEmail.setReason(QuarantineReason.HARD_BOUNCE);
		quarantinedEmail.setSesMessageId(sesMessageId);
		quarantinedEmail.setTimeout(Instant.now().plusMillis(defaultTimeout));

		return quarantinedEmail;
	}

}
