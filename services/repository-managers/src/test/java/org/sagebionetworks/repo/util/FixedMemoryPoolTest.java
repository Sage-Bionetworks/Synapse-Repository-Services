package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.util.FixedMemoryPool.BlockConsumer;
import org.sagebionetworks.repo.util.FixedMemoryPool.NoBlocksAvailableException;
import org.sagebionetworks.util.ThreadTestUtils;

import com.amazonaws.util.StringInputStream;

/**
 * Test for the FixedMemoryPool.
 * 
 * @author John
 *
 */
public class FixedMemoryPoolTest {

	@Before
	public void before() {
		ThreadTestUtils.doBefore();
	}

	@After
	public void after() {
		ThreadTestUtils.doAfter();
	}

	@Test
	public void testConstructor(){
		int maxMemoryBytes = 101;
		int blockSize = 13;
		int maxNumberBlocks = maxMemoryBytes/blockSize;
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		assertEquals(blockSize, pool.getBlockSizeBytes());
		assertEquals(maxMemoryBytes, pool.getMaxMemoryBytes());
		assertEquals(maxNumberBlocks, pool.getMaxNumberOfBlocks());
		// the pool should start out empty.
		assertEquals(0, pool.getCurrentBlockCount());
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testConstructorSwappedArgs(){
		int maxMemoryBytes = 20;
		int blockSize = 10;
		FixedMemoryPool pool = new FixedMemoryPool(blockSize, maxMemoryBytes);
		pool.checkinBlock(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullCheckin(){
		int maxMemoryBytes = 20;
		int blockSize = 10;
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		pool.checkinBlock(null);
	}
	@Test (expected=IllegalArgumentException.class)
	public void testInvalidCheckin(){
		int maxMemoryBytes = 20;
		int blockSize = 10;
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		// Incorrect block size.
		pool.checkinBlock(new byte[1]);
	}
	
	
	@Test
	public void testCheckoutCheckin(){
		int maxMemoryBytes = 20;
		int blockSize = 10;
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		// the pool should start out empty.
		assertEquals(0, pool.getCurrentBlockCount());
		byte[] block = pool.checkoutBlock();
		assertNotNull(block);
		assertEquals(blockSize, block.length);
		assertEquals(1, pool.getCurrentBlockCount());
		// checkout again
		byte[] block2 = pool.checkoutBlock();
		assertNotNull(block2);
		assertEquals(blockSize, block.length);
		assertEquals(2, pool.getCurrentBlockCount());
		// The third checkout should return null as we have exceeded the pool size
		byte[] block3 = pool.checkoutBlock();
		assertTrue(block3 == null);
		// Now check-in the second block
		pool.checkinBlock(block2);
		assertEquals(2, pool.getCurrentBlockCount());
		// Try checking out again.
		block3 = pool.checkoutBlock();
		assertNotNull(block3);
		assertEquals(2, pool.getCurrentBlockCount());
	}
	
	/**
	 * When an exception is thrown the block must be checked back into the pool.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCheckoutAndUseBlockException() throws Exception{
		final byte b = 123;
		int maxMemoryBytes = 10;
		int blockSize = 6;
		byte[] expectedBlock = new byte[blockSize];
		Arrays.fill(expectedBlock, b);
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		try{
			// Throw an exception after adding some data to the block.
			pool.checkoutAndUseBlock(new BlockConsumer<String>(){
				@Override
				public String useBlock(byte[] block) throws Exception {
					assertNotNull(block);
					// put the data in the block
					Arrays.fill(block, b);
					// Now throw an exception.
					throw new IllegalStateException("testsing");
				}});
			fail("An exception should have been thrown.");
		}catch(IllegalStateException e){
			// this exception is expected.
		}
		// Now validate that the block was put back in the pool after the exception
		byte[] block = pool.checkoutBlock();
		assertNotNull(block);
		assertTrue(Arrays.equals(expectedBlock, block));
	}
	
	@Test
	public void testCheckoutAndUseBlockRecycle() throws Exception{
		final String sampleData = "firstCall";
		int maxMemoryBytes = 20;
		int blockSize = sampleData.getBytes().length;
		FixedMemoryPool pool = new FixedMemoryPool(maxMemoryBytes, blockSize);
		// Checkout from the pool.
		String result = pool.checkoutAndUseBlock(new BlockConsumer<String>(){
			@Override
			public String useBlock(byte[] block) throws Exception {
				// Fill the block with data.
				StringInputStream in = new StringInputStream(sampleData);
				in.read(block);
				in.close();
				return sampleData;
			}});
		assertEquals(sampleData, result);
		// The pool should have 1 blocks
		assertEquals(1, pool.getCurrentBlockCount());
		// This time copy the data out of the block and return it.
		String result2 = pool.checkoutAndUseBlock(new BlockConsumer<String>(){
			@Override
			public String useBlock(byte[] block) throws Exception {
				// Fill the block with data.
				return new String(block);
			}});
		assertEquals(sampleData, result2);
	}
	
	/**
	 * This is an important test at it validates that FixedMemoryPool.checkoutAndUseBlock() does not block.
	 * @throws Exception 
	 * 
	 */
	@Test
	public void testBlocking() throws Exception{
		// Setup a pool with two blocks of memory
		final FixedMemoryPool pool = new FixedMemoryPool(20, 10);

		final CountDownLatch waitForA = new CountDownLatch(1);
		final CountDownLatch waitForB = new CountDownLatch(1);
		final CountDownLatch waitReleaseA = new CountDownLatch(1);
		final CountDownLatch waitReleaseB = new CountDownLatch(1);
		// Setup the threads
		Thread threadA = new Thread() {
			@Override
			public void run() {
				// This thread runs consumer a
				try {
					pool.checkoutAndUseBlock(new BlockConsumer<Long>() {
						@Override
						public Long useBlock(byte[] block) throws Exception {
							waitForA.countDown();
							waitReleaseA.await(20, TimeUnit.SECONDS);
							return null;
						}
					});
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		};
		Thread threadB = new Thread() {
			@Override
			public void run() {
				// This thread runs consumer a
				try {
					pool.checkoutAndUseBlock(new BlockConsumer<Long>() {
						@Override
						public Long useBlock(byte[] block) throws Exception {
							waitForB.countDown();
							waitReleaseB.await(20, TimeUnit.SECONDS);
							return null;
						}
					});
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		};
		// The setup is done we are not ready to start the test.
		// First start thread A
		threadA.start();
		waitForA.await(20, TimeUnit.SECONDS);
		// At this point one block should be allocated
		assertEquals("With one thread running one block of memory should be allocated.", 1, pool.getAlocatedBlocks());
		// Star the next thead
		threadB.start();
		waitForB.await(20, TimeUnit.SECONDS);
		assertFalse("FixedMemoryPool.checkoutAndUseBlock() is blocking if only one block is in use at this point!  FixedMemoryPool.checkoutAndUseBlock() must not be synchronized!!!!!!", pool.getAlocatedBlocks() == 1);
		assertEquals("With two thread running two block of memory should be allocated.", 2, pool.getAlocatedBlocks());
		// Now try to allocate another block that is beyond the max
		try{
			pool.checkoutAndUseBlock(new BlockConsumer<Long>() {
				@Override
				public Long useBlock(byte[] block) throws Exception {
					return null;
				}
			});
			fail("All blocks are checked-out so this should have failed");
		}catch(NoBlocksAvailableException e){
			// This is expected.
		}
		waitReleaseA.countDown();
		threadA.join(20 * 1000);
		assertEquals("With one thread running one block of memory should be allocated.", 1, pool.getAlocatedBlocks());
		// release the second consumer
		waitReleaseB.countDown();
		threadB.join(20 * 1000);
		assertEquals("With zero thread running no blocks of memory should be allocated.", 0, pool.getAlocatedBlocks());
	}

}
