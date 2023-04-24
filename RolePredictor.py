from aiwolf import AbstractPlayer, Agent, Content, GameInfo, GameSetting, Role

class RolePredictor:

    def __init__(self, game_info: GameInfo, game_setting: GameSetting, _player) -> None:
        self.N = game_setting.player_num
        self.M = len(game_setting.role_num_map)
        self.player = _player
        self.me = _player.me
        pass

    def update(self, game_info: GameInfo, game_setting: GameSetting) -> None:

        N = self.N
        M = self.M

        pass