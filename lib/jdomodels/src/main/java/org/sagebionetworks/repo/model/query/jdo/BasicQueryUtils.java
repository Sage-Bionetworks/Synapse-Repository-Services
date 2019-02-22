package org.sagebionetworks.repo.model.query.jdo;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;

public class BasicQueryUtils {
	
	public static final String VERSIONABLE = "versionable";
	
	public static final List<String> VERSIONABLE_TYPES = Arrays.asList(EntityType.file.name(), EntityType.table.name(), EntityType.entityview.name());

	public static final String ENTITY = "entity";
	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	public static final Expression EXP_BENEFACTOR_NOT_EQUAL_TRASH = new Expression(new CompoundId(null, NodeField.BENEFACTOR_ID.getFieldName()), Comparator.NOT_EQUALS, TRASH_FOLDER_ID);
	public static final Expression EXP_VERSIONABLE_TYPES = new Expression(new CompoundId(null, NodeField.NODE_TYPE.getFieldName()), Comparator.IN, VERSIONABLE_TYPES);
	
	/**
	 * Convert the query from clause to a list of expressions.s
	 * @param query
	 * @return
	 */
	public static BasicQuery convertFromToExpressions(BasicQuery query){
		BasicQuery processed = new BasicQuery(query);
		// Add a filter on type if needed
		if(query.getFrom() != null){
			// Add the type to the filter
			if(!ENTITY.equals(query.getFrom().toLowerCase())){
				if(VERSIONABLE.equals(query.getFrom().toLowerCase())){
					processed.addExpression(EXP_VERSIONABLE_TYPES);
				}else{
					EntityType type = EntityType.valueOf(query.getFrom().toLowerCase());
					processed.addExpression(new Expression(new CompoundId(null, NodeField.NODE_TYPE.getFieldName()), Comparator.EQUALS, type.name()));
				}
			}
		}
		// Filter out nodes in the trash can
		processed.addExpression(EXP_BENEFACTOR_NOT_EQUAL_TRASH);
		// clear the from clause
		processed.setFrom(null);
		return processed;
	}

}
