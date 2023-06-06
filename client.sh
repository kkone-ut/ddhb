port="10000"
host="127.0.0.1"
role="none" # 指定するときはすべて大文字

while getopts ":p:h:r:" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    h) host="$OPTARG"
    ;;
    r) role="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

role=`echo $role | tr "[:lower:]" "[:upper:]"`
python3 start.py -p $port -h $host -r $role