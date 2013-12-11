#!/bin/bash

set -o nounset

function usage() {
  echo "Usage: $0 (-h|-d|-n) -a [auth module] -l [dir] -s [dir] -c [name] -z [url] -p [path] (-k [keyfile])"
  echo " -a        auth module (default unsecure auth module: com.twitter.aurora.auth.UnsecureAuthModule)"
  echo " -c [name] cluster name ('${LOCAL_CLUSTER_NAME}' for local workstation runs)"
  echo " -d        start a debugger if running locally"
  echo " -e [zone] "
  echo " -h        print out this help message"
  echo " -k [path] path to keyfile for negotiating SSL connections (default /usr/local/mesos/keys/MesosKeyStore.[name])"
  echo " -l [dir]  log base directory"
  echo " -n        don't run, just print out commands that would be run"
  echo " -p [path] path to use for pid file (optional and ignored for local runs)"
  echo " -s [path] path for scheduler database files to live under"
  echo " -u [name] cluster name (on UI)"
  echo " -z [url]  mesos-master zookeeper registration url (optional and ignored for local runs)"
  if (( $# > 0 ))
  then
    echo
    echo "$@"
    exit 1
  else
    exit 0
  fi
}

cd $(dirname $0)
HERE=$(pwd)

dc="local"
LOCAL_CLUSTER_NAME="local"
lib_dir=/usr/local/lib
keyfile="${HERE}/../../dist/resources/test/com/twitter/aurora/scheduler/app/AuroraTestKeyStore"
thrift_port=0
http_port=8081
master_zoo_url="local"
thermos_executor_path="${HERE}/../../dist/thermos_executor.pex"
gc_executor_path="${HERE}/../../../dist/gc_executor.pex"
native_log_quorum_size=1
## EMPTY??
user_capabilities="ROOT=aurora"
zk_in_proc="true"
zk_endpoints="localhost:0"
require_slave_checkpoint="true"

while getopts "dhna:c:e:k:l:p:s:z:" opt
do
  case ${opt} in
    a) auth_module=${OPTARG} ;;
    c) cluster_name=${OPTARG} ;;
    d) debug="true" ;;
    e) dc=${OPTARG} ;;
    h) usage ;;
    k) keyfile=${OPTARG} ;;
    l) log_dir=${OPTARG} ;;
    n) dry_run="true" ;;
    p) pidfile=${OPTARG} ;;
    s) native_log_file_path=${OPTARG}/mesos_log ;;
    u) cluster_ui_name=${OPTARG} ;;
    z) master_zoo_url=${OPTARG} ;;
    *) usage "Invalid option: -${OPTARG}" ;;
  esac
done
if (( $OPTIND > 1 ))
then
  shift $(($OPTIND - 1))
fi

java_launcher=(
  java
)

additional_flags=""

backup_dir="/var/lib/mesos/scheduler_backups"

# Specify the serverset address to use.
case ${cluster_name:-''} in
  local|dev1|dev2|test|nonprod|prod) ;;
  '') usage 'Cluster name must be specified with -c' ;;
  *)  usage 'Cluster name must be one of [local]' ;;
esac
cluster_ui_name=${cluster_ui_name:-$cluster_name}
serverset_path="/twitter/service/mesos/$cluster_name/scheduler"

####Local cluster options.
# Provide defaults to make running a local scheduler easy.
log_dir=${log_dir:-/tmp}
native_log_file_path=${native_log_file_path:-/dev/null}
additional_flags=(
-testing_isolated_scheduler=true
-testing_log_file_path=/tmp/testing_log_file
)
thrift_port=55555

pidfile=/dev/null
backup_dir="/tmp"
native_log_zk_group_path="/local/service/mesos-native-log"
jar="${HERE}/../../dist/libs/incubator-aurora.jar"
.//dist/libs/incubator-aurora.jar
# override for local
if [ "${keyfile}" = "" ]; then
keyfile="${HERE}/../../../tests/resources/com/twitter/aurora/internal/AuroraTestKeyStore"
fi

if [ "${dry_run:-}" = "true" ]
then
java_launcher=(
echo
${java_launcher[@]}
)
fi

java_launcher=(
exec
${java_launcher[@]}
-Xms2g
-Xmx2g
)

if [ "${debug:-}" = "true" ]
then
export LOCAL_MESOS_DEBUG=true
java_launcher=(
${java_launcher[@]}
-Xdebug
-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
)
fi

export LOCAL_MESOS_LOGS=${log_dir}
export MESOS_RESOURCES="cpus:2;mem:2048;ports:[50000-60000];disk:4000"

export JVM_OPTS="-Djava.library.path=$lib_dir  -Djava.util.logging.manager=com.twitter.common.util.logging.UnresettableLogManager -Xms2g -Xmx2g"
${HERE}/../../dist/install/aurora-scheduler/bin/aurora-scheduler \
    -thermos_executor_path=${thermos_executor_path} \
    -gc_executor_path=${gc_executor_path} \
    -http_port=${http_port} \
    -zk_in_proc=${zk_in_proc} \
    -zk_endpoints=${zk_endpoints} \
    -zk_session_timeout=10secs \
    -zk_digest_credentials=mesos:mesos \
    -serverset_path=${serverset_path} \
    -mesos_master_address=${master_zoo_url} \
    -log_dir=${log_dir} \
    -mesos_ssl_keyfile=${keyfile} \
    -cluster_name=${cluster_ui_name} \
    -thrift_port=${thrift_port} \
    -user_capabilities=${user_capabilities} \
    -native_log_quorum_size=${native_log_quorum_size} \
    -native_log_file_path=${native_log_file_path} \
    -native_log_zk_group_path=${native_log_zk_group_path} \
    -backup_dir=${backup_dir} \
    -logtostderr \
    -vlog=INFO \
    -require_slave_checkpoint=${require_slave_checkpoint} \
    -viz_job_url_prefix=https://viz.${dc}.twitter.com/dashboards/mesos-container-stats?source=sd. \
    ${additional_flags[@]} \
    $@
