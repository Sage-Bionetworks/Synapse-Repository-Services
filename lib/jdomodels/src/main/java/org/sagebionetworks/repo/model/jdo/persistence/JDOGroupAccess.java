package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true")
public class JDOGroupAccess {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	@Column(name=SqlConstants.COL_NODE_PARENT_ID)
    @ForeignKey(name="NODE_PARENT_FK", deleteAction=ForeignKeyAction.CASCADE)
    private JDOAccessControlList accessControlList;
	
	
	private JDOUserGroup userGroup;
	
}
