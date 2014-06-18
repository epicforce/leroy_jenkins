@echo off

SET CURRENTDIR="%cd%"

SET K="a"

cd /d "%4"

xcopy /E /R /Y /Q "%1" "%4" 

echo "[INFO] Deploying workflow: %2%"
echo "[INFO] Deploying environment: %3%"
"controller.exe" --workflow %2 --environment %3
