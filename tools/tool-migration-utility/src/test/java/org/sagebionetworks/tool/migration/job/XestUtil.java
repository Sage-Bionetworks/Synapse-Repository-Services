package org.sagebionetworks.tool.migration.job;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;

// This used to be called 'TestUtil' but JUnit choked on it, saying it has no tests
public class XestUtil {
	public static MigratableObjectData createMigratableObjectData(String id, String etag, String parentId) {
		return createMigratableObjectData(id, etag, parentId, MigratableObjectType.ENTITY);
	}
	
	public static MigratableObjectData createMigratableObjectData(String id, String etag, String parentId, MigratableObjectType mot) {
		MigratableObjectData ans = new MigratableObjectData();
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(id);
		mod.setType(mot);
		ans.setId(mod);
		ans.setEtag(etag);
		Set<MigratableObjectDescriptor> dependencies = new HashSet<MigratableObjectDescriptor>();
		if (parentId!=null) {
			MigratableObjectDescriptor parent = new MigratableObjectDescriptor();
			parent.setId(parentId);
			parent.setType(mot);
			dependencies.add(parent);
		}
		ans.setDependencies(dependencies);
		return ans;
	}
	
	public static MigratableObjectData cloneMigratableObjectData(MigratableObjectData mod) {
		MigratableObjectData clone = new MigratableObjectData();
		MigratableObjectDescriptor id = mod.getId();
		MigratableObjectDescriptor cloneId = new MigratableObjectDescriptor();
		cloneId.setId(id.getId());
		cloneId.setType(id.getType());
		clone.setId(cloneId);
		clone.setEtag(mod.getEtag());
		Set<MigratableObjectDescriptor> dependencies = new HashSet<MigratableObjectDescriptor>();
		for (MigratableObjectDescriptor d : mod.getDependencies()) {
			MigratableObjectDescriptor dMod = new MigratableObjectDescriptor();
			dMod.setId(d.getId());
			dMod.setType(d.getType());
			dependencies.add(dMod);
		}
		clone.setDependencies(dependencies);
		return clone;
	}

}
