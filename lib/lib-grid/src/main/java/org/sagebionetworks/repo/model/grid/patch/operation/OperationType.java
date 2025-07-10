package org.sagebionetworks.repo.model.grid.patch.operation;

public enum OperationType {

	new_con(0),
	new_val(1),
	new_obj(2),
	new_vec(3),
	new_str(4),
	new_bin(5),
	new_arr(6),
	ins_val(9),
	ins_obj(10),
	ins_vec(11),
	ins_str(12),
	ins_bin(13),
	ins_arr(14),
	del(16),
	nop(17);
	

	private OperationType(int code) {
		this.code = code;
	}

	private final int code;

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
	
	
}
