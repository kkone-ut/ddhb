port="10000"
host="127.0.0.1"
role="WEREWOLF" # 指定するときはすべて大文字
times=1

while getopts ":p:h:r:t:" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    h) host="$OPTARG"
    ;;
    r) role="$OPTARG"
    ;;
    t) times="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

role=`echo $role | tr "[:lower:]" "[:upper:]"`

for i in $(seq 1 $times)
do
  latest_commit=$(git log -1 --pretty=format:"%H")
  short_commit=${latest_commit:0:7}
  date=`date "+%Y-%m-%d_%H.%M.%S"`
  filename=log_client/${date}_${short_commit}.log
  # -u でバッファリング無効
  # tee で標準出力とファイル出力を同時に行う
  python3 -u start.py -p $port -h $host -r $role 2>&1 | tee $filename
  exit_status=$?
  if (( exit_status != 0)); then
    break
  fi
  if (( i != times )); then
    sleep 5
  fi
done