package com.fehead.open.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fehead.lang.properties.FeheadProperties;
import com.fehead.lang.error.BusinessException;
import com.fehead.lang.error.EmBusinessError;
import com.fehead.open.common.service.RedisService;
import com.fehead.open.common.service.SmsService;
import com.fehead.open.common.service.model.ValidateCode;
import com.fehead.open.common.util.CreateCodeUtil;
import com.fehead.open.common.util.SmsUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
 * @author Nightnessss 2019/8/11 17:38
 */
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    final SmsUtil smsUtil;

    private final PasswordEncoder passwordEncoder;

    private final FeheadProperties feheadProperties;

    private final RedisService redisService;

    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Override
    public boolean check(String key) throws BusinessException {

        boolean result = false;
        // 检查验证码在60秒内是否已经发送
        if (redisService.exists(key)) {
            result = true;
        }

        return result;
    }

    @Override
    public void send(String telphone, Integer modelId) throws BusinessException, JsonProcessingException {
        Map<String, String> paramMap = new HashMap<>();
        ValidateCode smsCode = CreateCodeUtil.createCode(telphone, 6);
        paramMap.put("code", smsCode.getCode());
        String modelName = "";
        try {
            modelName = feheadProperties.getSmsProperties().getSmsModel().get(modelId).getName();
        } catch (Exception e) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        logger.info("action：" + feheadProperties.getSmsProperties().getSmsModel().get(modelId).getDes());
        logger.info("模板：" + modelName);
        logger.info("验证码：" + smsCode.getCode());
        smsCode.encode(passwordEncoder);
        logger.info("encode:" + smsCode.getCode());
        switch (modelId) {
            case 0:
//                String key = passwordEncoder.encode(telphone);
//                logger.info("sms_key: " + key);
//                redisService.set("sms_key_" + telphone, key, new Long(300));
                redisService.set(feheadProperties.getSmsProperties().getRegisterPreKeyInRedis() + smsCode.getTelphone(), smsCode, new Long(30*60));
                break;
            case 1:
                redisService.set(feheadProperties.getSmsProperties().getLoginPreKeyInRedis() + smsCode.getTelphone(), smsCode, new Long(30*60));
                break;
            case 2:
                redisService.set(feheadProperties.getSmsProperties().getResetPreKeyInRedis() + smsCode.getTelphone(), smsCode, new Long(30*60));
                break;
            default:
                break;
        }
        // 发送短信
        smsUtil.sendSms(modelName, paramMap, telphone);
    }
}
