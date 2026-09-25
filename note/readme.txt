
    --Clear cache:
docker exec -it redis-cache redis-cli FLUSHALL

    --Test qwen: 

curl http://localhost:11434/api/chat -d "{
    \"model\": \"qwen\",
    \"messages\": [
        {
        \"role\": \"user\",
        \"content\": \"Who im i?\"
        } 
    ],
    \"stream\": false
}"


Set-ExecutionPolicy -Scope Process Bypass; .\download-flowerbasket.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download-bouquest.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download-tableplant.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download_orchid.ps1


