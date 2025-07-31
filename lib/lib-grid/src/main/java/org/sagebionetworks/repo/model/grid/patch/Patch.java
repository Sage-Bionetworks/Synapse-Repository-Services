package org.sagebionetworks.repo.model.grid.patch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationView;
import org.sagebionetworks.repo.model.grid.patch.operation.immutable.ImmutableOperation;

public class Patch {

	private LogicalTimestamp patchId;
	private String metadata;
	private List<Operation<?>> operations;
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

	public List<Operation<?>> getOperations() {
		return operations;
	}

	public Patch setOperations(List<Operation<?>> operations) {
		this.operations = operations;
		if (operations == null) {
			this.span = 0L;
		} else {
			this.span = operations.stream().mapToLong(Operation::getSpan).sum();
		}
		return this;
	}

	/**
	 * Add a new operation of the provided type to the patch. The newly created
	 * operation will be issued a correct operationId.
	 *
	 * @param <T>
	 * @param clazz
	 * @return the operation, wrapped to ensure immutability. Note that any changes to the operation after this call may
	 *   break the patch
	 */
	public <T extends Operation<T>> ImmutableOperation<T> addNewOperation(Class<? extends T> clazz) {
		try {
			T operation = clazz.getDeclaredConstructor().newInstance();
			return this.addNewOperation(operation);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a new operation to the patch. The newly created
	 * operation will be issued a correct operationId.
	 *
	 * @param <T>
	 * @param operation
	 * @return the operation, wrapped to ensure immutability. Note that any changes to the operation after this call may
	 *   break the patch
	 */
	public <T extends Operation<T>> ImmutableOperation<T> addNewOperation(T operation) {
		try {
			if (operations == null) {
				operations = new ArrayList<>();
			}
			operation.setOperationId(LogicalTimestamp.newIncrement(patchId, getSpan()));
			operations.add(operation);
			span += operation.getSpan();
			return ImmutableOperation.of(operation);
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
