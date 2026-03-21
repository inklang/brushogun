package org.inklang.bukkit

import org.bukkit.command.CommandSender
import org.bukkit.Server
import org.inklang.lang.Value
import org.inklang.lang.ClassDescriptor

/**
 * Provides Paper/Bukkit-specific globals for Ink scripts.
 * These are injected into the VM's global scope.
 */
object PaperGlobals {

    /**
     * Get a map of Paper/Bukkit globals to inject into a VM.
     * @param console The server console sender
     * @param server The Bukkit server instance
     * @return Map of global name to Value
     */
    fun getGlobals(console: CommandSender, server: Server): Map<String, Value> {
        val serverDescriptor = ClassDescriptor(
            name = "BukkitServer",
            superClass = null,
            methods = mapOf(
                "name" to Value.NativeFunction { Value.String(server.name) },
                "version" to Value.NativeFunction { Value.String(server.version) },
                "bukkitVersion" to Value.NativeFunction { Value.String(server.bukkitVersion) }
            ),
            readOnly = true
        )

        return mapOf(
            "console" to Value.Instance(
                ClassDescriptor(
                    name = "ConsoleSender",
                    superClass = null,
                    methods = emptyMap(),
                    readOnly = true
                ),
                mutableMapOf(
                    "name" to Value.String(console.name)
                )
            ),
            "server" to Value.Instance(serverDescriptor, mutableMapOf())
        )
    }
}
