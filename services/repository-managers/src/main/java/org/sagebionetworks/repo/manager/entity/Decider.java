package org.sagebionetworks.repo.manager.entity;

import java.util.Optional;
import java.util.function.Function;

public interface Decider extends Function<Context, Optional<UsersEntityAccessInfo>>{

}
