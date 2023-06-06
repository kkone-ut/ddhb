port=10000
n=15
view=false
game=1
lib=./
log=./log/
setting=./SampleSetting.cfg
client=false
loop=false

help=false

while getopts ":p:n:vg:clh" opt; do
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
    l) loop="true"
    ;;
    h) help="true"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

# help を表示
if "$help"; then
    echo "Usage: server.sh [-p port] [-n number] [-v] [-g game] [-c] [-h]"
    echo "  -p port: port number"
    echo "  -n number: number of players"
    echo "  -v: view mode"
    echo "  -g game: number of games"
    echo "  -c: client mode"
    echo "  -l: loop mode"
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

# クライアントを接続しない場合は全プレイヤーを記述
# クライアントを接続する場合は最後のプレイヤーを除いて記述
for i in $(seq 1 $n)
do
    if [ "$i" -ne "$n" ] || ! "$client"; then
        # echo "BasketPlayer${i},java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
        # echo "WasabiPlayer${i},java,jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer" >> AutoStarter.ini
        # echo "TomatoPlayer${i},java,com.gmail.toooo1718tyan.Player.RoleAssignPlayer" >> AutoStarter.ini
        # echo "IOHPlayer${i},java,org.aiwolf.IOH.IOHRoleAssignPlayer" >> AutoStarter.ini
        # echo "KarmaPlayer${i},java,aiwolf.org.karma.KarmaRoleAssignPlayer" >> AutoStarter.ini
        # echo "TOKUPlayer${i},java,org.aiwolf.TOKU.TOKURoleAssginPlayer" >> AutoStarter.ini # TOKURoleAssginPlayer (スペルミス)
        # echo "CamelliaPlayer${i},java,camellia.aiwolf.demo.DemoRoleAssignPlayer" >> AutoStarter.ini
        # echo "DaisyoPlayer${i},java,org.aiwolf.daisyo.RoleAssignPlayer" >> AutoStarter.ini
        echo "PythonPlayer${i},python,../start.py" >> AutoStarter.ini
    fi
done

if "$loop"; then
    while true
    do
        java -cp 'lib/aiwolf/*:clients_java/' org.aiwolf.ui.bin.AutoStarter AutoStarter.ini
    done
else
    java -cp 'lib/aiwolf/*:clients_java/' org.aiwolf.ui.bin.AutoStarter AutoStarter.ini
fi