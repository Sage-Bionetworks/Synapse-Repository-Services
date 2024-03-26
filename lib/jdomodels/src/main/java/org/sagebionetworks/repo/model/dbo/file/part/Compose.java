package org.sagebionetworks.repo.model.dbo.file.part;

import java.util.Objects;

public class Compose {

	PartRange left;
	PartRange right;

	public PartRange getLeft() {
		return left;
	}

	public Compose setLeft(PartRange left) {
		this.left = left;
		return this;
	}

	public PartRange getRight() {
		return right;
	}

	public Compose setRight(PartRange right) {
		this.right = right;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Compose other = (Compose) obj;
		return Objects.equals(left, other.left) && Objects.equals(right, other.right);
	}

	@Override
	public String toString() {
		return "Compose [left=" + left + ", right=" + right + "]";
	}

}
