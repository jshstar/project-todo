package com.sparta.project_todo.email.service;

import java.util.Random;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sparta.project_todo.email.entity.EmailMessage;
import com.sparta.project_todo.redis.util.RedisUtil;
import com.sparta.project_todo.user.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender javaMailSender;
	private final SpringTemplateEngine templateEngine;
	private final RedisUtil redisUtil;
	public void sendMail(EmailMessage emailMessage, String type) {
		String authNum = createCode();

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();

		try {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
			mimeMessageHelper.setTo(emailMessage.getTo()); // 메일 수신자
			mimeMessageHelper.setSubject(emailMessage.getSubject()); // 메일 제목
			mimeMessageHelper.setText(setContext(authNum, type), true); // 메일 본문 내용, HTML 여부
			javaMailSender.send(mimeMessage);
			redisUtil.setDataExpire(emailMessage.getTo(),authNum, 60*5L);
			log.info("Success");

			// return authNum;

		} catch (MessagingException e) {
			log.info("fail");
			throw new RuntimeException(e);
		}
	}

	// 인증번호 생성
	public String createCode() {
		Random random = new Random();
		StringBuffer key = new StringBuffer();

		for (int i = 0; i < 8; i++) {
			int index = random.nextInt(4);

			switch (index) {
				case 0: key.append((char) ((int) random.nextInt(26) + 97)); break;
				case 1: key.append((char) ((int) random.nextInt(26) + 65)); break;
				default: key.append(random.nextInt(9));
			}
		}
		return key.toString();
	}

	// thymeleaf를 통한 html 적용
	public String setContext(String code, String type) {
		Context context = new Context();
		context.setVariable("code", code);
		return templateEngine.process(type, context);
	}

	public Boolean verifyEmailCode(String email, String code){
		String findCodeByEmail = redisUtil.getData(email);
		if(findCodeByEmail == null){
			return null;
		} else{
			return findCodeByEmail.equals(code);
		}

	}

}
