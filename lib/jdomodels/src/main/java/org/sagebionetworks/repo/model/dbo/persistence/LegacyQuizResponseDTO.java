
package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.quiz.QuestionResponse;


/**
 * Legacy DTO, needed for migration.  
 * The set of responses to a Quiz
 */
public class LegacyQuizResponseDTO {

    private List<QuestionResponse> questionResponses;
    private Date createdOn;
    private Long id;
    private String createdBy;
    private Long score;
    private Long quizId;
    private Boolean pass;

    public LegacyQuizResponseDTO() {
    }


    /**
     * The list of responses to the questions in the Quiz
     * 
     * 
     * 
     * @return
     *     questionResponses
     */
    public List<QuestionResponse> getQuestionResponses() {
        return questionResponses;
    }

    /**
     * The list of responses to the questions in the Quiz
     * 
     * 
     * 
     * @param questionResponses
     */
    public void setQuestionResponses(List<QuestionResponse> questionResponses) {
        this.questionResponses = questionResponses;
    }

    /**
     * When this response was created
     * 
     * 
     * 
     * @return
     *     createdOn
     */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * When this response was created
     * 
     * 
     * 
     * @param createdOn
     */
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * The unique ID for this response
     * 
     * 
     * 
     * @return
     *     id
     */
    public Long getId() {
        return id;
    }

    /**
     * The unique ID for this response
     * 
     * 
     * 
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The unique identifier of the one creating the response
     * 
     * 
     * 
     * @return
     *     createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * The unique identifier of the one creating the response
     * 
     * 
     * 
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * the number of correct answers in the response
     * 
     * 
     * 
     * @return
     *     score
     */
    public Long getScore() {
        return score;
    }

    /**
     * the number of correct answers in the response
     * 
     * 
     * 
     * @param score
     */
    public void setScore(Long score) {
        this.score = score;
    }

    /**
     * The ID of the Quiz to which this is a response
     * 
     * 
     * 
     * @return
     *     quizId
     */
    public Long getQuizId() {
        return quizId;
    }

    /**
     * The ID of the Quiz to which this is a response
     * 
     * 
     * 
     * @param quizId
     */
    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    /**
     * if the Quiz is a test, this indicates whether the response passes, as determined by the system
     * 
     * 
     * 
     * @return
     *     pass
     */
    public Boolean getPass() {
        return pass;
    }

    /**
     * if the Quiz is a test, this indicates whether the response passes, as determined by the system
     * 
     * 
     * 
     * @param pass
     */
    public void setPass(Boolean pass) {
        this.pass = pass;
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = ((prime*result)+((questionResponses == null)? 0 :questionResponses.hashCode()));
        result = ((prime*result)+((createdOn == null)? 0 :createdOn.hashCode()));
        result = ((prime*result)+((id == null)? 0 :id.hashCode()));
        result = ((prime*result)+((createdBy == null)? 0 :createdBy.hashCode()));
        result = ((prime*result)+((score == null)? 0 :score.hashCode()));
        result = ((prime*result)+((quizId == null)? 0 :quizId.hashCode()));
        result = ((prime*result)+((pass == null)? 0 :pass.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass()!= obj.getClass()) {
            return false;
        }
        LegacyQuizResponseDTO other = ((LegacyQuizResponseDTO) obj);
        if (questionResponses == null) {
            if (other.questionResponses!= null) {
                return false;
            }
        } else {
            if (!questionResponses.equals(other.questionResponses)) {
                return false;
            }
        }
        if (createdOn == null) {
            if (other.createdOn!= null) {
                return false;
            }
        } else {
            if (!createdOn.equals(other.createdOn)) {
                return false;
            }
        }
        if (id == null) {
            if (other.id!= null) {
                return false;
            }
        } else {
            if (!id.equals(other.id)) {
                return false;
            }
        }
        if (createdBy == null) {
            if (other.createdBy!= null) {
                return false;
            }
        } else {
            if (!createdBy.equals(other.createdBy)) {
                return false;
            }
        }
        if (score == null) {
            if (other.score!= null) {
                return false;
            }
        } else {
            if (!score.equals(other.score)) {
                return false;
            }
        }
        if (quizId == null) {
            if (other.quizId!= null) {
                return false;
            }
        } else {
            if (!quizId.equals(other.quizId)) {
                return false;
            }
        }
        if (pass == null) {
            if (other.pass!= null) {
                return false;
            }
        } else {
            if (!pass.equals(other.pass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds toString method to pojo.
     * returns a string
     * 
     * @return
     */
    @Override
    public String toString() {
        StringBuilder result;
        result = new StringBuilder();
        result.append("");
        result.append("org.sagebionetworks.repo.model.quiz.QuizResponse");
        result.append(" [");
        result.append("questionResponses=");
        result.append(questionResponses);
        result.append(" ");
        result.append("createdOn=");
        result.append(createdOn);
        result.append(" ");
        result.append("id=");
        result.append(id);
        result.append(" ");
        result.append("createdBy=");
        result.append(createdBy);
        result.append(" ");
        result.append("score=");
        result.append(score);
        result.append(" ");
        result.append("quizId=");
        result.append(quizId);
        result.append(" ");
        result.append("pass=");
        result.append(pass);
        result.append(" ");
        result.append("]");
        return result.toString();
    }

}
