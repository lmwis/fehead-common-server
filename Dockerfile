FROM java:8

MAINTAINER lmwis <lmwis2000ygn@gmail.com>

ADD target/fehead-common-service-1.0-SNAPSHOT.jar /home/fehead/app.jar

# 设置容器启动时执行的操作
ENTRYPOINT ["nohup","java","-jar","/home/fehead/app.jar","&"]
