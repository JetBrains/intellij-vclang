package org.vclang.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns.and
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.*
import com.intellij.psi.TokenType.BAD_CHARACTER
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import org.vclang.psi.*
import org.vclang.psi.VcElementTypes.*
import org.vclang.psi.ext.impl.DefinitionAdapter
import org.vclang.psi.impl.VcFieldTeleImpl
import java.util.Collections.singletonList

class VclangCompletionContributor: CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PREC_CONTEXT, KeywordCompletionProvider(FIXITY_KWS))
        extend(CompletionType.BASIC, afterLeaf(FAT_ARROW), originalPositionCondition(withParentOrGrandParent(VcClassFieldSyn::class.java), KeywordCompletionProvider(FIXITY_KWS))) // fixity kws for class field synonym (2nd part)
        extend(CompletionType.BASIC, AS_CONTEXT, ProviderWithCondition({parameters, _ -> (parameters.position.parent.parent as VcNsId).asKw == null}, KeywordCompletionProvider(singletonList(AS_KW))))

        extend(CompletionType.BASIC, NS_CMD_CONTEXT, originalPositionCondition(withParent(VcFile::class.java), KeywordCompletionProvider(listOf(HIDING_KW, USING_KW))))
        extend(CompletionType.BASIC, NS_CMD_CONTEXT, ProviderWithCondition({ parameters, _ -> noUsing(parameters.position.parent.parent as VcStatCmd)}, KeywordCompletionProvider(singletonList(USING_KW))))
        extend(CompletionType.BASIC, NS_CMD_CONTEXT, ProviderWithCondition({ parameters, _ -> noUsingAndHiding(parameters.position.parent.parent as VcStatCmd)}, KeywordCompletionProvider(singletonList(HIDING_KW))))
        extend(CompletionType.BASIC, withAncestors(PsiErrorElement::class.java, VcNsUsing::class.java, VcStatCmd::class.java), ProviderWithCondition({parameters, _ -> noHiding(parameters.position.parent.parent.parent as VcStatCmd)}, KeywordCompletionProvider(singletonList(HIDING_KW))))

        extend(CompletionType.BASIC, STATEMENT_END_CONTEXT, onJointOfStatementsCondition(VcStatement::class.java, KeywordCompletionProvider(STATEMENT_KWS)))
        extend(CompletionType.BASIC, STATEMENT_END_CONTEXT, onJointOfStatementsCondition(VcStatement::class.java,
                ProviderWithCondition({ parameters, _ -> parameters.position.ancestors.filter { it is VcWhere }.toList().isEmpty() }, KeywordCompletionProvider(singletonList(IMPORT_KW)))))
        extend(CompletionType.BASIC, DATA_AFTER_TRUNCATED_CONTEXT, genericJointCondition({_, _, jD -> jD.prevElement?.node?.elementType == TRUNCATED_KW}, KeywordCompletionProvider(singletonList(DATA_KW)))) //data after \truncated keyword

        extend(CompletionType.BASIC, STATEMENT_END_CONTEXT, onJointOfStatementsCondition(VcStatement::class.java, KeywordCompletionProvider(singletonList(WHERE_KW)),
                {jD: JointData ->
                    var anc = jD.prevElement
                    while (anc != null && anc !is VcStatement) anc = anc.parent
                    var flag = false
                    if (anc is VcStatement) {
                        val da = anc.definition
                        if (da is DefinitionAdapter<*>) {
                            val where =  da.getWhere()
                            if (where == null || (where.lbrace == null && where.rbrace == null)) flag = true
                        }
                    }
                    flag}))

        extend(CompletionType.BASIC,  withAncestors(PsiErrorElement::class.java, VcDefClass::class.java),
                genericJointCondition({_, _, jD -> jD.prevElement?.node?.elementType == ID}, KeywordCompletionProvider(singletonList(EXTENDS_KW))))
        extend(CompletionType.BASIC, withParentOrGrandParent(VcFieldDefIdentifier::class.java),
                ProviderWithCondition({ parameters, _ ->
                    val fieldDefId = when {
                        parameters.position.parent is VcFieldDefIdentifier -> parameters.position.parent
                        parameters.position.parent!!.parent is VcFieldDefIdentifier -> parameters.position.parent!!.parent
                        else -> null
                    }
                    if (fieldDefId != null && fieldDefId.parent is VcFieldTele)fieldDefId.parent.nextSibling !is VcFieldTele else false
                }, KeywordCompletionProvider(singletonList(EXTENDS_KW))))

        //extend(CompletionType.BASIC, ANY, KeywordCompletionProvider(singletonList(INVALID_KW)))
    }

    companion object {
        val FIXITY_KWS = listOf(INFIX_LEFT_KW, INFIX_RIGHT_KW, INFIX_NON_KW, NON_ASSOC_KW, LEFT_ASSOC_KW, RIGHT_ASSOC_KW)
        val STATEMENT_KWS = listOf(FUNCTION_KW, DATA_KW, CLASS_KW, INSTANCE_KW, TRUNCATED_KW, OPEN_KW)
        val GLOBAL_STATEMENT_KWS = STATEMENT_KWS + singletonList(IMPORT_KW)

        const val KEYWORD_PRIORITY = 10.0

        private fun afterLeaf(et: IElementType) = PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(et))
        private fun <T : PsiElement> withParent(et: Class<T>) = PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(et))
        private fun <T : PsiElement> withGrandParent(et: Class<T>) = PlatformPatterns.psiElement().withSuperParent(2, PlatformPatterns.psiElement(et))
        private fun <T : PsiElement> withParentOrGrandParent(et: Class<T>) = or(withParent(et), withGrandParent(et))
        private fun <T : PsiElement> withGrandParents(vararg et: Class<out T>) = or(*et.map { withGrandParent(it) }.toTypedArray())
        private fun <T : PsiElement> withParents(vararg et: Class<out T>) = or(*et.map { withParent(it) }.toTypedArray())
        private fun <T : PsiElement> withAncestors(vararg et: Class<out T>): ElementPattern<PsiElement> = and(*et.mapIndexed { i, it -> PlatformPatterns.psiElement().withSuperParent(i + 1, PlatformPatterns.psiElement(it)) }.toTypedArray())

        val PREC_CONTEXT = or(afterLeaf(FUNCTION_KW), afterLeaf(DATA_KW), afterLeaf(CLASS_KW), afterLeaf(AS_KW),
                and(afterLeaf(PIPE), withGrandParents(VcConstructor::class.java, VcDataBody::class.java)), //simple data type constructor
                and(afterLeaf(FAT_ARROW), withGrandParents(VcConstructor::class.java, VcConstructorClause::class.java)), //data type constructors with patterns
                and(afterLeaf(PIPE), withGrandParents(VcClassField::class.java, VcClassStat::class.java)), //class field
                and(afterLeaf(FAT_ARROW), withGrandParent(VcClassFieldSyn::class.java))) //class field synonym

        val AS_CONTEXT = and(withGrandParent(VcNsId::class.java), withParents(VcRefIdentifier::class.java, PsiErrorElement::class.java))
        val NS_CMD_CONTEXT = withAncestors(PsiErrorElement::class.java, VcStatCmd::class.java)
        val ANY = PlatformPatterns.psiElement()!!
        val STATEMENT_END_CONTEXT = withParents(PsiErrorElement::class.java, VcRefIdentifier::class.java)
        val DATA_AFTER_TRUNCATED_CONTEXT = withAncestors(PsiErrorElement::class.java, VcDefData::class.java)

        private fun noUsing(cmd: VcStatCmd): Boolean = cmd.nsUsing?.usingKw == null
        private fun noHiding(cmd: VcStatCmd): Boolean = cmd.hidingKw == null
        private fun noUsingAndHiding(cmd: VcStatCmd): Boolean = noUsing(cmd) && noHiding(cmd)


        class ProviderWithCondition(private val condition: (CompletionParameters, ProcessingContext?) -> Boolean, private val completionProvider: CompletionProvider<CompletionParameters>) : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
                if (condition(parameters, context))
                    completionProvider.addCompletionVariants(parameters, context, result)
            }
        }

        private fun <T> originalPositionCondition(opc: ElementPattern<T>, completionProvider: CompletionProvider<CompletionParameters>): CompletionProvider<CompletionParameters> =
                ProviderWithCondition({ parameters, context ->
                    opc.accepts(parameters.originalPosition, context)
                }, completionProvider)

        private class JointData(val prevElement: PsiElement?, val delimeterBeforeCaret: Boolean, val nextElement: PsiElement?)

        private fun textBeforeCaret(whiteSpace: PsiWhiteSpace, caretOffset: Int): String = when {
                whiteSpace.textRange.contains(caretOffset) -> whiteSpace.text.substring(0, caretOffset - whiteSpace.textRange.startOffset)
                caretOffset < whiteSpace.textRange.startOffset -> ""
                else -> whiteSpace.text
            }


        private fun elementsOnJoint(file: PsiFile, caretOffset: Int): JointData {
            var ofs = 0
            var nextElement: PsiElement?
            var prevElement: PsiElement?
            var delimiterBeforeCaret = false
            var skippedFirstErrorExpr: PsiElement? = null
            do {
                val pos = caretOffset + (ofs++)
                nextElement = if (pos > file.textLength) null else file.findElementAt(pos)
            } while (nextElement is PsiWhiteSpace || nextElement is PsiComment)
            ofs = -1
            do {
                val pos = caretOffset + (ofs--)
                prevElement = if (pos < 0) null else file.findElementAt(pos)
                delimiterBeforeCaret = delimiterBeforeCaret || (prevElement is PsiWhiteSpace && textBeforeCaret(prevElement, caretOffset).contains('\n')) || (pos <= 0)
                var skipFirstErrorExpr = (prevElement?.node?.elementType == BAD_CHARACTER || (prevElement?.node?.elementType == INVALID_KW &&
                                prevElement?.parent is PsiErrorElement && prevElement.text.startsWith("\\")))
                if (skipFirstErrorExpr && skippedFirstErrorExpr != null && skippedFirstErrorExpr != prevElement) skipFirstErrorExpr = false else skippedFirstErrorExpr = prevElement
            } while (prevElement is PsiWhiteSpace || prevElement is PsiComment || skipFirstErrorExpr)

            return JointData(prevElement, delimiterBeforeCaret, nextElement)
        }

        private fun <T> ancestorsUntil(c: Class<T>, element: PsiElement?): HashSet<PsiElement> {
            val ancestors = HashSet<PsiElement>()
            var elem: PsiElement? = element
            if (elem != null) ancestors.add(elem)
            while (elem != null && !c.isInstance(elem)) {
                elem = elem.parent
                ancestors.add(elem)
            }
            return ancestors
        }

        private fun genericJointCondition(condition: (CompletionParameters, ProcessingContext?, JointData) -> Boolean, completionProvider: CompletionProvider<CompletionParameters>) =
                ProviderWithCondition({ parameters, context -> condition(parameters, context, elementsOnJoint(parameters.originalFile, parameters.offset)) }, completionProvider)


        private fun <T> onJointOfStatementsCondition(statementClass: Class<T>, completionProvider: CompletionProvider<CompletionParameters>,
                                                     additionalCondition: (JointData) -> Boolean = {_: JointData -> true}): CompletionProvider<CompletionParameters> =
                genericJointCondition({_, _, jointData ->
                    val ancestorsNE = ancestorsUntil(statementClass, jointData.nextElement)
                    val ancestorsPE = ancestorsUntil(statementClass, jointData.prevElement)
                    jointData.delimeterBeforeCaret && additionalCondition(jointData) && ancestorsNE.intersect(ancestorsPE).isEmpty() }, completionProvider)

        // Contribution to LookupElementBuilder
        fun LookupElementBuilder.withPriority(priority: Double): LookupElement = PrioritizedLookupElement.withPriority(this, priority)
    }

    class KeywordCompletionProvider(private val keywords : List<IElementType>) : CompletionProvider<CompletionParameters>() {

        private val insertHandler = InsertHandler<LookupElement> { insertContext, _ ->
            val document = insertContext.document
            document.insertString(insertContext.tailOffset, " ") // add tail whitespace
            insertContext.commitDocument()
            insertContext.editor.caretModel.moveToOffset(insertContext.tailOffset)
        }

        private val truncatedInsertHandler = InsertHandler<LookupElement> {insertContext, _ ->
            val document = insertContext.document
            document.insertString(insertContext.tailOffset, " \\data ")
            insertContext.commitDocument()
            insertContext.editor.caretModel.moveToOffset(insertContext.tailOffset)
        }

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
            var prefix = result.prefixMatcher.prefix
            while (prefix.contains(' '))
                prefix = prefix.substring(prefix.indexOf(' ')+1, prefix.length)
            if (prefix == "\\") prefix = ""

            val nonEmptyPrefix = prefix.isNotEmpty() ||
                                 parameters.offset > 0 && parameters.originalFile.text.substring(parameters.offset - 1, parameters.offset) == "\\" //prefix consists of single slash character

            System.out.println("position.parent: "+parameters.position.parent?.javaClass)
            System.out.println("position.grandparent: "+parameters.position.parent?.parent?.javaClass)
            System.out.println("position.grandgrandparent: "+parameters.position.parent?.parent?.parent?.javaClass)
            System.out.println("originalPosition.parent: "+parameters.originalPosition?.parent?.javaClass)
            System.out.println("originalPosition.grandparent: "+parameters.originalPosition?.parent?.parent?.javaClass)
            val jointData = elementsOnJoint(parameters.originalFile, parameters.offset)
            System.out.println("prevElement: ${jointData.prevElement} text: ${jointData.prevElement?.text}")
            System.out.println("prevElement.parent: ${jointData.prevElement?.parent?.javaClass}")
            System.out.println("prevElement.grandparent: ${jointData.prevElement?.parent?.parent?.javaClass}")
            System.out.println("nextElement: ${jointData.nextElement} text: ${jointData.nextElement?.text}")
            System.out.println("nextElement.parent: ${jointData.nextElement?.parent?.javaClass}")
            if (parameters.position.parent is PsiErrorElement) System.out.println("errorDescription: "+(parameters.position.parent as PsiErrorElement).errorDescription)
            System.out.println("")

            for (keyword in keywords) {
                val handler = when (keyword) {
                    TRUNCATED_KW -> truncatedInsertHandler
                    else -> insertHandler
                }

                val lookupElement = when (keyword) {
                    TRUNCATED_KW -> LookupElementBuilder.create(keyword.toString()).withPresentableText("\\truncated \\data")
                    else -> LookupElementBuilder.create(keyword.toString())
                }

                result.withPrefixMatcher(PlainPrefixMatcher(if (nonEmptyPrefix) "\\"+prefix else "")).addElement(lookupElement.bold().withInsertHandler(handler).withPriority(KEYWORD_PRIORITY))
            }

        }
    }

    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean = typeChar == '\\'
}