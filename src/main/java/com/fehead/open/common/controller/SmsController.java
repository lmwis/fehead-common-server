package com.fehead.open.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fehead.lang.properties.FeheadProperties;
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
    public FeheadResponse sendSms(@RequestParam("tel")String tel,@RequestParam("action")String action) throws BusinessException, JsonProcessingException {

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
            if (smsService.check(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + tel)) {
                ValidateCode code = (ValidateCode) redisService.get(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + tel);
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

        // 根据行为选择模板发送短信  0为注册模板，1为登录模板，2为重置模版
        if (action.equals(SmsAction.LOGIN.actionStr)) {
            smsService.send(tel, 1);
        } else if (action.equals(SmsAction.REGISTER.actionStr)) {
            smsService.send(tel, 0);
        } else if(action.equals(SmsAction.RESET.actionStr)){
            smsService.send(tel, 2);
        }else {
            logger.info("action异常");
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "action异常");
        }

        return CommonReturnType.create(tel);

    }

    /**
     * 对手机号和验证码进行校验
     * @param tel
     * @param code
     * @return
     * @throws BusinessException
     */
    @PutMapping(value = "/validate")
    public FeheadResponse validateSms(@RequestParam("tel")String tel, @RequestParam("code")String code) throws BusinessException {

        String smsKey = "";
        logger.info("手机号：" + tel);
        logger.info("验证码：" + code);
        if (!CheckEmailAndTelphoneUtil.checkTelphone(tel)) {
            logger.info("手机号不合法");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号不合法");
        }
        if (code.isEmpty()) {
            logger.info("验证码为空");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码为空");
        }
        if (registerValidate(tel, code)) { // 校验成功

            return CommonReturnType.create(tel);
            // 不需要额外密钥了
//            smsKey = passwordEncoder.encode(tel);
//            logger.info("密钥：" + smsKey);
//            redisService.set("sms_key_"+ tel, smsKey, (long)30*60);
        }else{
            throw new BusinessException(EmBusinessError.SMS_ILLEGAL);
        }




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
