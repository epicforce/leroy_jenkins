#!/bin/sh
cd "$1"
echo "Performing preflight check"
"%1/controller" --preflight-check --workflow %2 --environment %3