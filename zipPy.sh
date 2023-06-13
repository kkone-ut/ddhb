# git log を使って最新のコミットのSHAハッシュを取得
latest_commit=$(git log -1 --pretty=format:"%H")

# 最新のコミットのSHAハッシュの最初の7文字を取得
short_commit=${latest_commit:0:7}

# zip圧縮
zip ddhb_${short_commit}.zip *.py
