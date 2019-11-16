package com.fehead.open.common.controller;

import com.fehead.lang.config.FeheadProperties;
import com.fehead.lang.controller.BaseController;
import com.fehead.lang.error.BusinessException;
import com.fehead.lang.error.EmBusinessError;
import com.fehead.lang.response.AuthenticationReturnType;
import com.fehead.lang.response.CommonReturnType;
import com.fehead.lang.response.FeheadResponse;
import com.fehead.open.common.service.EmailService;
import com.fehead.open.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Description:
 * @Author lmwis
 * @Date 2019-11-16 19:10
 * @Version 1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/email")
public class EmailController extends BaseController {

    private final EmailService eMailService;

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final RedisService redisService;

    private final PasswordEncoder passwordEncoder;

    private final FeheadProperties feheadProperties;

    /**
     * 发送校验邮件
     * @param request
     * @param response
     * @return
     * @throws MessagingException
     * @throws BusinessException
     * @throws IOException
     * @throws ServletException
     */
    @PostMapping("/send")
    public FeheadResponse sendAuthenticationEmail(HttpServletRequest request, HttpServletResponse response) throws MessagingException, BusinessException, com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException {

        String toAddress = request.getParameter("address");
        String action = request.getParameter("action");

        if(StringUtils.isEmpty(toAddress)){
//            feheadAuthenticationFailureHandler.onAuthenticationFailure(request,response,new SmsValidateException(EmBusinessError.EMAIL_TO_EMPTY));
            logger.info("接收地址为空");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "接收地址为空");
        }

        // 检查行为是否为空
        if (action.isEmpty()) {
            logger.info("action为空");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "action为空");
        }

        logger.info("调用发送邮件接口");


        // 目前登录注册的验证邮件格式相同，暂不做区分
        if (action.equals("login")) {
            eMailService.sendValidateEmail(toAddress);
        } else if (action.equals("register")) {
            //发送注册校验邮箱
            eMailService.sendValidateEmail(toAddress);
        } else {
            logger.info("action参数不合法");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "action参数不合法");
        }

        return CommonReturnType.create("发送成功");
    }

    /**
     * 邮件校验
     * @param address
     * @param code
     * @return
     * @throws BusinessException
     */
    @PostMapping("/validate")
    public FeheadResponse validateEmailCode(String address,String code) throws BusinessException {

        String smsKey = "";
        if(StringUtils.isEmpty(address) || StringUtils.isEmpty(code)){
            return AuthenticationReturnType.create(EmBusinessError.EMAIL_TO_EMPTY.getErrorMsg(), HttpStatus.PAYMENT_REQUIRED.value());
        }

        if (eMailService.validateEmailCode(address,code)) {
            smsKey = passwordEncoder.encode(address);
            logger.info("密钥：" + smsKey);
            redisService.set("sms_key_"+ address, smsKey, (long)feheadProperties.getTimeProperties().getSmsKeyExpiredTime());
        } else {
            logger.info("验证码不匹配");
            throw new BusinessException(EmBusinessError.SMS_ILLEGAL);
        }
        return CommonReturnType.create(smsKey);
    }

}
