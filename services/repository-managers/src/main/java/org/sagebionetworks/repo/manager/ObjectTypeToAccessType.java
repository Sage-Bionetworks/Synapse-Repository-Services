package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;

public enum ObjectTypeToAccessType {
        ENTITY(ObjectType.ENTITY, ACCESS_TYPE.CHANGE_PERMISSIONS),
        TEAM(ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE),
        EVALUATION(ObjectType.EVALUATION, ACCESS_TYPE.CHANGE_PERMISSIONS),
        FORM_GROUP(ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS),
        OAUTH_CLIENT(ObjectType.OAUTH_CLIENT, ACCESS_TYPE.CHANGE_PERMISSIONS),
        ORGANIZATION(ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS),
        PORTAL(ObjectType.PORTAL, ACCESS_TYPE.CHANGE_PERMISSIONS);

        private final ObjectType objectType;
        private final ACCESS_TYPE accessTypes;

    ObjectTypeToAccessType(ObjectType entity, ACCESS_TYPE changePermissions) {
            this.objectType = entity;
        	this.accessTypes = changePermissions;
    }

    public ObjectType getObjectType() {
            return objectType;
        }

        public ACCESS_TYPE getAccessTypes() {
            return accessTypes;
        }

        public static ACCESS_TYPE getAccessTypesForObjectType(ObjectType objectType) {
            for (ObjectTypeToAccessType mapping : ObjectTypeToAccessType.values()) {
                if (mapping.getObjectType() == objectType) {
                    return mapping.getAccessTypes();
                }
            }
            throw new IllegalArgumentException("Unsupported ObjectType: " + objectType);
        }

}
