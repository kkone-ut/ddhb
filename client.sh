port="10000"
host="127.0.0.1"
role="none" # 指定するときはすべて大文字
debug="true"

while getopts ":p:h:r:d" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    h) host="$OPTARG"
    ;;
    r) role="$OPTARG"
    ;;
    d) debug="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

role=`echo $role | tr "[:lower:]" "[:upper:]"`
if "$debug"; then
    python3 start.py -p $port -h $host -r $role -d
else
    python3 start.py -p $port -h $host -r $role
fi
