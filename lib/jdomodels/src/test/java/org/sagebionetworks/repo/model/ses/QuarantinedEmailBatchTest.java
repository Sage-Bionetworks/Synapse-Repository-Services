package org.sagebionetworks.repo.model.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class QuarantinedEmailBatchTest {
	
	private QuarantinedEmailBatch batch;
	private String email = "testemail@test.com";
	private String messageId = UUID.randomUUID().toString();
	private QuarantineReason reason = QuarantineReason.OTHER;
	
	@BeforeEach
	public void before() {
		batch = new QuarantinedEmailBatch();
	}
	
	@Test
	public void testAddWithoutReason() {
		Assertions.assertThrows(IllegalStateException.class, ()->{ 
			batch.add(email);
		});
	}
	
	@Test
	public void testAddWithReason() {
		
		batch.withReason(reason);
		batch.add(email);

		assertEquals(1, batch.size());
		
		QuarantinedEmail result = batch.iterator().next();
		QuarantinedEmail expected = new QuarantinedEmail(email, reason);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testAddSesMessageId() {
		
		batch.withReason(reason);
		batch.withSesMessageId(messageId);
		batch.add(email);

		assertEquals(1, batch.size());
		
		QuarantinedEmail result = batch.iterator().next();
		QuarantinedEmail expected = new QuarantinedEmail(email, reason).withSesMessageId(messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testAddWithDifferentReasons() {
		
		String otherEmail = System.currentTimeMillis() + email;
		QuarantineReason otherReason = QuarantineReason.PERMANENT_BOUNCE;
		
		batch.withReason(reason);
		batch.add(email);
		
		batch.withReason(otherReason);
		batch.add(otherEmail);
		
		List<QuarantinedEmail> expected = ImmutableList.of(
				new QuarantinedEmail(email, reason),
				new QuarantinedEmail(otherEmail, otherReason)
	    );
		
		assertEquals(expected.size(), batch.size());
		
		for (int i=0; i<expected.size(); i++) {
			assertEquals(expected.get(i), batch.get(i));
		}
	}
	
	@Test
	public void testAddWithExpirationTimeout() {
		Long expirationTimeout = 60*1000L;
		String otherEmail = System.currentTimeMillis() + email;
		
		batch.withReason(reason);
		
		batch.add(email);
		batch.withExpirationTimeout(expirationTimeout);
		batch.add(otherEmail);
		
		assertEquals(expirationTimeout, batch.getExpirationTimeout());
		
	}

	@Test
	public void testAddWithQuarantinedEmail() {
		String otherEmail = System.currentTimeMillis() + email;
		QuarantinedEmail quarantinedEmail = new QuarantinedEmail(otherEmail, reason);
		
		batch.withReason(reason);
		
		batch.add(email);
		batch.add(quarantinedEmail);
		
		List<QuarantinedEmail> expected = ImmutableList.of(
				new QuarantinedEmail(email, reason),
				quarantinedEmail
	    );
		
		assertEquals(expected.size(), batch.size());
		
		for (int i=0; i<expected.size(); i++) {
			assertEquals(expected.get(i), batch.get(i));
		}
		
	}

	
}
