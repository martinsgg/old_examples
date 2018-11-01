package com.wildway.beans;

import java.io.IOException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.wildway.daos.ClientAnswerDAO;
import com.wildway.daos.QuestionWithAnswersDAO;
import com.wildway.entities.ClientAnswer;
import com.wildway.entities.QuestionWithAnswers;

@ManagedBean
@RequestScoped
public class QuestionBean extends ClientAnswer {
	static Log log = LogFactory.getLog(QuestionBean.class.getName());
	List<String> answers;
	
    public void checkDone(){
		if(getQuestionId()>getQuestionCount()){
			try {
				FacesContext.getCurrentInstance().getExternalContext().redirect("thank-you.xhtml");
			} catch (IOException e) {e.printStackTrace();}
		}
    }
	
	public QuestionBean(){
		checkDone();
		initQuestion();
	}
	
	public void initQuestion(){
		try {
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
			QuestionWithAnswersDAO questionWithAnswersDAO = (QuestionWithAnswersDAO) context.getBean("QuestionWithAnswersDAO");
			for(QuestionWithAnswers qa:questionWithAnswersDAO.list()){
				if(qa.getId() == getQuestionId()){
					this.setQuestion(qa.getQuestion());
					answers = qa.getAnswers();
					break;
				}
			}
			context.close(); 
		} catch (Exception e){log.error(e.getMessage());e.printStackTrace();}
	}
	
	public List<String> getAnswers(){
		return answers;
	}
	
	public static int getQuestionId(){
		int qid = 1;
		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
			qid = Integer.parseInt(session.getAttribute("qid").toString());
		} catch(Exception e){log.error(e.getMessage());e.printStackTrace();}
		
		return qid;
	}
	
	public int getQuestionCount(){
		return getQuestionCountS();
	}
	
	public static int getQuestionCountS(){
		int count = 0;
		try {
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
			QuestionWithAnswersDAO questionWithAnswersDAO = (QuestionWithAnswersDAO) context.getBean("QuestionWithAnswersDAO");
			count = questionWithAnswersDAO.list().size();
			context.close(); 
		} catch (Exception e){log.error(e.getMessage());e.printStackTrace();}
		return count;
	}
	
	public void submit(){
		// answer exists
		boolean answerExsists = false;
		try {
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
			QuestionWithAnswersDAO questionWithAnswersDAO = (QuestionWithAnswersDAO) context.getBean("QuestionWithAnswersDAO");
			QuestionWithAnswers qwa = questionWithAnswersDAO.getById(new Long(getQuestionId()));
			for(String answer:qwa.getAnswers()){
				if(answer.equals(this.getAnswer())){
					answerExsists = true;
				}
			}
			if(qwa.getAnswers().size()==0){
				answerExsists = true;
			}
			context.close();
		} catch (Exception e){}
		
		if(this.getAnswer() != null && this.getAnswer().length()>1 && answerExsists){
			try {
				ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
				ClientAnswerDAO clientAnswerDAO = (ClientAnswerDAO) context.getBean("ClientAnswerDAO");
				ClientAnswer ClientAnswer = new ClientAnswer();
				ClientAnswer.setAnswer(this.getAnswer());
				ClientAnswer.setQuestion(this.getQuestion());
				ClientAnswer.setQuestion_id(new Long(getQuestionId()));
				
				FacesContext facesContext = FacesContext.getCurrentInstance();
				HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
				
				String agent = "";
				try {
					agent = ((HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest()).getHeader("User-Agent");
				} catch(Exception e){}
				
				HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
				String ipAddress = request.getHeader("X-FORWARDED-FOR");
				if (ipAddress == null) {
				    ipAddress = request.getRemoteAddr();
				}
				
				JSONObject Participator = new JSONObject();
				Participator.put("id", session.getId());
				Participator.put("agent", agent);
				Participator.put("ip-address", ipAddress);
				ClientAnswer.setUser(Participator.toJSONString());
				
				clientAnswerDAO.save(ClientAnswer);
				context.close(); 
			} catch (Exception e){log.error(e.getMessage());e.printStackTrace();}
			try {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
				
				ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
				QuestionWithAnswersDAO QuestionWithAnswersDAO = (QuestionWithAnswersDAO) context.getBean("QuestionWithAnswersDAO");
				QuestionWithAnswers QuestionWithAnswers = QuestionWithAnswersDAO.getById(new Long(getQuestionId()));
				
				if(QuestionWithAnswers.getNextQuestion(this.getAnswer()) != null && QuestionWithAnswers.getNextQuestion(this.getAnswer()).equals("finish")){
					session.setAttribute("qid",100);
				} else {
					session.setAttribute("qid", getQuestionId()+1);
				}
				
				this.setAnswer(null);
				checkDone();
				initQuestion();
			} catch(Exception e){log.error(e.getMessage());e.printStackTrace();}
		} else {
			FacesContext.getCurrentInstance().addMessage(null, new javax.faces.application.FacesMessage("Nepieciešama atbilde. Paldies!"));
		}
	}
}
