# 実行方法
ターミナルを2つ使用する。

1つ目のターミナル
```
./server.sh -c -l -g 100 -a all -n 15
```
- -c：python クライアントを接続する場合
- -l：ループする場合
- -g \<num>：ゲーム数
- -a \<agent>：エージェントを指定する場合。allの場合、各2エージェントずつ。
- -n \<num>：5 or 15 (デフォルト 15)
- -v：ビューモード
- -p \<port>：ポート番号
- -h \<host>：ホスト (デフォルト 127.0.0.1)

2つ目のターミナル
```
./client.sh -r SEER
```
- -r \<role>：役職指定 (VILLAGER, WEREWOLF, …)
- -t \<times>：回数指定 (./server.sh -c -g 100 ./client.sh 10 なら 100 ゲーム 10 回)
