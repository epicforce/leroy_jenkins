@echo off

SET CURRENTDIR="%cd%"

SET K="a"

cd /d "%4"

xcopy /E /R /Y /Q "%1" "%4" 

echo "Performing deploy"
"controller.exe" --workflow %2 --environment %3