#!/bin/sh
# MySQL 容器首次初始化时建库（挂到 /docker-entrypoint-initdb.d/ 下）。
#
# 为什么不是直接把 install.sql 挂进 initdb.d：
#   install.sql 用 `source sql/xxx.sql` 按**相对路径**串联十几个脚本，相对的是 mysql 客户端的
#   当前工作目录。所以这里把 sql/ 挂在 /sql、把工作目录切到 /，`source sql/schema.sql` 就正好
#   落在 /sql/schema.sql —— 与"在仓库根目录执行 install.sql"完全等价，install.sql 一个字都不用改。
#
# 只在数据目录为空时执行（官方 entrypoint 的行为），所以重启容器不会重复建库。
#
# 注意：这里刻意**不写 set -e**。官方 entrypoint 对没有可执行位的 *.sh 是用 `.` 引入执行的，
# 那种情况下 set 会污染 entrypoint 自己的 shell；改用显式判断退出码，两种执行方式都安全。
cd / || exit 1

echo "[init-db] 开始建库：install.sql（库名固定 ssy_db，见 sql/*.sql 里的 USE ssy_db）"
mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" < /sql/install.sql || exit 1
echo "[init-db] 建库完成：上面那张自检表的期望值见 DEPLOY.md"
