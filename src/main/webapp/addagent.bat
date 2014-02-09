c:
cd "%1"
ECHO "%1"
TYPE "%1\agentdata.txt" | "%2" --addagent