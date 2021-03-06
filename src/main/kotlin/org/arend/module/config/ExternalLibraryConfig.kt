package org.arend.module.config

import org.arend.util.Range
import org.arend.util.Version
import org.arend.yaml.*
import org.jetbrains.yaml.psi.YAMLFile


class ExternalLibraryConfig(override val name: String, yaml: YAMLFile) : LibraryConfig(yaml.project) {
    override val sourcesDir = yaml.sourcesDir ?: ""
    override val binariesDir = yaml.binariesDir
    override val testsDir = yaml.testsDir
    override val extensionsDir = yaml.extensionsDir
    override val extensionMainClass = yaml.extensionMainClass
    override val modules = yaml.modules
    override val dependencies = yaml.dependencies
    override val langVersion: Range<Version> = yaml.langVersion.let {
        if (it.isEmpty()) Range.unbound() else Range.parseVersionRange(it)
    }

    override val isExternal: Boolean
        get() = true

    override var root = yaml.virtualFile?.parent
        get() =
            if (field?.isValid == false) {
                field = null
                null
            } else field
        private set
}