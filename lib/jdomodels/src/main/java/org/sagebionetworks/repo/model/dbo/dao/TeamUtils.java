package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

import java.io.IOException;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class TeamUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Team.class).build();

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static void copyDtoToDbo(Team dto, DBOTeam dbo) throws DatastoreException {
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setEtag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	public static Team copyDboToDto(DBOTeam dbo) throws DatastoreException {
		Team dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		return dto;
	}

	public static void copyToSerializedField(Team dto, DBOTeam dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Team deserialize(byte[] b) {		
		try {
			return (Team)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, b);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Team copyFromSerializedField(DBOTeam dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
	
	// SQL building block to select admin Team members and the Teams on which they are admin's
	// Note: In this SQL fragment gm is a member of the Team 't' having UPDATE access to the Team
	public static final String ALL_TEAMS_AND_ADMIN_MEMBERS_CORE =
				TABLE_TEAM+" t, "+
				TABLE_ACCESS_CONTROL_LIST+" acl, "+
				TABLE_RESOURCE_ACCESS+" ra, "+
				TABLE_RESOURCE_ACCESS_TYPE+" at, "+
				TABLE_GROUP_MEMBERS+" gm "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" and acl."+COL_ACL_OWNER_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" and acl."+COL_OWNER_TYPE+"='"+ObjectType.TEAM.name()+
			"' and ra."+COL_RESOURCE_ACCESS_GROUP_ID+"=gm."+COL_GROUP_MEMBERS_MEMBER_ID+
			" and ra."+COL_RESOURCE_ACCESS_OWNER+"=acl."+COL_ACL_ID+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ID+"=ra."+COL_RESOURCE_ACCESS_ID+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"='"+ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE+"'";
			
}
