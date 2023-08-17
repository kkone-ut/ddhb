from argparse import ArgumentParser

from aiwolf import AbstractPlayer, TcpipClient

from ddhbPlayer import ddhbPlayer
from Util import Util
from ActionLogger import ActionLogger

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
    # 引数を解析する
    input_args = parser.parse_args()

    Util.init()
    Util.debug_print("name: ", input_args.name)
    if input_args.name == None:
        Util.local = True

    ActionLogger.init()

    TcpipClient(agent, input_args.name, input_args.hostname, input_args.port, input_args.role).connect()
