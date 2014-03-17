@echo off

SET CURRENTDIR="%cd%"

SET K="a"

cd /d "%1"

echo "Performing preflight check"
"%1/controller.exe" --preflight-check --workflow %2 --environment %3