@echo off
cd /d E:\claude\montessori\backend
set DB_PORT=5433
set REDIS_PORT=6380
call mvnw.cmd spring-boot:run
