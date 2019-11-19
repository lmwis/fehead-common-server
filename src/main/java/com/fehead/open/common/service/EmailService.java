package com.fehead.open.common.service;

import com.fehead.lang.error.BusinessException;
import javax.mail.MessagingException;

/**
 * @Description:
 * @Author lmwis
 * @Date 2019-11-16 19:19
 * @Version 1.0
 */
public interface EmailService {

    public void sendValidateEmail(String toAddress) throws BusinessException, MessagingException;

    boolean validateEmailCode(String yourEmail, String code) throws BusinessException;
}
