#!/usr/bin/env bash
# =============================================================================
# GameCommunityCreator-MVP —— 本地数据库初始化脚本（可重复执行）
#
# 三步：
#   1) CREATE DATABASE IF NOT EXISTS —— database/schema.sql 不含 CREATE DATABASE / USE，
#      库名由调用方传入，因此这里必须先建库；
#   2) 在目标库中执行 database/schema.sql（CREATE TABLE IF NOT EXISTS，幂等）；
#   3) 在目标库中执行 database/seed.sql（先清空演示表再写入固定数据，可重复执行）。
#
# 用法：
#   bash scripts/init-db.sh                 # 默认库 gcc_mvp
#   bash scripts/init-db.sh my_db           # 指定库名
#   DB_NAME=my_db bash scripts/init-db.sh
#
# 可用环境变量覆盖（默认值面向本机开发，部署请用 MySQL 环境变量）：
#   DB_NAME     库名（只允许字母/数字/下划线）  默认 gcc_mvp
#   MYSQL_HOST  主机                          默认 127.0.0.1
#   MYSQL_PORT  端口                          默认 3306
#   MYSQL_USER  用户                          默认 root
#   MYSQL_PWD   密码（推荐，避免出现在命令行历史里）  默认 123456
#   MYSQL_BIN   mysql 客户端路径               默认本机 E:\Code\CodeEditPath\MySQl\SQL9\bin\mysql.exe
#   SCHEMA_FILE / SEED_FILE                   默认 database/schema.sql、database/seed.sql
#
# 说明：schema.sql / seed.sql 都要求「命令行显式传库名」执行（脚本本身不含 USE）；
#       第 1 步只创建空库，不修改任何业务数据。
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

DB_NAME="${DB_NAME:-${1:-gcc_mvp}}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PWD="${MYSQL_PWD:-123456}"
SCHEMA_FILE="${SCHEMA_FILE:-${REPO_ROOT}/database/schema.sql}"
SEED_FILE="${SEED_FILE:-${REPO_ROOT}/database/seed.sql}"

export MYSQL_PWD

# --- 库名合法性校验（也防止把特殊字符拼进 -e 的 SQL 里） -----------------------
if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "[ERROR] 数据库名不合法：${DB_NAME}（只允许字母、数字、下划线）" >&2
  exit 1
fi

# --- 解析 mysql 客户端：环境变量 > PATH > 本机已知路径 -------------------------
if [[ -z "${MYSQL_BIN:-}" ]]; then
  if command -v mysql >/dev/null 2>&1; then
    MYSQL_BIN="$(command -v mysql)"
  elif [[ -x "/e/Code/CodeEditPath/MySQl/SQL9/bin/mysql.exe" ]]; then
    MYSQL_BIN="/e/Code/CodeEditPath/MySQl/SQL9/bin/mysql.exe"
  else
    echo "[ERROR] 未找到 mysql 客户端：请设置 MYSQL_BIN 环境变量指向 mysql 可执行文件。" >&2
    exit 1
  fi
fi

if [[ ! -x "${MYSQL_BIN}" && ! -f "${MYSQL_BIN}" ]]; then
  echo "[ERROR] MYSQL_BIN 不存在或不可执行：${MYSQL_BIN}" >&2
  exit 1
fi
if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "[ERROR] 找不到建表脚本：${SCHEMA_FILE}" >&2
  exit 1
fi
if [[ ! -f "${SEED_FILE}" ]]; then
  echo "[ERROR] 找不到种子数据脚本：${SEED_FILE}" >&2
  exit 1
fi

MYSQL_ARGS=(-h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" --default-character-set=utf8mb4)

echo "== GameCommunityCreator-MVP 数据库初始化 =="
echo "   mysql       : ${MYSQL_BIN}"
echo "   连接        : ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}"
echo "   库名        : ${DB_NAME}"
echo "   建表脚本    : ${SCHEMA_FILE}"
echo "   种子脚本    : ${SEED_FILE}"
echo

echo "[1/3] 创建数据库（若不存在）：${DB_NAME} ..."
"${MYSQL_BIN}" "${MYSQL_ARGS[@]}" \
  -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo "      OK database ${DB_NAME}"

echo "[2/3] 在库 ${DB_NAME} 中执行 database/schema.sql（可重复执行）..."
"${MYSQL_BIN}" "${MYSQL_ARGS[@]}" "${DB_NAME}" < "${SCHEMA_FILE}"
echo "      OK schema.sql"

echo "[3/3] 在库 ${DB_NAME} 中执行 database/seed.sql（重置演示数据，可重复执行）..."
"${MYSQL_BIN}" "${MYSQL_ARGS[@]}" "${DB_NAME}" < "${SEED_FILE}"
echo "      OK seed.sql"

echo
echo "完成：数据库 ${DB_NAME} 已初始化。下次可直接启动后端（DB_URL 默认指向 gcc_mvp）。"
