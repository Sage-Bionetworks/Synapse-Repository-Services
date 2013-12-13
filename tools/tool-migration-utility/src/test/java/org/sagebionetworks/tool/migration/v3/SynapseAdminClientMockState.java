package org.sagebionetworks.tool.migration.v3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.WikiMigrationResult;
import org.sagebionetworks.repo.model.status.StackStatus;

/**
 * Helper for migration testing
 * Holds the state of a mock stack
 * 
 * TODO Needs better comments
 */
public class SynapseAdminClientMockState {
	
	public String endpoint;
	
	public Map<MigrationType, List<RowMetadata>> metadata = new LinkedHashMap<MigrationType, List<RowMetadata>>();
	
	public Set<Long> exceptionNodes = new HashSet<Long>();
	
	public List<Set<Long>> deleteRequestsHistory = new LinkedList<Set<Long>>();

	public Stack<StackStatus> statusHistory = new Stack<StackStatus>();

	public Stack<Long> currentChangeNumberStack = new Stack<Long>();
	
	public Long maxChangeNumber = 100L;
	
	public List<Long> replayChangeNumbersHistory = new LinkedList<Long>();

	public BackupRestoreStatus status;
	
	public long statusSequence = 0L;
	
	public List<WikiMigrationResult> wikiMigrationResults = new ArrayList<WikiMigrationResult>();
	
}
