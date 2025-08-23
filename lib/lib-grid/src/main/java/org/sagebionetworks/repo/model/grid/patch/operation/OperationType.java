package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.node.IndexType;

public enum OperationType {

	new_con(0, IndexType.con),
	new_val(1, IndexType.val),
	new_obj(2, IndexType.obj),
	new_vec(3, IndexType.vec),
	new_str(4, IndexType.str),
	new_bin(5, IndexType.bin),
	new_arr(6, IndexType.arr),
	ins_val(9, IndexType.val),
	ins_obj(10, IndexType.obj),
	ins_vec(11, IndexType.vec),
	ins_str(12, IndexType.str),
	ins_bin(13, IndexType.bin),
	ins_arr(14, IndexType.arr),
	del(16, IndexType.arr),
	nop(17, null);

	private OperationType(int code, IndexType indexType) {
		this.code = code;
		this.indexType = indexType;
	}

	private final int code;
	private final IndexType indexType;

	public static OperationType fromCode(int code) {
		for (OperationType type : OperationType.values()) {
			if (type.code == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown code:" + code);
	}

	public int getCode() {
		return code;
	}

	/**
	 * The type of node that is changed by an operation of this type.
	 * 
	 * @return
	 */
	public IndexType getIndexType() {
		return indexType;
	}

}
