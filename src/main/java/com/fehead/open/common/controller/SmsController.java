package com.fehead.open.common.controller;

import com.fehead.lang.config.FeheadProperties;
import com.fehead.lang.controller.BaseController;
import com.fehead.lang.error.BusinessException;
import com.fehead.lang.error.EmBusinessError;
import com.fehead.lang.response.CommonReturnType;
import com.fehead.lang.response.FeheadResponse;
import com.fehead.open.common.service.RedisService;
import com.fehead.open.common.service.SmsService;
import com.fehead.open.common.service.model.ValidateCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import util.CheckEmailAndTelphoneUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description:
 * @Author lmwis
 * @Date 2019-11-15 19:56
 * @Version 1.0
 */
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController extends BaseController {
    enum SmsAction{
        REGISTER("register"),
        LOGIN("login"),
        RESET("reset")
        ;
        private String actionStr;
        SmsAction(String actionStr) {
            this.actionStr = actionStr;
        }
        public String value() {
            return actionStr;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);


    private final RedisService redisService;

    private final FeheadProperties feheadProperties;

    private final SmsService smsService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 提供手机号和当前行为，根据行为发送相应类型短信
     * @param tel
     * @param action
     * @return
     * @throws BusinessException
     */
    @PostMapping(value = "/send")
    public FeheadResponse sendSms(@RequestParam("tel")String tel,@RequestParam("action")String action) throws BusinessException {

        // 检查手机号是否合法
        if (!CheckEmailAndTelphoneUtil.checkTelphone(tel)) {
            logger.info("手机号不合法");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号不合法");
        }

        // 检查验证码在60秒内是否已经发送
        if (action.equals(SmsAction.REGISTER.actionStr)) {
            if (smsService.check(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + tel)) {
                ValidateCode code = (ValidateCode) redisService.get(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + tel);
                if (!code.isExpired(60)) {
                    logger.info("验证码已发送");
                    throw new BusinessException(EmBusinessError.SMS_ALREADY_SEND);
                } else {
                    redisService.remove(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + tel);
                }
            }
        } else if (action.equals(SmsAction.LOGIN.actionStr)) {
            if (smsService.check(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + tel)) {
                ValidateCode code = (ValidateCode) redisService.get(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + tel);
                if (!code.isExpired(60)) {
                    logger.info("验证码已发送");
                    throw new BusinessException(EmBusinessError.SMS_ALREADY_SEND);
                } else {
                    redisService.remove(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + tel);
                }
            }
        } else if (action.equals(SmsAction.RESET.actionStr)) {
            if (smsService.check(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + tel)) {
                ValidateCode code = (ValidateCode) redisService.get(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + tel);
                if (!code.isExpired(60)) {
                    logger.info("验证码已发送");
                    throw new BusinessException(EmBusinessError.SMS_ALREADY_SEND);
                } else {
                    redisService.remove(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + tel);
                }
            }
        }else {
            logger.info("action异常");
            throw new BusinessException(EmBusinessError.OPERATION_ILLEGAL, "action异常");
        }

        return null;

    }

    /**
     * 对手机号和验证码进行校验
     * @param request
     * @param response
     * @return
     * @throws BusinessException
     */
    @PutMapping(value = "/validate")
    public FeheadResponse validateSms(HttpServletRequest request, HttpServletResponse response) throws BusinessException {

        String telphoneInRequest = request.getParameter("tel");
        String codeInRequest = request.getParameter("code");
        String smsKey = "";
        logger.info("手机号：" + telphoneInRequest);
        logger.info("验证码：" + codeInRequest);
        if (!CheckEmailAndTelphoneUtil.checkTelphone(telphoneInRequest)) {
            logger.info("手机号不合法");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号不合法");
        }
        if (codeInRequest.isEmpty()) {
            logger.info("验证码为空");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码为空");
        }
        if (registerValidate(telphoneInRequest, codeInRequest)) {
            smsKey = passwordEncoder.encode(telphoneInRequest);
            logger.info("密钥：" + smsKey);
            redisService.set("sms_key_"+ telphoneInRequest, smsKey, new Long(30*60));
        }

        return CommonReturnType.create(smsKey);
    }
    private boolean registerValidate(String telphoneInRequest, String codeInRequest) throws BusinessException {
        ValidateCode smsCode = new ValidateCode();

        // 检查redis中是否存有该手机号验证码
        if (!redisService.exists(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + telphoneInRequest)) {
            if (!redisService.exists(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + telphoneInRequest)) {
                logger.info("验证码不存在");
                throw new BusinessException(EmBusinessError.SMS_ISNULL);
            }else{
                smsCode = (ValidateCode)redisService.get(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + telphoneInRequest);
            }
        }else {
            smsCode = (ValidateCode)redisService.get(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + telphoneInRequest);
        }


        if (StringUtils.isBlank(codeInRequest)) {
            logger.info("验证码不能为空");
            throw new BusinessException(EmBusinessError.SMS_BLANK);
        }

        if (smsCode == null) {
            logger.info("验证码不存在");
            throw new BusinessException(EmBusinessError.SMS_ISNULL);
        }


        if (!passwordEncoder.matches(codeInRequest, smsCode.getCode())) {
            logger.info("验证码不匹配");
            throw new BusinessException(EmBusinessError.SMS_ILLEGAL);
        }

        redisService.remove(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + telphoneInRequest);
        redisService.remove(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + telphoneInRequest);


        return true;
    }

}
