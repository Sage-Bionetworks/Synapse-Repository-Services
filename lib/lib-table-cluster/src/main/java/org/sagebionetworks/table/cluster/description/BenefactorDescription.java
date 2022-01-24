package org.sagebionetworks.table.cluster.description;

import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;

public class BenefactorDescription {
	
	private final String benefactorColumnName;
	private final ObjectType benefactorType;
	
	public BenefactorDescription(String benefactorColumnName, ObjectType benefactorType) {
		super();
		this.benefactorColumnName = benefactorColumnName;
		this.benefactorType = benefactorType;
	}

	/**
	 * @return the benefactorColumnName
	 */
	public String getBenefactorColumnName() {
		return benefactorColumnName;
	}


	/**
	 * @return the benefactorType
	 */
	public ObjectType getBenefactorType() {
		return benefactorType;
	}


	@Override
	public int hashCode() {
		return Objects.hash(benefactorColumnName, benefactorType);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BenefactorDescription)) {
			return false;
		}
		BenefactorDescription other = (BenefactorDescription) obj;
		return Objects.equals(benefactorColumnName, other.benefactorColumnName)
				&& benefactorType == other.benefactorType;
	}


	@Override
	public String toString() {
		return "BenefactorDescription [benefactorColumnName=" + benefactorColumnName + ", benefactorType="
				+ benefactorType + "]";
	}

}
