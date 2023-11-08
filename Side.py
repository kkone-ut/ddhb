from enum import Enum
from typing import List

from Util import Util

from aiwolf import Role


class Side(Enum):
    """Enumeration type for side."""

    UNC = "UNC"
    """Uncertain."""
    VILLAGERS = "VILLAGERS"
    """Villager."""
    WEREWOLVES = "WEREWOLVES"
    """Werewolf."""
    ANY = "ANY"
    """Wildcard."""

    def get_role_list(self, N: int) -> List[Role]:
        """Get role list of the side.
        Args:
            N (int): The number of players.
        Returns:
            List[Role]: The role list of the side.
        """
        if self == Side.VILLAGERS:
            if N == 5:
                return [Role.VILLAGER, Role.SEER]
            elif N == 15:
                return [Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD]
            else:
                Util.error("Invalid N: " + str(N))
        elif self == Side.WEREWOLVES:
            return [Role.WEREWOLF, Role.POSSESSED]
        else:
            Util.error("Invalid side: " + str(self))
