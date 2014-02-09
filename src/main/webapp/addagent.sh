#!/bin/sh

cd "$1"

cat "$1/agentdata.txt" | "$1/controller" --addagent $2
