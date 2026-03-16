package com.github.zero9178.mlirods.settings

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.annotations.NonNls

class TableGenToolsProjectConfigurable(private val project: Project) : SearchableConfigurable {
    private val settings = project.service<TableGenToolsProjectSettings>()
    private val lspEnabledValue = AtomicBooleanProperty(settings.lspEnabled)
    private val lspServerPathValue = AtomicProperty(settings.lspServerPath)

    override fun getId(): @NonNls String = "tools.tableGen"

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("tableGen.tools.displayName")

    override fun isModified(): Boolean  {
        return lspEnabledValue.get() != settings.lspEnabled || lspServerPathValue.get() != settings.lspServerPath
    }

    override fun createComponent() = panel {
        group("Language Server") {
            row {
                checkBox(MyBundle.message("tableGen.lsp.enable"))
                    .bindSelected(lspEnabledValue)
            }
            row("LSP Server Path:") {
                textFieldWithBrowseButton(
                    MyBundle.message("tableGen.lsp.serverPath"),
                    project = project,
                ).bindText(lspServerPathValue)
                    .resizableColumn()
                    .component
            }
        }
    }

    override fun apply() {
        settings.lspEnabled = lspEnabledValue.get()
        settings.lspServerPath = lspServerPathValue.get()

        serviceIfCreated<ProjectManager>()?.openProjects?.forEach {
            restartTableGenLSPAsync(it)
        }
    }

    override fun reset() {
        lspEnabledValue.set(settings.lspEnabled)
        lspServerPathValue.set(settings.lspServerPath)
    }}