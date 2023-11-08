# mypy で型チェックを行う
# mypy のインストールが必要: python3 -m pip install mypy
# 表示が崩れたらシェルを開き直すと直る
# script は mypy の出力を色付きで grep に渡すために使用
# 使用例1: ライブラリを除いたすべてのファイルを検索
# ./mypygrep.sh -v library
# 使用例2: ddhbVillager.py のエラーのみを検索
# ./mypygrep.sh ddhbVillager.py
script -q /dev/null mypy start.py | grep -v "Skipping analyzing" | grep $@
