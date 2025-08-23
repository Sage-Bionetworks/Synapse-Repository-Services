package org.sagebionetworks.repo.model.grid.patch;

import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.OperationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Patch {

	private LogicalTimestamp patchId;
	private String metadata;
	private List<Operation> operations;
	private long span = 0; // Cache the span as a performance optimization

	public LogicalTimestamp getPatchId() {
		return patchId;
	}

	public Patch setPatchId(LogicalTimestamp patchId) {
		this.patchId = patchId;
		return this;
	}

	public String getMetadata() {
		return metadata;
	}

	public Patch setMetadata(String metadta) {
		this.metadata = metadta;
		return this;
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public Patch setOperations(List<Operation> operations) {
		this.operations = operations;
		if (operations == null) {
			this.span = 0L;
		} else {
			this.span = operations.stream().mapToLong(Operation::getSpan).sum();
		}
		return this;
	}

	/**
	 * Factory Method that accepts any valid OperationBuilder.
	 * It generates the ID and asks the builder to construct the final object.
	 */
	public LogicalTimestamp addNewOperation(OperationBuilder builder) {
		try {
			if (operations == null) {
				operations = new ArrayList<>();
			}
			LogicalTimestamp nextId = LogicalTimestamp.newIncrement(patchId, getSpan());
			Operation operation = builder.build(nextId);
			operations.add(operation);
			span += operation.getSpan();
			return operation.getOperationId();
		} catch (IllegalArgumentException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The number of clock cycles consumed by this patch is the span.
	 * 
	 * @return
	 */
	public long getSpan() {
		if (operations == null) {
			return 0L;
		}
		return this.span;
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadata, operations, patchId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Patch other = (Patch) obj;
		return Objects.equals(metadata, other.metadata) && Objects.equals(operations, other.operations)
				&& Objects.equals(patchId, other.patchId);
	}

	@Override
	public String toString() {
		return "Patch [patchId=" + patchId + ", metadta=" + metadata + ", operations=" + operations + "]";
	}

}
