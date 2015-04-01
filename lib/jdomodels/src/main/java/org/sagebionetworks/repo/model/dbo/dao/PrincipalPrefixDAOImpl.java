package org.sagebionetworks.repo.model.dbo.dao;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPrincipalPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PrincipalPrefixDAOImpl implements PrincipalPrefixDAO {
	
	private static final String EMPTY = "";

	private static final String REG_EX_NON_ALPHA_NUMERIC = "^[a-z,0-9]";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean addPrincipalAlias(String alias, Long principalId) {
		String processed = preProcessToken(alias);
		if(EMPTY.equals(processed)){
			return false;
		}
		DBOPrincipalPrefix dbo = new DBOPrincipalPrefix();
		dbo.setPrincipalId(principalId);
		dbo.setToken(processed);
		basicDAO.createNew(dbo);
		return true;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean addPrincipalName(String firstName, String lastName,
			Long principalId) {

		List<DBOPrincipalPrefix> list = new LinkedList<DBOPrincipalPrefix>();
		// frist-last
		String firstLast = preProcessToken(firstName+lastName);
		DBOPrincipalPrefix dbo = new DBOPrincipalPrefix();
		if(!EMPTY.equals(firstLast)){
			dbo.setPrincipalId(principalId);
			dbo.setToken(firstLast);
			list.add(dbo);
		}

		// last-first
		String lastFrist = preProcessToken(lastName+firstName);
		if(!EMPTY.equals(lastFrist)){
			dbo = new DBOPrincipalPrefix();
			dbo.setPrincipalId(principalId);
			dbo.setToken(lastFrist);
			list.add(dbo);
		}

		if(list.isEmpty()){
			// Nothing to add
			return false;
		}
		// batch add.
		basicDAO.createBatch(list);
		return true;
	}
	
	/**
	 * Pre-process an input token.
	 * @param input
	 * @return
	 */
	public static String preProcessToken(String input){
		if(input == null){
			return EMPTY;
		}
		input = input.trim().toLowerCase();
		return input.replaceAll(REG_EX_NON_ALPHA_NUMERIC, EMPTY);
	}

	@Override
	public void clearPrincipal(Long principalId) {
		// TODO Auto-generated method stub

	}

	@Override
	public PaginatedResults<UserGroupHeader> listUsersForPrefix(String prefix,
			Long limit, Long offset) {
		
		return null;
	}

	@Override
	public PaginatedResults<TeamMember> listTeamMembersForPrefix(String prefix,
			Long teamId, Long limit, Long offset) {
		// TODO Auto-generated method stub
		return null;
	}

}
