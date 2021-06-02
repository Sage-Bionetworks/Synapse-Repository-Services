package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationObjectHelper implements DaoObjectHelper<Evaluation> {
	
	private IdGenerator idGenerator;
	private EvaluationDAO evaluationDao;

	@Autowired
	public EvaluationObjectHelper(IdGenerator idGenerator, EvaluationDAO evaluationDao) {
		this.idGenerator = idGenerator;
		this.evaluationDao = evaluationDao;
	}
	
	@Override
	public Evaluation create(Consumer<Evaluation> consumer) {
		Evaluation evaluation = new Evaluation();
		
		evaluation.setId(idGenerator.generateNewId(IdType.EVALUATION_ID).toString());
		evaluation.setContentSource("123");
		evaluation.setName("TestEvaluation");
		evaluation.setCreatedOn(new Date());
		evaluation.setOwnerId("123");
		
		consumer.accept(evaluation);
		
		String evaluationId = evaluationDao.create(evaluation, Long.valueOf(evaluation.getOwnerId()));
		
		return evaluationDao.get(evaluationId);
	}

}
