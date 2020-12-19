# fehead-common-service
Fehead平台通用服务
## /email 
邮件相关
#### /send: 发送校验邮箱
    address 发送至此地址 | action (login或者register)
####/validate: 邮箱校验
    address 发送至此地址 | code 校验码
## /sms 
短信相关
#### /send
提供手机号和当前行为，根据行为发送相应类型短信
param:
- tel:string 手机号
- action:string 枚举值，有(login|register|reset)
return:
- 发送成功返回200和手机号
- 其他异常信息
#### /validate
对手机号和验证码进行校验
return:
- 校验成功返回200和手机号
- 其他异常信息
    