@echo off

cd /D %~dp0

java -classpath "%~dp0\bin" -Xms16m -Xmx4096m com.asofterspace.assMusician.AssMusician %*

pause
