package org.sagebionetworks.bridge.model.dbo.dao;

import java.util.List;

import org.junit.After;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.Lists;

public abstract class TestBase {
	@SuppressWarnings("rawtypes")
	public class Deletable {
		Class clazz;
		Object id;

		public Deletable(Class clazz, Object id) {
			this.id = id;
			this.clazz = clazz;
		}
	}

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private IdGenerator idGenerator;

	private List<Deletable> toDelete = Lists.newArrayList();

	@After
	public void deleteAfterwards() throws Exception {
		if (dboBasicDao != null) {
			for (Deletable item : Lists.reverse(toDelete)) {
				doDelete(item);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void doDelete(Deletable item) throws Exception {
		Long id = KeyFactory.stringToKey(item.id.toString());
		if (item.clazz == UserGroup.class) {
			userGroupDAO.delete("" + id);
		} else {
			SqlParameterSource params = new SinglePrimaryKeySqlParameterSource(id);
			try {
				dboBasicDao.getObjectByPrimaryKey(item.clazz, params);
			} catch (NotFoundException e) {
				return;
			}
			dboBasicDao.deleteObjectByPrimaryKey(item.clazz, params);
		}
	}

	public void addToDelete(Class<?> clazz, Object id) {
		addToDelete(new Deletable(clazz, id));
	}

	public void addToDelete(Deletable deletable) {
		toDelete.add(deletable);
	}

	public String createMember() throws Exception {
		UserGroup newMember = new UserGroup();
		newMember.setIsIndividual(true);
		String newMemberId = userGroupDAO.create(newMember).toString();
		addToDelete(UserGroup.class, newMemberId);
		return newMemberId;
	}
}
