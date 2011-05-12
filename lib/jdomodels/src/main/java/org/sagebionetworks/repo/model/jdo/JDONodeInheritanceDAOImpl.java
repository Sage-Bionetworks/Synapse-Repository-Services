package org.sagebionetworks.repo.model.jdo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDONodeInheritanceDAOImpl implements NodeInheritanceDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;
	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	private JDONode getNodeById(Long id) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		try{
			return jdoTemplate.getObjectById(JDONode.class, id);
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Set<String> getBeneficiaries(String benefactorId) throws NotFoundException {
		JDONode benefactor = getNodeById(Long.parseLong(benefactorId));
		HashSet<String> set = new HashSet<String>();
		Iterator<JDONode> it = benefactor.getPermissionsBeneficiaries().iterator();
		while(it.hasNext()){
			JDONode node = it.next();
			set.add(node.getId().toString());
		}
		return set;
	}

	@Transactional(readOnly = true)
	@Override
	public String getBenefactor(String beneficiaryId) throws NotFoundException {
		JDONode beneficiary = getNodeById(Long.parseLong(beneficiaryId));
		return beneficiary.getPermissionsBenefactor().getId().toString();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException {
		JDONode benefactor = getNodeById(Long.parseLong(toBenefactorId));
		JDONode beneficiary = getNodeById(Long.parseLong(beneficiaryId));
		beneficiary.setPermissionsBenefactor(benefactor);
	}

}
