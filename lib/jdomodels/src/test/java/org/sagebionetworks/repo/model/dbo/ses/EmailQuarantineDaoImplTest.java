package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ses.QuarantineReason;
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

	@BeforeEach
	public void before() {
		dao.clearAll();
	}
	
	@AfterEach
	public void after() {
		dao.clearAll();
	}
	
	@Test
	public void testAddToQuarantineWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = null;
			QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
			// Call under test
			dao.addToQuarantine(testEmail, reason, sesMessageId);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "  ";
			QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
			// Call under test
			dao.addToQuarantine(testEmail, reason, sesMessageId);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			QuarantineReason reason = null;
			// Call under test
			dao.addToQuarantine(testEmail, reason, sesMessageId);
		});
	}
	
	@Test
	public void testAddToQuarantineWithNonExisting() {
		QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
		
		// Call under test
		dao.addToQuarantine(testEmail, reason, sesMessageId);
		
		assertTrue(dao.isQuarantined(testEmail));
		assertEquals(reason, dao.getQuarantineReason(testEmail).get());
	}
	
	@Test
	public void testAddToQuarantineWithNoMessageId() {
		QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
		String sesMessageId = null;
		
		// Call under test
		dao.addToQuarantine(testEmail, reason, sesMessageId);
		
		assertTrue(dao.isQuarantined(testEmail));
		assertEquals(reason, dao.getQuarantineReason(testEmail).get());
	}
	
	@Test
	public void testAddToQuarantineWithExisting() {
		
		dao.addToQuarantine(testEmail, QuarantineReason.TOO_MANY_BOUNCES, sesMessageId);
		
		QuarantineReason updatedReason = QuarantineReason.HARD_BOUNCE;
		
		// Call under test
		dao.addToQuarantine(testEmail, updatedReason, null);
		
		assertTrue(dao.isQuarantined(testEmail));
		assertEquals(updatedReason, dao.getQuarantineReason(testEmail).get());
		
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
		assertFalse(dao.isQuarantined(testEmail));
	}
	
	@Test
	public void testRemoveFromQuarantineWithExisting() {
		QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
		
		dao.addToQuarantine(testEmail, QuarantineReason.HARD_BOUNCE, sesMessageId);
		
		String toKeepEmail = System.currentTimeMillis() + testEmail; 
		dao.addToQuarantine(toKeepEmail, reason, sesMessageId);
		
		// Call under test
		boolean result = dao.removeFromQuarantine(testEmail);
		
		assertTrue(result);
		assertFalse(dao.isQuarantined(testEmail));
		assertTrue(dao.isQuarantined(toKeepEmail));
	}
	
	@Test
	public void testIsQuarantinedWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = null;
			// Call under test
			dao.isQuarantined(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "";
			// Call under test
			dao.isQuarantined(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "    ";
			// Call under test
			dao.isQuarantined(testEmail);
		});
	}
	
	@Test
	public void testIsQuarantinedWithNonExisting() {
		
		String otherEmail = System.currentTimeMillis() + testEmail;
		
		dao.addToQuarantine(otherEmail, QuarantineReason.HARD_BOUNCE, sesMessageId);
		
		// Call under test
		boolean result = dao.isQuarantined(testEmail);
		
		assertFalse(result);
		assertTrue(dao.isQuarantined(otherEmail));
	}
	
	@Test
	public void testIsQuarantinedWithExisting() {
		
		dao.addToQuarantine(testEmail, QuarantineReason.HARD_BOUNCE, sesMessageId);
		
		// Call under test
		boolean result = dao.isQuarantined(testEmail);
		
		assertTrue(result);
		assertTrue(dao.getQuarantineReason(testEmail).isPresent());
	}
	
	@Test
	public void testGetQuarantinedReasonWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = null;
			// Call under test
			dao.getQuarantineReason(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "";
			// Call under test
			dao.getQuarantineReason(testEmail);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String testEmail = "    ";
			// Call under test
			dao.getQuarantineReason(testEmail);
		});
	}
	
	@Test
	public void testGetQuarantineReasonWithNonExisting() {
		
		String otherEmail = System.currentTimeMillis() + testEmail;
		
		dao.addToQuarantine(otherEmail, QuarantineReason.HARD_BOUNCE, sesMessageId);

		// Call under test
		Optional<QuarantineReason> result = dao.getQuarantineReason(testEmail);
		
		assertFalse(result.isPresent());
		
	}
	
	@Test
	public void testGetQuarantineReasonWithExisting() {
		
		QuarantineReason reason = QuarantineReason.HARD_BOUNCE;
		String otherEmail = System.currentTimeMillis() + testEmail;
		
		dao.addToQuarantine(otherEmail, reason, sesMessageId);
		dao.addToQuarantine(testEmail, reason, sesMessageId);
		
		// Call under test
		Optional<QuarantineReason> result = dao.getQuarantineReason(testEmail);
		
		assertEquals(reason, result.get());
		
	}
	
	
	
}
