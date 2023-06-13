port="10000"
host="127.0.0.1"
role="none" # 指定するときはすべて大文字
debug="true"
log="false"

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
    l) log="true"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

role=`echo $role | tr "[:lower:]" "[:upper:]"`
if "$log"; then
    filename=log_client/$(date "+%Y-%m-%d_%H.%M.%S").log
    # -u でバッファリング無効
    # tee で標準出力とファイル出力を同時に行う
    python3 -u start.py -p $port -h $host -r $role -d 2>&1 | tee $filename
elif "$debug"; then
    python3 start.py -p $port -h $host -r $role -d
else
    python3 start.py -p $port -h $host -r $role
fi
