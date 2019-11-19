
package com.fehead.open.common.util;


import com.fehead.lang.properties.FeheadProperties;
import com.fehead.lang.error.BusinessException;
import com.fehead.lang.error.EmBusinessError;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 写代码 敲快乐
 * だからよ...止まるんじゃねぇぞ
 * ▏n
 * █▏　､⺍
 * █▏ ⺰ʷʷｨ
 * █◣▄██◣
 * ◥██████▋
 * 　◥████ █▎
 * 　　███▉ █▎
 * 　◢████◣⌠ₘ℩
 * 　　██◥█◣\≫
 * 　　██　◥█◣
 * 　　█▉　　█▊
 * 　　█▊　　█▊
 * 　　█▊　　█▋
 * 　　 █▏　　█▙
 * 　　 █
 *
 * @author Nightnessss 2019/7/23 12:00
 */
@Component
@RequiredArgsConstructor
public class SendEmailUtil {

    private final JavaMailSender javaMailSender;

    private final TemplateEngine templateEngine;

    private final FeheadProperties feheadProperties;

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 发送邮件
     * @param toAddress 接收人的邮箱地址
     * @param params 参数变量的值
     * @param subject 邮件标题
     * @param templateName HTML的名字（不含.html）
     * @throws MessagingException
     */
    public void sendEmail(String toAddress, Map<String,String> params, String subject, String templateName) throws MessagingException, BusinessException {

        if (StringUtils.isEmpty(toAddress)) {
            logger.info("收件人地址为空");
            throw new BusinessException(EmBusinessError.EMAIL_TO_EMPTY);
        }
        if (StringUtils.isEmpty(subject)) {
            logger.info("邮件标题为空");
            throw new BusinessException(EmBusinessError.EMAIL_TITLE_EMPTY);
        }
        if (StringUtils.isEmpty(templateName)) {
            logger.info("HTML邮件不存在");
            throw new BusinessException(EmBusinessError.EMAIL_TEMPLATE_NOT_EXIST);
        }

        Context context = new Context();
        params.forEach((k,v) -> {
            context.setVariable(k,v);
            context.setVariable(k,v);
        });

        String emailContent = "";
        try {
            emailContent = templateEngine.process(templateName, context);
        } catch (Exception e) {
            logger.info("HTML邮件不存在: " + e.getMessage());
            throw new BusinessException(EmBusinessError.EMAIL_TEMPLATE_NOT_EXIST);
        }
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String fromAddress = feheadProperties.getEmailProperties().getFromAddress();
        helper.setFrom(fromAddress);
        helper.setTo(toAddress);
        helper.setCc(fromAddress);
        helper.setSubject(subject);
        helper.setText(emailContent, true);
        logger.info("FROM " + fromAddress + " TO " + toAddress);
        logger.info("TITLE: " + subject);
        try {
            // 调用api发送邮件
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.info("发送失败: " + e.getMessage());
            throw new BusinessException(EmBusinessError.EMAIL_SEND_FAILURE);
        }
    }
}