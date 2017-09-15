package org.vclang.psi

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.vclang.VcLanguage
import org.vclang.psi.VcElementTypes.*

class VcTokenType(debugName: String) : IElementType(debugName, VcLanguage)

val VC_KEYWORDS: TokenSet = TokenSet.create(
        OPEN_KW, EXPORT_KW, HIDING_KW, FUNCTION_KW, NON_ASSOC_KW,
        LEFT_ASSOC_KW, RIGHT_ASSOC_KW, PROP_KW, WHERE_KW, WITH_KW,
        ELIM_KW, NEW_KW, PI_KW, SIGMA_KW, LAM_KW, LET_KW,
        IN_KW, CASE_KW, WITH_KW, DATA_KW, CLASS_KW, EXTENDS_KW,
        VIEW_KW, ON_KW, BY_KW, INSTANCE_KW, TRUNCATED_KW,
        DEFAULT_KW, LP_KW, LH_KW, SUC_KW, MAX_KW,
        SET, UNIVERSE, TRUNCATED_UNIVERSE
)

val VC_COMMENTS: TokenSet = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)

val VC_WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)