package com.fehead.open.common.controller;

import com.fehead.lang.controller.BaseController;
import com.fehead.lang.error.BusinessException;
import com.fehead.lang.error.EmBusinessError;
import com.fehead.lang.response.CommonReturnType;
import com.fehead.lang.response.FeheadResponse;
import com.fehead.open.common.service.EmailService;
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/send")
    public FeheadResponse sendAuthenticationEmail(HttpServletRequest request, HttpServletResponse response) throws MessagingException, BusinessException, IOException, ServletException {

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

}
