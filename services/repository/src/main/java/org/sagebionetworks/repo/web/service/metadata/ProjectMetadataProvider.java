package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class ProjectMetadataProvider implements TypeSpecificMetadataProvider<Project>, TypeSpecificCreateProvider<Project> {

	@Autowired
	ForumManager forumManager;
	@Autowired
	SubscriptionManager subscriptionManager;

	@Override
	public void addTypeSpecificMetadata(Project entity, UserInfo user, EventType eventType) {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getId() == null) throw new IllegalArgumentException("Entity.id cannot be null");
	}

	@Override
	public void entityCreated(UserInfo userInfo, Project project) {
		Forum forum = forumManager.createForum(userInfo, project.getId());
		Topic toSubscribe = new Topic();
		toSubscribe.setObjectId(forum.getId());
		toSubscribe.setObjectType(SubscriptionObjectType.FORUM);
		subscriptionManager.create(userInfo, toSubscribe);
	}
}
