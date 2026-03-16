package com.github.zero9178.mlirods.settings

import com.intellij.openapi.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
@State(
    name = "tableGen.tools.project",
    storages = [Storage("tableGen.tools.xml", roamingType = RoamingType.DISABLED)],
    category = SettingsCategory.TOOLS
)
class TableGenToolsProjectSettings(private val cs: CoroutineScope) :
    SerializablePersistentStateComponent<TableGenToolsProjectSettings.State>(State()) {
    private val myLspEnabledFlow = MutableSharedFlow<Boolean>(replay = 1)
    private val myLspServerPathFlow = MutableSharedFlow<String>(replay = 1)

    /**
     * Controls whether 'tblgen-lsp-server' should be used or not.
     */
    var lspEnabled: Boolean
        get() = state.lspEnabled
        set(value) {
            updateState {
                it.copy(lspEnabled = value)
            }
            cs.launch { myLspEnabledFlow.emit(value) }
        }

    var lspServerPath: String
        get() = state.lspServerPath
        set(value) {
            updateState {
                it.copy(lspServerPath = value)
            }
            cs.launch { myLspServerPathFlow.emit(value) }
        }

    val lspEnabledFlow: Flow<Boolean>
        get() = myLspEnabledFlow.distinctUntilChanged()

    val lspServerPathFlow: Flow<String>
        get() = myLspServerPathFlow.distinctUntilChanged()

    override fun loadState(state: State) {
        super.loadState(state)
        // Update the flow anytime state is reloaded from disk.
        cs.launch {
            myLspEnabledFlow.emit(state.lspEnabled)
            myLspServerPathFlow.emit(state.lspServerPath)
        }
    }

    data class State(
        @JvmField val lspEnabled: Boolean = true,
        @JvmField val lspServerPath: String = ""
    )
}