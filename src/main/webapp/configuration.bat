@echo off

SET CURRENTDIR="%cd%"

SET K="a"

echo "Copying configuration to leroy home"
xcopy /E /R /Y /Q "%CURRENTDIR%" "%1\" > K

cd /d "%1"

IF EXIST "%1\temp-generated_configs." (
	RD /S /Q "%1/temp-generated_configs."
)


echo "Performing configuration"

"%1\controller.exe" --config configurations.xml

RD /S /Q "%CURRENTDIR%\" > K
MKDIR "%CURRENTDIR%"

xcopy /E /R /Y /Q "%1\temp-generated_configs" "%CURRENTDIR%" > K

