package org.arend.quickfix

import org.intellij.lang.annotations.Language
import org.arend.ArendTestBase
import org.arend.FileTree
import org.arend.fileTreeFromText
import org.arend.psi.ArendFile

abstract class QuickFixTestBase : ArendTestBase() {
    protected fun configure(@Language("Arend") contents: String, annotate: Boolean = true): FileTree {
        val result = fileTreeFromText(contents)
        result.createAndOpenFileWithCaretMarker()
        if (annotate) {
            myFixture.doHighlighting()
        }
        return result
    }

    protected fun checkNoQuickFixes(fixName: String, @Language("Arend") contents: String? = null) {
        if (contents != null) {
            InlineFile(contents).withCaret()
        }
        assert(myFixture.filterAvailableIntentions(fixName).isEmpty())
    }

    protected fun checkQuickFix(fixName: String, @Language("Arend") resultingContent: String) {
        myFixture.launchAction(myFixture.findSingleIntention(fixName))
        testCaret(resultingContent)
    }

    protected fun simpleQuickFixTest(fixName: String, @Language("Arend") contents: String, @Language("Arend") resultingContent: String) {
        configure(contents)
        checkQuickFix(fixName, resultingContent)
    }

    protected fun simpleActionTest (@Language("Arend") contents: String, @Language("Arend") resultingContent: String, f: (ArendFile) -> Unit) {
        InlineFile(contents).withCaret()

        val file = myFixture.configureByFile("Main.ard")
        if (file is ArendFile)
            f.invoke(file)

        myFixture.checkResult(resultingContent.trimIndent(), true)
    }

    protected fun typedQuickFixTest(fixName: String, @Language("Arend") contents: String, @Language("Arend") resultingContent: String) {
        val fileTree = configure(contents, false)
        typecheck(fileTree.fileNames)
        myFixture.doHighlighting()
        checkQuickFix(fixName, resultingContent)
    }

    protected fun typedCheckNoQuickFixes(fixName: String, @Language("Arend") contents: String) {
        val fileTree = configure(contents, false)
        typecheck(fileTree.fileNames)
        myFixture.doHighlighting()
        assert(myFixture.filterAvailableIntentions(fixName).isEmpty())
    }

}
