@echo off

SET CURRENTDIR="%cd%"

SET K="a"

echo "Copying configuration to leroy home"
xcopy /E /R /Y /Q "%CURRENTDIR%" "%1\" 

cd /d "%1"

IF EXIST "./temp-generated_configs." (
	del "temp-generated_configs." /f /s /q	
)


echo "Performing configuration"

"%1/controller.exe" --generate-configs configurations.xml

RD /S /Q "%CURRENTDIR%\" > K
MKDIR "%CURRENTDIR%"

xcopy /E /R /Y /Q "%1\temp-generated_configs" "%CURRENTDIR%"

