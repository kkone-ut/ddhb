port="10000"
host="127.0.0.1"
role="none" # 指定するときはすべて大文字
debug="true"
loop="false"

while getopts ":p:h:r:dl" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    h) host="$OPTARG"
    ;;
    r) role="$OPTARG"
    ;;
    d) debug="$OPTARG"
    ;;
    l) loop="true"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

role=`echo $role | tr "[:lower:]" "[:upper:]"`

while true
do
  latest_commit=$(git log -1 --pretty=format:"%H")
  short_commit=${latest_commit:0:7}
  date=`date "+%Y-%m-%d_%H.%M.%S"`
  filename=log_client/${short_commit}_${date}.log
  # -u でバッファリング無効
  # tee で標準出力とファイル出力を同時に行う
  python3 -u start.py -p $port -h $host -r $role -d 2>&1 | tee $filename
  exit_status=$?
  if [[ "$loop" == "false" ]] || (( exit_status != 0)) ; then
    break
  fi
  sleep 5
done