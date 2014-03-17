@echo off

SET CURRENTDIR="%cd%"

SET K="a"

RD /S /Q "%CURRENTDIR%\Win64" > K

echo "unzip"
jar xf leroy_Win64.zip

echo "Kill Task"
TASKKILL /IM controller.exe /F > K

echo "Copying configuration to leroy home"
XCOPY /E /R /Y /Q /S /I "%CURRENTDIR%\Win64" "%1" 