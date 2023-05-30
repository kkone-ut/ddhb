port=10000
n=15
view=false
game=1
lib=./
log=./log/
setting=./SampleSetting.cfg
client=false

while getopts ":p:n:vg:ch" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    n) n="$OPTARG"
    ;;
    v) view="true"
    ;;
    g) game="$OPTARG"
    ;;
    c) client="true"
    ;;
    h) help="true"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

# help を表示
if [ "$help" = "true" ]; then
    echo "Usage: server.sh [-p port] [-n number] [-v] [-g game] [-c] [-h]"
    echo "  -p port: port number"
    echo "  -n number: number of players"
    echo "  -v: view mode"
    echo "  -g game: number of games"
    echo "  -c: client mode"
    echo "  -h help: show help"
    exit 1
fi

cd AIWolf-ver0.6.3

echo "lib=${lib}" > AutoStarter.ini
echo "log=${log}" >> AutoStarter.ini
echo "port=${port}" >> AutoStarter.ini
echo "game=${game}" >> AutoStarter.ini
echo "view=${view}" >> AutoStarter.ini
echo "setting=${setting}" >> AutoStarter.ini
echo "agent=${n}" >> AutoStarter.ini

for i in $(seq 1 $(($n-1)))
do
    echo "BasketPlayer${i},java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
done

if [ "$client" = "false" ]; then
    echo "BasketPlayer${n},java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
fi

java -cp .:aiwolf-server.jar:aiwolf-common.jar:aiwolf-client.jar:aiwolf-viewer.jar:jsonic-1.3.10.jar org.aiwolf.ui.bin.AutoStarter AutoStarter.ini