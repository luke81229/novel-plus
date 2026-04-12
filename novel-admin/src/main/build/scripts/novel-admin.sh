#!/bin/sh
APP_NAME=novel-admin
JAR_NAME=$APP_NAME\.jar
#PID  代表是PID文件
PID=$APP_NAME\.pid
NOHUP_LOG=logs/${APP_NAME}-nohup.log


#使用说明，用来提示输入参数
usage() {
    echo "Usage: ./novel-admin.sh [start|stop|restart|status]"
    exit 1
}

#检查程序是否在运行
is_exist(){
  pid=`ps -ef|grep $JAR_NAME|grep -v grep|awk '{print $2}' `
  #如果不存在返回1，存在返回0     
  if [ -z "${pid}" ]; then
   return 1
  else
    return 0
  fi
}

#启动方法
start(){
  is_exist
  if [ $? -eq "0" ]; then 
    echo ">>> 小威小说网后台正在运行 PID = ${pid} <<<"
  else 
    echo ">>> 小威小说网后台开始启动 <<<"
    mkdir -p logs
    nohup java -Dspring.profiles.active=prod -jar $JAR_NAME >>"$NOHUP_LOG" 2>&1 &
    echo $! > $PID
    echo ">>> 小威小说网后台已拉起 PID = $(cat $PID)，控制台输出见 $NOHUP_LOG（可 tail -f） <<<"
    sleep 3
    status
   fi
  }

#停止方法
stop(){
  #is_exist
  pidf=$(cat $PID)
  #echo "$pidf"  
  echo ">>> 小威小说网后台 PID = $pidf 开始停止 <<<"
  kill $pidf
  rm -rf $PID
  sleep 2
  is_exist
  if [ $? -eq "0" ]; then 
    echo ">>> 小威小说网后台 PID = $pid 开始强制停止 <<<"
    kill -9  $pid
    sleep 2
    status 
  else
    status
  fi  
}

#输出运行状态
status(){
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> 小威小说网后台正在运行 PID = ${pid} <<<"
  else
    echo ">>> 小威小说网后台没有运行 <<<"
  fi
}

#重启
restart(){
  stop
  start
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
  "start")
    start
    ;;
  "stop")
    stop
    ;;
  "status")
    status
    ;;
  "restart")
    restart
    ;;
  *)
    usage
    ;;
esac
exit 0
