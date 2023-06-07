port=10000
n=15
view=false
game=1
lib=./
log=./log/
setting=./SampleSetting.cfg
client=false
loop=false
otherAgents="basket"

help=false

while getopts ":p:n:vg:a:clh" opt; do
  case $opt in
    p) port="$OPTARG"
    ;;
    n) n="$OPTARG"
    ;;
    v) view="true"
    ;;
    g) game="$OPTARG"
    ;;
    a) otherAgents="$OPTARG"
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

# otherAgents が "all" でないなら
if [ "$otherAgents" = "all" ]; then
    if [ "$n" = 5 ]; then
        echo "BasketPlayer,java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
        echo "WasabiPlayer,java,jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer" >> AutoStarter.ini
        echo "TomatoPlayer,java,com.gmail.toooo1718tyan.Player.RoleAssignPlayer" >> AutoStarter.ini
        echo "IOHPlayer,java,org.aiwolf.IOH.IOHRoleAssignPlayer" >> AutoStarter.ini
        echo "KarmaPlayer,java,aiwolf.org.karma.KarmaRoleAssignPlayer" >> AutoStarter.ini
    elif [ "$n" = 15 ]; then
        for i in $(seq 1 2)
        do
            echo "BasketPlayer${i},java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
            echo "WasabiPlayer${i},java,jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer" >> AutoStarter.ini
            echo "TomatoPlayer${i},java,com.gmail.toooo1718tyan.Player.RoleAssignPlayer" >> AutoStarter.ini
            echo "IOHPlayer${i},java,org.aiwolf.IOH.IOHRoleAssignPlayer" >> AutoStarter.ini
            echo "KarmaPlayer${i},java,aiwolf.org.karma.KarmaRoleAssignPlayer" >> AutoStarter.ini
            echo "TOKUPlayer${i},java,org.aiwolf.TOKU.TOKURoleAssginPlayer" >> AutoStarter.ini # TOKURoleAssginPlayer (スペルミス)
            echo "CamelliaPlayer${i},java,camellia.aiwolf.demo.DemoRoleAssignPlayer" >> AutoStarter.ini
        done
        if [ "$client" = "false" ]; then
            echo "DaisyoPlayer${i},java,org.aiwolf.daisyo.RoleAssignPlayer" >> AutoStarter.ini
        fi
    else
        echo "Error: number of players is not 5 or 15"
        exit 1
    fi
else
    for i in $(seq 1 $n)
    do
        if [ "$i" -ne "$n" ] || ! "$client"; then
            if [ "$otherAgents" = "basket" ]; then
                echo "BasketPlayer${i},java,org.aiwolf.Basket.BasketRoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "wasabi" ]; then
                echo "WasabiPlayer${i},java,jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "tomato" ]; then
                echo "TomatoPlayer${i},java,com.gmail.toooo1718tyan.Player.RoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "ioh" ]; then
                echo "IOHPlayer${i},java,org.aiwolf.IOH.IOHRoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "karma" ]; then
                echo "KarmaPlayer${i},java,aiwolf.org.karma.KarmaRoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "toku" ]; then
                echo "TOKUPlayer${i},java,org.aiwolf.TOKU.TOKURoleAssginPlayer" >> AutoStarter.ini # TOKURoleAssginPlayer (スペルミス)
            elif [ "$otherAgents" = "camellia" ]; then
                echo "CamelliaPlayer${i},java,camellia.aiwolf.demo.DemoRoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "daisyo" ]; then
                echo "DaisyoPlayer${i},java,org.aiwolf.daisyo.RoleAssignPlayer" >> AutoStarter.ini
            elif [ "$otherAgents" = "ddhb" ]; then
                echo "PythonPlayer${i},python,../start.py" >> AutoStarter.ini
            else
                echo Agent \"$otherAgents\" is not supported.
                exit 1
            fi
        fi
    done
fi

if "$loop"; then
    while true
    do
        java -cp 'lib/aiwolf/*:clients_java/' org.aiwolf.ui.bin.AutoStarter AutoStarter.ini
    done
else
    java -cp 'lib/aiwolf/*:clients_java/' org.aiwolf.ui.bin.AutoStarter AutoStarter.ini
fi