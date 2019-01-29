package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BannedPasswordsAutowiredTest {
	@Autowired
	BannedPasswords bannedPasswords;

	@Test(expected = IllegalArgumentException.class)
	public void testIsPasswordBanned_nullPassword(){
		bannedPasswords.isPasswordBanned(null);
	}

	@Test
	public void testIsPasswordBanned(){
		//test for some passwords that definitely should be in the banned password set (or it would be a terribly curated set)
		assertTrue(bannedPasswords.isPasswordBanned("password"));
		assertTrue(bannedPasswords.isPasswordBanned("12345678"));
	}

	@Test
	public void testIsPasswordBanned_caseInsensitive(){
		assertTrue(bannedPasswords.isPasswordBanned("password"));
		assertTrue(bannedPasswords.isPasswordBanned("PaSsWoRd"));
	}

}
