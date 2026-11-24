@echo off
REM ============================================================================
REM GameCommunityCreator-MVP - local database initializer (repeatable)
REM
REM Three steps:
REM   1) CREATE DATABASE IF NOT EXISTS (database\schema.sql contains no
REM      CREATE DATABASE / USE statement, so the database must be created
REM      first and the name passed on the command line);
REM   2) run database\schema.sql inside the target database (idempotent);
REM   3) run database\seed.sql inside the target database (reset demo data).
REM
REM Usage (double click or cmd):
REM   scripts\init-db.bat                -> default database gcc_mvp
REM   scripts\init-db.bat my_db          -> custom database name
REM   scripts\init-db.bat --no-pause     -> non-interactive (no pause at end)
REM
REM Overridable environment variables:
REM   DB_NAME, MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PWD,
REM   MYSQL_BIN, SCHEMA_FILE, SEED_FILE
REM
REM NOTE: keep this file pure ASCII. cmd.exe reads .bat byte by byte, and
REM non-ASCII characters can break the following lines.
REM ============================================================================
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%.."

REM First positional argument is the database name, unless --no-pause was given.
if /I "%~1"=="--no-pause" goto :db_name_ready
if not defined DB_NAME if not "%~1"=="" set "DB_NAME=%~1"
:db_name_ready
if not defined DB_NAME set "DB_NAME=gcc_mvp"
if not defined MYSQL_HOST set "MYSQL_HOST=127.0.0.1"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_USER set "MYSQL_USER=root"
if not defined MYSQL_PWD set "MYSQL_PWD=123456"
if not defined MYSQL_BIN set "MYSQL_BIN=E:\Code\CodeEditPath\MySQl\SQL9\bin\mysql.exe"
if not defined SCHEMA_FILE set "SCHEMA_FILE=%REPO_ROOT%\database\schema.sql"
if not defined SEED_FILE set "SEED_FILE=%REPO_ROOT%\database\seed.sql"

REM Validate database name: only letters, digits and underscore.
echo(%DB_NAME%| findstr /r "^[A-Za-z0-9_][A-Za-z0-9_]*$" >nul
if errorlevel 1 goto :bad_db

REM Resolve mysql client from PATH when the default path is missing.
if exist "%MYSQL_BIN%" goto :have_mysql
where mysql >nul 2>nul
if errorlevel 1 goto :no_mysql
for /f "delims=" %%i in ('where mysql') do set "MYSQL_BIN=%%i"
:have_mysql

if not exist "%MYSQL_BIN%" goto :no_mysql
if not exist "%SCHEMA_FILE%" goto :no_schema
if not exist "%SEED_FILE%" goto :no_seed

set "MYSQL_ARGS=-h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% --default-character-set=utf8mb4"

echo == GameCommunityCreator-MVP database init ==
echo    mysql    : %MYSQL_BIN%
echo    server   : %MYSQL_USER%@%MYSQL_HOST%:%MYSQL_PORT%
echo    database : %DB_NAME%
echo    schema   : %SCHEMA_FILE%
echo    seed     : %SEED_FILE%
echo.

echo [1/3] Creating database if not exists: %DB_NAME% ...
"%MYSQL_BIN%" %MYSQL_ARGS% -e "CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 goto :failed
echo       OK database %DB_NAME%

echo [2/3] Running database\schema.sql in database %DB_NAME% ...
"%MYSQL_BIN%" %MYSQL_ARGS% %DB_NAME% < "%SCHEMA_FILE%"
if errorlevel 1 goto :failed
echo       OK schema.sql

echo [3/3] Running database\seed.sql in database %DB_NAME% ...
"%MYSQL_BIN%" %MYSQL_ARGS% %DB_NAME% < "%SEED_FILE%"
if errorlevel 1 goto :failed
echo       OK seed.sql

echo.
echo Done. Database "%DB_NAME%" is ready.
echo.
if /I "%~1"=="--no-pause" exit /b 0
pause
exit /b 0

:bad_db
echo [ERROR] Invalid database name: %DB_NAME% (only letters, digits and underscore).
exit /b 1

:no_mysql
echo [ERROR] mysql client not found. Set MYSQL_BIN to the mysql executable path.
exit /b 1

:no_schema
echo [ERROR] schema file not found: %SCHEMA_FILE%
exit /b 1

:no_seed
echo [ERROR] seed file not found: %SEED_FILE%
exit /b 1

:failed
echo.
echo [ERROR] database initialization failed (see mysql output above).
exit /b 1
