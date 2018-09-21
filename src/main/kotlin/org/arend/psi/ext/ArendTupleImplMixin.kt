package org.arend.psi.ext

import com.intellij.lang.ASTNode
import org.arend.psi.ArendTuple
import org.arend.term.abs.AbstractExpressionVisitor

abstract class ArendTupleImplMixin(node: ASTNode) : ArendExprImplMixin(node), ArendTuple {
    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R {
        val exprList = tupleExprList
        return if (exprList.size == 1) {
            exprList[0].accept(visitor, params)
        } else {
            visitor.visitTuple(this, tupleExprList, if (visitor.visitErrors()) org.arend.psi.ext.getErrorData(this) else null, params)
        }
    }
}