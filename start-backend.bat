@echo off
setlocal
set "JAVA_HOME=C:\workspace\java\jdk17"
set "PATH=C:\workspace\java\jdk17\bin;C:\workspace\java\maven\bin;%PATH%"
cd /d C:\workspace\Yihen-Drama\yihen-drama
call C:\workspace\java\maven\bin\mvn.cmd spring-boot:run
