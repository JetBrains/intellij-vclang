package org.arend.psi.ext.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IStubElementType
import org.arend.ArendIcons
import org.arend.naming.reference.*
import org.arend.psi.*
import org.arend.psi.ext.PsiLocatedReferable
import org.arend.psi.stubs.ArendClassFieldParamStub
import org.arend.resolving.ArendDefReferenceImpl
import org.arend.resolving.ArendReference
import org.arend.resolving.FieldDataLocatedReferable
import org.arend.term.ClassFieldKind
import org.arend.term.abs.Abstract
import org.arend.resolving.util.ReferableExtractVisitor

abstract class FieldDefIdentifierAdapter : ReferableAdapter<ArendClassFieldParamStub>, ArendFieldDefIdentifier {
    constructor(node: ASTNode) : super(node)

    constructor(stub: ArendClassFieldParamStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getKind() = GlobalReferable.Kind.FIELD

    override val referenceNameElement
        get() = this

    override val referenceName: String
        get() = textRepresentation()

    override val longName: List<String>
        get() = listOf(referenceName)

    override val resolve: PsiElement?
        get() = this

    override val unresolvedReference: UnresolvedReference?
        get() = null

    override fun getClassFieldKind() = ClassFieldKind.ANY

    override fun getReference(): ArendReference = ArendDefReferenceImpl<ArendFieldDefIdentifier>(this)

    override fun getPrec(): ArendPrec? = null

    override fun getAlias(): ArendAlias? = null

    override fun getReferable() = this

    override fun isVisible() = false

    override fun isExplicitField() = (parent as? ArendFieldTele)?.isExplicit ?: true

    override fun isParameterField() = true

    override fun isClassifying() = (parent as? ArendFieldTele)?.classifyingKw != null

    override fun isCoerce() = (parent as? ArendFieldTele)?.coerceKw != null

    override fun getTypeClassReference(): ClassReferable? =
        resultType?.let { ReferableExtractVisitor().findClassReferable(it) }

    override val typeOf: ArendExpr?
        get() = resultType

    override fun getParameters(): List<Abstract.Parameter> = emptyList()

    override fun getResultType(): ArendExpr? = (parent as? ArendFieldTele)?.expr

    override fun getResultTypeLevel(): ArendExpr? = null

    override fun getIcon(flags: Int) = ArendIcons.CLASS_FIELD

    override fun getUseScope() = GlobalSearchScope.projectScope(project)

    override val psiElementType: PsiElement?
        get() = resultType

    override val rangeInElement: TextRange
        get() = TextRange(0, text.length)

    override fun makeTCReferable(data: SmartPsiElementPointer<PsiLocatedReferable>, parent: LocatedReferable?) =
        FieldDataLocatedReferable(data, this, parent)
}