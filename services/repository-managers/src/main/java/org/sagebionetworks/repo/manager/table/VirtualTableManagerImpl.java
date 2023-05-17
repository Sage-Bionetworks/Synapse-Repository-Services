package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class VirtualTableManagerImpl implements VirtualTableManager {

	public VirtualTableManagerImpl() {}

	@Override
	public void validate(VirtualTable virtualTable) {
		ValidateArgument.required(virtualTable, "The virtualTable");

		String definingSql = virtualTable.getDefiningSQL();

		ValidateArgument.requiredNotBlank(definingSql, "The definingSQL of the virtual table");

		QuerySpecification querySpecification;

		try {
			querySpecification = TableQueryParser.parserQuery(definingSql);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
		querySpecification.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);

	}

}
