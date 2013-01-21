package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Database Object for a Wiki Page.
 * @author John
 *
 */
public class DBOWikiPage implements DatabaseObject<DBOUserProfile> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_WIKI_ID, true),
	};

	@Override
	public TableMapping<DBOUserProfile> getTableMapping() {
		// TODO Auto-generated method stub
		return null;
	}

}
