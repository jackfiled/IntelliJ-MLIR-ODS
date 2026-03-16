package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.LspLifetimeListener
import com.github.zero9178.mlirods.lsp.TableGenLspServerDescriptor
import com.github.zero9178.mlirods.lsp.TableGenLspServerSupportProviderInterface
import com.github.zero9178.mlirods.settings.TableGenToolsProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.jetbrains.cidr.cpp.cmake.model.CMakeConfiguration
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import org.eclipse.lsp4j.InitializeResult
import java.io.File

internal class CMakeTableGenLspServerSupportProvider : TableGenLspServerSupportProviderInterface {
    private val tableGenCompileCommandFilename = "tablegen_compile_commands.yml"

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ): Boolean {
        val settings = project.service<TableGenToolsProjectSettings>()
        val compileCommandFile =  resolveTableGenCompileCommandFile(project)
        if (compileCommandFile == null) {
            thisLogger().warn("Failed to resolve '$tableGenCompileCommandFilename'.")
            return false
        }


        if (settings.lspServerPath.isNotBlank()) {
            thisLogger().info("Use tblgen-lsp-server executable from setting: '${settings.lspServerPath}'")
            val executable = File(settings.lspServerPath)

            serverStarter.ensureServerStarted(
                TableGenLspServerDescriptor(executable, compileCommandFile, project)
            )
            return true
        }

        val buildConfig = findLspExecutableByCMake(project) ?: return false
        val executable = buildConfig.productFile ?: return false

        serverStarter.ensureServerStarted(
            TableGenLspServerDescriptor(
                executable,
                compileCommandFile,
                project,
                object: LspLifetimeListener {
                    override fun serverInitialized(params: InitializeResult) {
                        project.service<CMakeTableGenBuildNotificationProviderService>()
                            .clearNotifications()
                    }

                    override fun serverFailedToStart() {
                        project.service<CMakeTableGenBuildNotificationProviderService>()
                            .showBuildNotification(buildConfig)
                    }
                }
            )
        )
        return true
    }

    fun findLspExecutableByCMake(project: Project): CMakeConfiguration? {
        val target =
            project.service<CMakeWorkspace>().modelTargets.firstOrNull(CMakeTarget::isTableGenLspServer)
        if (target == null) {
            thisLogger().info("Project has no 'tblgen-lsp-server' cmake target")
            return null
        }

        val activeConfig = project.service<CMakeActiveProfileService>().profileName
        val buildConfig = target.buildConfigurations.find {
            it.name == activeConfig
        }
        if (buildConfig == null) {
            thisLogger().info("'tblgen-lsp-server' has no build configuration called '$activeConfig'")
            return null
        }

        return buildConfig
    }

    fun resolveTableGenCompileCommandFile(project: Project): File? {
        val cmakeWorkspace = project.service<CMakeWorkspace>()
        val activeProfile = project.service<CMakeActiveProfileService>()

        // Finding any target to get build directory.
        val anyTarget = cmakeWorkspace.modelTargets.firstOrNull { target ->
            target.buildConfigurations.any {
                it.name == activeProfile.profileName
            }
        } ?: return null

        val buildConfig = anyTarget.buildConfigurations.firstOrNull {
            it.name == activeProfile.profileName
        } ?: return null

        return buildConfig.configurationGenerationDir.resolve(tableGenCompileCommandFilename)
    }
}
