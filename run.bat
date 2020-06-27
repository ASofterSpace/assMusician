@echo off

cd /D %~dp0

java -classpath "%~dp0\bin" -Xms256m -Xmx8192m com.asofterspace.assMusician.AssMusician %*

pause
