#!/usr/bin/env -S python -B
#
# start.py
#
# Copyright 2022 OTSUKI Takashi
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from argparse import ArgumentParser

from aiwolf import AbstractPlayer, TcpipClient

from ddhbPlayer import ddhbPlayer
from Util import Util

import warnings

if __name__ == "__main__":
    # 警告をエラーとして扱う (デバッグ用)
    warnings.simplefilter('error')

    agent: AbstractPlayer = ddhbPlayer()
    parser: ArgumentParser = ArgumentParser(add_help=False)
    # 引数の仕様を定義する
    parser.add_argument("-p", type=int, action="store", dest="port", required=True)
    parser.add_argument("-h", type=str, action="store", dest="hostname", required=True)
    parser.add_argument("-r", type=str, action="store", dest="role", default="none") # 役指定
    parser.add_argument("-n", type=str, action="store", dest="name") # プレイヤー名
    parser.add_argument("-q", action="store_true", dest="quiet") # デバッグモードオフ
    # 引数を解析する
    input_args = parser.parse_args()
    Util.debug_print("name: ", input_args.name)
    if input_args.quiet:
        Util.debug_mode = False
    TcpipClient(agent, input_args.name, input_args.hostname, input_args.port, input_args.role).connect()
