# 引数で指定されたのがファイルのリストなら、そのファイルをSJISからUTF-8に変換する
# 引数で指定されたのがディレクトリなら、そのディレクトリ以下のすべてのjavaファイルをSJISからUTF-8に変換する

if [ -d $1 ]; then
    # ディレクトリならそのディレクトリ以下のすべてのjavaファイルをSJISからUTF-8に変換する
    find $1 -name "*.java" | xargs ./sjis2utf.sh
    exit 0
fi

# 指定されたすべてのファイルをSJISからUTF-8に変換する
for file in $*
do
    iconv -f sjis -t utf-8 $file 1<> $file
done