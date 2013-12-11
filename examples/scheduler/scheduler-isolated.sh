#!/bin/bash

set -o nounset

#TODO: Check missing dist, else run installApp

cd $(dirname $0)
DIST="$(pwd)/../../dist"

keyfile="${DIST}/resources/test/com/twitter/aurora/scheduler/app/AuroraTestKeyStore"
http_port=8081
master_zoo_url="local"
thermos_executor_path="${DIST}/thermos_executor.pex"
gc_executor_path="${DIST}/gc_executor.pex"
user_capabilities="ROOT=aurora"
zk_in_proc="true"
zk_endpoints="localhost:0"
require_slave_checkpoint="true"
thrift_port=55555

# Specify the serverset address to use.
cluster_name="local"
serverset_path="/twitter/service/mesos/$cluster_name/scheduler"

log_dir="/tmp"
native_log_zk_group_path="/local/service/mesos-native-log"
debug_opts=""

# TEST??????
while getopts "dh" opt
do
  case ${opt} in
    d) debug="true" ;;
    h) usage ;;
    *) usage "Invalid option: -${OPTARG}" ;;
  esac
done
if (( $OPTIND > 1 ))
then
  shift $(($OPTIND - 1))
fi

if [ "${debug:-}" = "true" ]
then
export LOCAL_MESOS_DEBUG=true
debug_opts=(
${java_launcher[@]}
-Xdebug
-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
)
fi

export LOCAL_MESOS_LOGS=${log_dir}
export MESOS_RESOURCES="cpus:2;mem:2048;ports:[50000-60000];disk:4000"

export JVM_OPTS="-Djava.util.logging.manager=com.twitter.common.util.logging.UnresettableLogManager -Xms2g -Xmx2g ${debug_opts}" 

${DIST}/install/aurora-scheduler/bin/aurora-scheduler \
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
    -cluster_name=${cluster_name} \
    -thrift_port=${thrift_port} \
    -user_capabilities=${user_capabilities} \
    -native_log_quorum_size=1\
    -native_log_file_path=/dev/null \
    -native_log_zk_group_path=${native_log_zk_group_path} \
    -backup_dir=/tmp \
    -logtostderr \
    -vlog=INFO \
    -require_slave_checkpoint=${require_slave_checkpoint} \
    -viz_job_url_prefix=https://localhost/mesos-container-stats?source=sd. \
    -testing_isolated_scheduler=true \
    -testing_log_file_path=/tmp/testing_log_file \
    $@
