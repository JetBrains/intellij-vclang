package org.arend.psi.ext

import com.intellij.lang.ASTNode
import org.arend.naming.reference.NamedUnresolvedReference
import org.arend.naming.reference.Referable
import org.arend.psi.ArendNsId
import org.arend.psi.ArendStatCmd
import org.arend.psi.ancestors
import org.arend.term.ChildNamespaceCommand
import org.arend.term.NamespaceCommand
import org.arend.term.abs.Abstract
import org.arend.term.group.ChildGroup

abstract class ArendStatCmdImplMixin(node: ASTNode) : ArendSourceNodeImpl(node), ArendStatCmd, ChildNamespaceCommand {
    override fun getKind(): NamespaceCommand.Kind {
        if (importKw != null) return NamespaceCommand.Kind.IMPORT
        if (openKw != null) return NamespaceCommand.Kind.OPEN
        error("Incorrect expression: namespace command")
    }

    override fun getPath(): List<String> = longName?.let { it.refIdentifierList.map { it.referenceName } }
            ?: emptyList()

    override fun isUsing(): Boolean {
        val using = nsUsing
        return using == null || using.usingKw != null
    }

    override fun getOpenedReferences(): List<ArendNsId> = nsUsing?.nsIdList ?: emptyList()

    override fun getHiddenReferences(): List<Referable> = refIdentifierList.map { NamedUnresolvedReference(it, it.text) }

    override fun getParentGroup(): ChildGroup? = parent.ancestors.filterIsInstance<ChildGroup>().firstOrNull()

    override fun getOpenedReference(): Abstract.LongReference? = longName
}
