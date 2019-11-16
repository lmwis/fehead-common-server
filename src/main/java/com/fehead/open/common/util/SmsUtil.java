package com.fehead.open.common.util;


import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fehead.lang.config.FeheadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
 * @author Nightnessss 2019/7/20 17:03
 */
@Component
@RequiredArgsConstructor
public class SmsUtil {

    private final FeheadProperties securityProperties;

    private final ObjectMapper objectMapper;

    /**
     * 新版本：阿里云云通信发送短信通知
     * @param modelName 短信模板名，传入的模板必须是在阿里大于“管理中心-短信模板管理”中的可用模板。示例：sms.TemplateNotice
     * @param modelParam   模板内容里面的变量
     * @param phone   用户手机号码
     * @return boolean true成功false失败
     */
    public  boolean sendSms(String modelName, Map<String, String> modelParam, String phone) throws JsonProcessingException {

        boolean result = false;

        String appKey = securityProperties.getSmsProperties().getAppKey();
        String secret = securityProperties.getSmsProperties().getSecret();
        String regionId = securityProperties.getSmsProperties().getRegionId();
        String signName = securityProperties.getSmsProperties().getSignName();

        DefaultProfile profile = DefaultProfile.getProfile(regionId, appKey, secret);
        IAcsClient client = new DefaultAcsClient(profile);


        CommonRequest request = new CommonRequest();
        //request.setProtocol(ProtocolType.HTTPS);
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", regionId);
        request.putQueryParameter("PhoneNumbers", phone);
        request.putQueryParameter("SignName", signName);
        request.putQueryParameter("TemplateCode", modelName);
        request.putQueryParameter("TemplateParam", objectMapper.writeValueAsString(modelParam));
        System.out.println("modelParam: " + objectMapper.writeValueAsString(modelParam)) ;
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            result = true;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }

        return result;
    }
}
