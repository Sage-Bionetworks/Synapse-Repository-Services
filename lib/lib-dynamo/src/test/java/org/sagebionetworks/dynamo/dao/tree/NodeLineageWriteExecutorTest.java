package org.sagebionetworks.dynamo.dao.tree;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.DynamoWriteExecution;
import org.sagebionetworks.dynamo.DynamoWriteOperation;

public class NodeLineageWriteExecutorTest {

	private final MockOp op1 = new MockOp("a", "d", 1, 3, false);
	private final MockOp op2 = new MockOp("b", "e", 1, 3, false);
	private final MockOp op3 = new MockOp("b", "f", 1, 4, false);
	private final MockOp op4 = new MockOp("b", "g", 1, 4, false);
	private final MockOp op5 = new MockOp("b", "h", 1, 5, false);
	private final MockOp op6 = new MockOp("c", "r", 2, 5, false);
	private final MockOp op7 = new MockOp("c", "s", 2, 5, false);
	private final MockOp op8 = new MockOp("d", "f", 4, 1, false);
	private final MockOp op9 = new MockOp("d", "h", 4, 1, false);
	private List<DynamoWriteOperation> opList1;

	private final MockOp op26 = new MockOp("a", "d",6, 3, false);
	private final MockOp op27 = new MockOp("a", "d",7, 3, false);
	private final MockOp op28 = new MockOp("a", "d",7, 3, true); // rollback point
	private final MockOp op29 = new MockOp("a", "d",9, 3, false);
	private List<DynamoWriteOperation> opList2;

	@Before
	public void before() {

		this.opList1 = new ArrayList<DynamoWriteOperation>();
		this.opList1.add(op9);
		this.opList1.add(op8);
		this.opList1.add(op7);
		this.opList1.add(op6);
		this.opList1.add(op5);
		this.opList1.add(op4);
		this.opList1.add(op3);
		this.opList1.add(op2);
		this.opList1.add(op1);

		this.opList2 = new ArrayList<DynamoWriteOperation>();
		this.opList2.add(op29);
		this.opList2.add(op27);
		this.opList2.add(op28);
		this.opList2.add(op26);
	}

	@Test
	public void testOpList1() {

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		DynamoWriteExecution exe1 = new DynamoWriteExecution("opList1", opList1);
		executor.execute(exe1);

		Assert.assertTrue(this.op1.getExecuted());
		Assert.assertFalse(this.op1.getRestored());
		Assert.assertTrue(this.op2.getExecuted());
		Assert.assertFalse(this.op2.getRestored());
		Assert.assertTrue(this.op3.getExecuted());
		Assert.assertFalse(this.op3.getRestored());
		Assert.assertTrue(this.op4.getExecuted());
		Assert.assertFalse(this.op4.getRestored());
		Assert.assertTrue(this.op5.getExecuted());
		Assert.assertFalse(this.op5.getRestored());
		Assert.assertTrue(this.op6.getExecuted());
		Assert.assertFalse(this.op6.getRestored());
		Assert.assertTrue(this.op7.getExecuted());
		Assert.assertFalse(this.op7.getRestored());
		Assert.assertTrue(this.op8.getExecuted());
		Assert.assertFalse(this.op8.getRestored());
		Assert.assertTrue(this.op9.getExecuted());
		Assert.assertFalse(this.op9.getRestored());
	}

	@Test
	public void testOpList2() {

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		DynamoWriteExecution exe2 = new DynamoWriteExecution("opList2", opList2);
		executor.execute(exe2);

		// Verify restore
		Assert.assertTrue(this.op26.getExecuted());
		Assert.assertTrue(this.op26.getRestored());
		Assert.assertTrue(this.op27.getExecuted());
		Assert.assertTrue(this.op27.getRestored());
		Assert.assertTrue(this.op28.getExecuted());
		Assert.assertTrue(this.op28.getRestored());
		Assert.assertFalse(this.op29.getExecuted());
		Assert.assertFalse(this.op29.getRestored());
	}

	// Creates our own mock to also test the compareTo() implemented in LineagePairWriteOperation
	private static class MockOp extends LineagePairWriteOperation {

		private final String ancId;
		private final String descId;
		private final int depth;
		private final int dist;
		private final boolean fail;
		private boolean executed = false;
		private boolean restored = false;

		public MockOp(String ancId, String descId, int depth, int dist, boolean fail) {
			this.ancId = ancId;
			this.descId = descId;
			this.depth = depth;
			this.dist = dist;
			this.fail = fail;
		}

		@Override
		int getAncestorDepth() {
			return depth;
		}

		@Override
		int getDistance() {
			return dist;
		}

		@Override
		String getAncestorId() {
			return ancId;
		}

		@Override
		String getDescendantId() {
			return descId;
		}

		@Override
		public boolean write(int step) {
			this.executed = true;
			if (fail) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public void restore(int step) {
			this.restored = true; 
		}

		public boolean getExecuted() {
			return this.executed;
		}

		public boolean getRestored() {
			return this.restored;
		}
	}
}
