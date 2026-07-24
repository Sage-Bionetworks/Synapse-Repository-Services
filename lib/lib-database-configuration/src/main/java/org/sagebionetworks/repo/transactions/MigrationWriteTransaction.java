package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write transaction bound to the migration data source (rewriteBatchedStatements=true)
 * for high-throughput batch inserts during migration restores.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(
	transactionManager = "migrationTxManager",
	isolation = Isolation.READ_COMMITTED,
	propagation = Propagation.REQUIRED,
	rollbackFor = Throwable.class
)
public @interface MigrationWriteTransaction {

}
