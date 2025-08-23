package org.sagebionetworks.repo.model.grid.patch;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.node.ConstantUtils;

public class ConValue {
	
	private final ConType type;
	private final Object value;
	
	public ConValue(ConType type, Object value) {
		super();
		this.type = type;
		this.value = value;
	}

	public ConType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConValue other = (ConValue) obj;
		return type == other.type && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ConValue [type=" + type + ", value=" + value + "]";
	}

	public String toJson() {
		return ConstantUtils.constantValueToJson(value);
	}
	
}
