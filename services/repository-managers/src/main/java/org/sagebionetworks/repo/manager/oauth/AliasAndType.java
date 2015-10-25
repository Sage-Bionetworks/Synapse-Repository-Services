package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.principal.AliasType;

public class AliasAndType {
	private String alias;
	private AliasType type;
	
	public AliasAndType(String alias, AliasType type) {
		super();
		this.alias = alias;
		this.type = type;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public AliasType getType() {
		return type;
	}

	public void setType(AliasType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AliasAndType other = (AliasAndType) obj;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AliasAndType [alias=" + alias + ", type=" + type + "]";
	}


}
