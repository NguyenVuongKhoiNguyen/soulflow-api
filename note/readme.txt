Start Docker with: docker compose up -d

The sqlserver-init service creates flower_shop only when it does not exist.
When the Spring Boot application starts, Flyway applies migrations from
src/main/resources/db/migration. Do not run flower_shop.sql manually for new setups.

Login and Enter sqlcmd:
docker exec -it sqlserver-db /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "Nguyen@12345" -C
Expected return: 1> 
Then enter sql query without go
then enter go to execute those query
then enter exit to exit sqlcmd
end docker by docker compose down
erase container by docker compose down -v

Legacy manual database creation (not needed when using Docker Compose + Flyway):
docker exec -it sqlserver-db /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "Nguyen@12345" -C -i /init/flower_shop.sql

Clear cache:
docker exec -it redis-cache redis-cli FLUSHALL

Test qwen: 

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

docker exec -it ollama ollama run qwen
/bye for out of qwen

rebuild java code
docker compose up -d --build --force-recreate api 

Set-ExecutionPolicy -Scope Process Bypass; .\download-flowerbasket.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download-bouquest.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download-tableplant.ps1
Set-ExecutionPolicy -Scope Process Bypass; .\download_orchid.ps1


