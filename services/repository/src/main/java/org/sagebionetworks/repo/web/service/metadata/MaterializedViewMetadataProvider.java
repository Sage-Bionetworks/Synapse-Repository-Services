package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;


@Service
public class MaterializedViewMetadataProvider implements EntityValidator<MaterializedView> {

	@Override
	public void validateEntity(MaterializedView entity, EntityEvent event) throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		
		String viewSQL = entity.getDefiningSQL();
		
		ValidateArgument.requiredNotBlank(viewSQL, "The materialized view definingSQL");
		
		QuerySpecification querySpecification;
		
		try {
			querySpecification = TableQueryParser.parserQuery(viewSQL);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
		querySpecification.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
			
	}

}
