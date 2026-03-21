package org.inklang.bukkit

import org.bukkit.command.CommandSender
import org.inklang.InkContext
import org.inklang.InkIo
import org.inklang.InkJson
import org.inklang.InkDb
import org.inklang.InkScript
import org.inklang.ContextVM
import org.inklang.lang.Value
import java.io.File
import java.util.ArrayDeque

/**
 * Extended context for plugin scripts with lifecycle support.
 */
class PluginContext(
    private val sender: CommandSender,
    private val plugin: InkBukkit,
    private val io: InkIo,
    private val json: InkJson,
    private val db: InkDb,
    private val pluginName: String,
    private val pluginFolder: File
) : InkContext {

    private var vm: ContextVM? = null

    override fun log(message: String) {
        plugin.logger.info("[Ink/$pluginName] $message")
    }

    override fun print(message: String) {
        sender.sendMessage("§f[Ink/$pluginName] $message")
    }

    override fun io(): InkIo = io
    override fun json(): InkJson = json
    override fun db(): InkDb = db

    override fun registerEventHandler(
        eventName: String,
        handlerFunc: Value.Function,
        eventParamName: String,
        dataParamNames: List<String>
    ) {
        // Event registration handled at compile time via VM's event registry
    }

    override fun fireEvent(eventName: String, event: Value.EventObject, data: List<Value?>): Boolean {
        val loaded = vm ?: return false
        val registry = loaded.globals["__eventRegistry"] as? Value.Instance ?: return false
        val handlers = registry.fields["__handlers"] as? Value.InternalList ?: return false

        var cancelled = false
        for (handler in handlers.items) {
            if (handler is Value.EventHandler && handler.eventName.value == eventName) {
                val argBuffer = kotlin.collections.ArrayDeque<Value>()
                argBuffer.addLast(event)
                data.forEach { argBuffer.addLast(it ?: Value.Null) }
                loaded.executeHandler(handler.handlerFunc, argBuffer)
            }
        }
        return cancelled
    }

    override fun onEnable(script: InkScript) {
        // No-op: VM executes enable directly via PluginRuntime
    }

    override fun onDisable(script: InkScript) {
        // No-op: VM executes disable directly via PluginRuntime
    }

    override fun supportsLifecycle(): Boolean = true

    override fun setVM(vm: ContextVM) {
        this.vm = vm
    }

    fun getVM(): ContextVM? = vm

    fun getPluginFolder(): File = pluginFolder
    fun getPluginName(): String = pluginName
}
