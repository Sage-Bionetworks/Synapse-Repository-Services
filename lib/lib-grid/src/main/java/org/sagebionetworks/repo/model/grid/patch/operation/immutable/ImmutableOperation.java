package org.sagebionetworks.repo.model.grid.patch.operation.immutable;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationView;

import java.util.Objects;

/**
 * An immutable wrapper for an {@link OperationView}.
 * @param <T>
 */
public final class ImmutableOperation<T extends OperationView<T>> implements OperationView<T> {

    private final OperationView<? extends T> operation;

    public ImmutableOperation(OperationView<? extends T> operation) {
        this.operation = operation;
    }

    public static <T extends Operation<T>> ImmutableOperation<T> of(Operation<? extends T> operation) {
        return new ImmutableOperation<>(operation);
    }

    @Override
    public OperationType getType() {
        return operation.getType();
    }

    @Override
    public LogicalTimestamp getOperationId() {
        return operation.getOperationId();
    }

    @Override
    public long getSpan() {
        return operation.getSpan();
    }


    @Override
    public int hashCode() {
        return Objects.hash(operation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableOperation<?> other = (ImmutableOperation<?>) obj;
        return Objects.equals(operation, other.operation);
    }

    @Override
    public String toString() {
        return "ImmutableOperation [operation=" + operation.toString() + "]";
    }

    /**
     * Get the wrapped operation.
     * @return
     */
    public OperationView<? extends T> getOperation() {
        return operation;
    }
}
