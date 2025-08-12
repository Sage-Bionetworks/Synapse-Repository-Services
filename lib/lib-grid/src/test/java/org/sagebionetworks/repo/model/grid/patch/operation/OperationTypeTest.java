package org.sagebionetworks.repo.model.grid.patch.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.node.IndexType;

public class OperationTypeTest {

	@Test
	public void testNewCon() {
		assertEquals(0, OperationType.new_con.getCode());
		assertEquals(IndexType.con, OperationType.new_con.getIndexType());
	}

	@Test
	public void testNewVal() {
		assertEquals(1, OperationType.new_val.getCode());
		assertEquals(IndexType.val, OperationType.new_val.getIndexType());
	}

	@Test
	public void testNewObj() {
		assertEquals(2, OperationType.new_obj.getCode());
		assertEquals(IndexType.obj, OperationType.new_obj.getIndexType());
	}

	@Test
	public void testNewVec() {
		assertEquals(3, OperationType.new_vec.getCode());
		assertEquals(IndexType.vec, OperationType.new_vec.getIndexType());
	}

	@Test
	public void testNewStr() {
		assertEquals(4, OperationType.new_str.getCode());
		assertEquals(IndexType.str, OperationType.new_str.getIndexType());
	}

	@Test
	public void testNewBin() {
		assertEquals(5, OperationType.new_bin.getCode());
		assertEquals(IndexType.bin, OperationType.new_bin.getIndexType());
	}

	@Test
	public void testNewArr() {
		assertEquals(6, OperationType.new_arr.getCode());
		assertEquals(IndexType.arr, OperationType.new_arr.getIndexType());
	}

	@Test
	public void testInsVal() {
		assertEquals(9, OperationType.ins_val.getCode());
		assertEquals(IndexType.val, OperationType.ins_val.getIndexType());
	}

	@Test
	public void testInsObj() {
		assertEquals(10, OperationType.ins_obj.getCode());
		assertEquals(IndexType.obj, OperationType.ins_obj.getIndexType());
	}

	@Test
	public void testInsVec() {
		assertEquals(11, OperationType.ins_vec.getCode());
		assertEquals(IndexType.vec, OperationType.ins_vec.getIndexType());
	}

	@Test
	public void testInsStr() {
		assertEquals(12, OperationType.ins_str.getCode());
		assertEquals(IndexType.str, OperationType.ins_str.getIndexType());
	}

	@Test
	public void testInsBin() {
		assertEquals(13, OperationType.ins_bin.getCode());
		assertEquals(IndexType.bin, OperationType.ins_bin.getIndexType());
	}

	@Test
	public void testInsArr() {
		assertEquals(14, OperationType.ins_arr.getCode());
		assertEquals(IndexType.arr, OperationType.ins_arr.getIndexType());
	}
	
	@Test
	public void testDel() {
		assertEquals(16, OperationType.del.getCode());
		assertEquals(IndexType.arr, OperationType.del.getIndexType());
	}
	
	@Test
	public void testNop() {
		assertEquals(17, OperationType.nop.getCode());
		assertEquals(null, OperationType.nop.getIndexType());
	}

}
