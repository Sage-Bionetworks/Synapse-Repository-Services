package org.sagebionetworks.table.cluster.view.filter;

import org.sagebionetworks.repo.model.LimitExceededException;

import java.util.Set;

@FunctionalInterface
public interface ContainerProvider {
    Set<Long> getScope() throws LimitExceededException;
}
