package org.sagebionetworks.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@RunWith(ParallelizedSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TestParallelizedSpringJUnit4ClassRunner {

	@Autowired
	private AmazonErrorCodes errorCodes;

	private static AtomicInteger steps = new AtomicInteger(0);
	private static volatile boolean beforeAllDone = false;

	@BeforeClass
	public static void beforeClass() {
		assertEquals(1, steps.incrementAndGet());
	}

	@BeforeAll
	public void beforeAll() throws Exception {
		assertNotNull("autowired didn't happen", errorCodes);
		assertEquals(2, steps.incrementAndGet());
		// give test threads a chance to start if they were alive
		Thread.sleep(40);
		assertEquals(3, steps.incrementAndGet());
		beforeAllDone = true;
		System.err.println("beforeAll" + Thread.currentThread().getId());
	}

	@AfterAll
	public void afterAll() throws Exception {
		System.err.println("afterAll" + Thread.currentThread().getId());
		assertEquals(4 + 3 * 3, steps.incrementAndGet());
	}

	@AfterClass
	public static void afterClass() {
		System.err.println("afterClass" + Thread.currentThread().getId());
		assertEquals(5 + 3 * 3, steps.incrementAndGet());
	}

	@Before
	public void before() {
		assertTrue(beforeAllDone);
		steps.incrementAndGet();
		System.err.println("before" + Thread.currentThread().getId());
	}

	@After
	public void after() {
		steps.incrementAndGet();
		System.err.println("after" + Thread.currentThread().getId());
	}

	@Test
	public void test1() throws Exception {
		test(10);
	}

	@Test
	public void test2() throws Exception {
		test(20);
	}

	@Test
	public void test3() throws Exception {
		test(40);
	}

	public void test(int sleeptime) throws Exception {
		System.err.println("test" + Thread.currentThread().getId());
		assertTrue(beforeAllDone);
		Thread.sleep(sleeptime);
		steps.incrementAndGet();
	}
}
