package plugin

import kotlinx.coroutines.launch
import net.trueog.diamondbankog.DiamondBankException
import net.trueog.diamondbankog.PostgreSQL.ShardType
import net.trueog.utilitiesog.UtilitiesOG
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class Listeners : Listener {
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        // Make sure to never run any other Bukkit functions in launch {} (for example accessing players' inventories)
        // launch {} is needed in this case since getPlayerShards().await() calls a database which can be slow to run on
        // the main thread
        // and .await() is a function for in coroutines in the first place
        KotlinTemplateOG.scope.launch {
            val playerShardsResult =
                KotlinTemplateOG.diamondBankAPI.getPlayerShards(event.player.uniqueId, ShardType.ALL)
            val playerShards =
                playerShardsResult.getOrElse { e ->
                    when (e) {
                        DiamondBankException.EconomyDisabledException -> {
                            UtilitiesOG.trueogMessage(event.player, "<red>The economy is disabled.")
                            return@launch
                        }

                        DiamondBankException.TransactionsLockedException -> {
                            UtilitiesOG.trueogMessage(event.player, "<red>Your transactions are locked.")
                            return@launch
                        }

                        else -> {
                            UtilitiesOG.trueogMessage(event.player, "<red>Something went wrong.")
                            return@launch
                        }
                    }
                }

            val shardsInBank = playerShards.shardsInBank
            val shardsInInventory = playerShards.shardsInInventory
            val shardsInEnderChest = playerShards.shardsInEnderChest
            if (shardsInBank == null || shardsInInventory == null || shardsInEnderChest == null) {
                UtilitiesOG.trueogMessage(event.player, "<red>An error has occurred.")
                return@launch
            }
            val totalBalance = shardsInBank + shardsInInventory + shardsInEnderChest

            // Send a message to the player with their balance.
            UtilitiesOG.trueogMessage(event.player, "&BYour balance is: &e$totalBalance&B Diamonds.")
            UtilitiesOG.logToConsole(
                "[Template-OG]",
                "The player: " + event.player + "'s <aqua>balance</aqua> is: " + totalBalance + "&B Diamonds",
            )
        }
    }
}
