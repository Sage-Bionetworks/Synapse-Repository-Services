package org.sagebionetworks.repo.util;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A fixed pool of memory that can be shared by multiple threads.
 * 
 * All memory is stored with SoftReferences so if not used, it can be garbage collected.
 * However, memory that is checked out is strongly referenced and cannot be garbage collected.
 * 
 * This object is thread-safe.
 * 
 * Note: This is a non-blocking pool.  Meaning if a block of memory cannot be checked-out it fails rather than blocking
 * the caller until memory is available.
 * 
 * @author John
 *
 */
public class FixedMemoryPool {
	
	private volatile int maxNumberOfBlocks;
	private volatile long maxMemoryBytes = -1;
	private volatile long blockSizeBytes = -1;
	private volatile int currentBlockCount;
	private volatile int allocatedBlocks;
	/**
	 * Note: This pool does not need to thread safe as it is only access within a synchronized methods
	 * of this class.
	 * The pool queue holds soft references to each block.  This allows the blocks to be garbage collected if needed.
	 * @See <a href="http://docs.oracle.com/javase/6/docs/api/java/lang/ref/SoftReference.html">SoftReference</a>
	 */
	private Queue<SoftReference<byte[]>> pool;
	
	/**
	 * If this constructor is used then you must set blockSizeBytes and maxMemoryBytes and then call the initialize() method.
	 */
	public FixedMemoryPool(){
	}

	public void setMaxMemoryBytes(long maxMemoryBytes) {
		this.maxMemoryBytes = maxMemoryBytes;
	}

	public void setBlockSizeBytes(long blockSizeBytes) {
		this.blockSizeBytes =  blockSizeBytes;
	}

	/**
	 * Create a new thread-safe FixedMemoryPool.
	 * 
	 * @param maxMemoryBytes - The maximum amount of memory used by the pool in bytes.
	 * @param blockSizeByes - The size of each block in bytes.  The block size is fixed.
	 */
	public FixedMemoryPool(long maxMemoryBytes, long blockSizeByes){
		if(blockSizeByes > maxMemoryBytes) throw new IllegalArgumentException("The blockSizeByes cannot be larger than maxMemoryBytes");
		this.maxMemoryBytes = (int) maxMemoryBytes;
		this.blockSizeBytes = (int) blockSizeByes;
		initialize();
	}

	/**
	 * This must be called if the default non-argument constructor is used.
	 */
	public void initialize() {
		if(maxMemoryBytes < 0) throw new IllegalStateException("maxMemoryBytes must be set");
		if(blockSizeBytes < 0) throw new IllegalStateException("blockSizeBytes must be set");
		// The pool size is a function of the max memory and the block size.
		this.maxNumberOfBlocks = (int) (this.maxMemoryBytes/this.blockSizeBytes);
		// Note: This pool does not need to thread safe as it is only access within a synchronized methods.
		pool = new LinkedList<SoftReference<byte[]>>();
		// We start with an empty pool
		this.currentBlockCount = 0;
		this.allocatedBlocks = 0;
	}
	
	/**
	 * Check-out a block from the pool. 
	 * 
	 * @return This will return null when the maximum number of blocks have been checked-out.
	 * The caller is responsible for checking-in a block when finished:
	 */
	protected synchronized byte[] checkoutBlock(){
		byte[] block = null;
		// First try to get a block from the pool
		SoftReference<byte[]> softRef = pool.poll();
		if(softRef == null){
			// the pool is empty.
			// if we are still under the max pool size then we can allocate more memory
			if(currentBlockCount < maxNumberOfBlocks){
				// Increment the block count and allocate more memory
				currentBlockCount++;
				block = new byte[(int) blockSizeBytes];
			}
		}else{
			// We had a soft reference in the pool is it still allocated?
			block = softRef.get();
			if(block == null){
				// a previously allocated block has been garbage collected.
				// Since we allocated it before we should still be within range
				block = new byte[(int) blockSizeBytes];
			}
		}
		// return the block if we were able to allocate one.
		if(block != null){
			allocatedBlocks++;
		}
		return block;
	}
	
	/**
	 * Check-in a block when finished with it.
	 * 
	 * @param block
	 */
	protected synchronized void checkinBlock(byte[] block){
		if(block == null) throw new IllegalArgumentException("Cannot check-in a null block");
		if(block.length != blockSizeBytes) throw new IllegalArgumentException("Block size did not match expected sizes");
		// Put this back into the pool
		pool.add(new SoftReference<byte[]>(block));
		allocatedBlocks--;
	}
	
	/**
	 * This is the public API for checking out and using a block.
	 * An attempt will be made to check-out a block from the pool.  When check-out is successful,
	 * the block will be passed to the consumer via consumer.useBlock().  After the consumer terminates
	 * either normally or with Exception, the checked-out block will be returned to the pool.
	 * When check-out fails, a NoBlocksAvailableException will be immediately thrown.
	 * 
	 * Note: This method "thread-safe" and designed to be called by concurrent threads. This method does not block,
	 * if resources cannot be allocated.  Instead it is designed to fail-fast.
	 * 
	 * @param consumer
	 * @throws NoBlocksAvailableException - This exception is thrown when it fails to checkout a block from the pool
	 * 
	 * Important: DO NOT MAKE THIS METHODS SYNCHRONIZED! Doing so would change it to blocking rather than fail-fast.
	 */
	public <T> T checkoutAndUseBlock(BlockConsumer<T> consumer) throws Exception {
		if(consumer == null) throw new IllegalArgumentException("Consumer cannot be null");
		byte[] block = null;
		try{
			// Checkout the block.
			block = checkoutBlock();
			if(block == null) throw new NoBlocksAvailableException();
			// Use the block
			return consumer.useBlock(block);
		}finally{
			// Check-in the block.
			if(block != null){
				checkinBlock(block);
			}
		}
	}
	
	/**
	 * 
	 * Abstraction for anything that consumes the blocks from the pool.
	 *
	 */
	public static interface BlockConsumer <T> {
		
		/**
		 * If a block is successfully checked-out from the pool it will be passed to
		 * consumer.  The block will be automatically be checked back into the pool when
		 * when this method terminates either normally or with an exception.
		 *  
		 * @param block
		 */
		public T useBlock(byte[] block) throws Exception;
	}
	
	/**
	 * 
	 * Thrown when there are no more NoBlocksAvailable in the pool.
	 *
	 */
	public static class NoBlocksAvailableException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4848069926591312855L;

		public NoBlocksAvailableException() {
			super();
		}

		public NoBlocksAvailableException(String message, Throwable cause) {
			super(message, cause);
		}

		public NoBlocksAvailableException(String message) {
			super(message);
		}

		public NoBlocksAvailableException(Throwable cause) {
			super(cause);
		}
		
	}

	/**
	 * The maximum number of blocks in the pool.
	 * This value will always be maxMemoryBytes/blockSizeBytes;
	 * @return
	 */
	public int getMaxNumberOfBlocks() {
		return maxNumberOfBlocks;
	}

	/**
	 * This is the maximum amount of memory that this pool will attempt to use in bytes.
	 * @return
	 */
	public long getMaxMemoryBytes() {
		return maxMemoryBytes;
	}

	/**
	 * The fixed size of each block in the pool in bytes.
	 * @return
	 */
	public long getBlockSizeBytes() {
		return blockSizeBytes;
	}

	/**
	 * The current number of blocks allocated by the pool.  
	 * This will always be less than or equal to maxPoolSize.
	 * @return
	 */
	public int getCurrentBlockCount() {
		return currentBlockCount;
	}
	
	/**
	 * The number of blocks currently allocated.  This is provided for informational use only.
	 * To avoid race conditions, do not use this method for business decisions.
	 * 
	 * @return
	 */
	public int getAlocatedBlocks(){
		return this.allocatedBlocks;
	}
	

}
