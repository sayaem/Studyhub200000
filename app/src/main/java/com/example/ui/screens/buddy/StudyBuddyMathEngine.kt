package com.example.ui.screens.buddy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-performance, bulletproof mathematical and LaTeX formatting engine for StudyHub Study Buddy.
 * Seamlessly converts LaTeX mathematical source (e.g. `\(\vec{A} \cdot \vec{B} = |A||B| \cos\theta\)`)
 * into publication-quality typography with Unicode math glyphs, vectors, fractions, roots, and Greek symbols.
 */
object StudyBuddyMathEngine {

    private val GREEK_MAP = mapOf(
        "alpha" to "α",
        "beta" to "β",
        "gamma" to "γ",
        "Gamma" to "Γ",
        "delta" to "δ",
        "Delta" to "Δ",
        "epsilon" to "ε",
        "zeta" to "ζ",
        "eta" to "η",
        "theta" to "θ",
        "Theta" to "Θ",
        "iota" to "ι",
        "kappa" to "κ",
        "lambda" to "λ",
        "Lambda" to "Λ",
        "mu" to "μ",
        "nu" to "ν",
        "xi" to "ξ",
        "pi" to "π",
        "Pi" to "Π",
        "rho" to "ρ",
        "sigma" to "σ",
        "Sigma" to "Σ",
        "tau" to "τ",
        "upsilon" to "υ",
        "phi" to "φ",
        "Phi" to "Φ",
        "chi" to "χ",
        "psi" to "ψ",
        "Psi" to "Ψ",
        "omega" to "ω",
        "Omega" to "Ω"
    )

    private val OPERATORS_MAP = mapOf(
        "cdot" to " · ",
        "times" to " × ",
        "div" to " ÷ ",
        "pm" to " ± ",
        "mp" to " ∓ ",
        "leq" to " ≤ ",
        "le" to " ≤ ",
        "geq" to " ≥ ",
        "ge" to " ≥ ",
        "neq" to " ≠ ",
        "ne" to " ≠ ",
        "approx" to " ≈ ",
        "equiv" to " ≡ ",
        "propto" to " ∝ ",
        "infty" to "∞",
        "hbar" to "ℏ",
        "partial" to "∂",
        "nabla" to "∇",
        "int" to "∫",
        "iint" to "∬",
        "iiint" to "∭",
        "oint" to "∮",
        "sum" to "∑",
        "prod" to "∏",
        "forall" to "∀",
        "exists" to "∃",
        "in" to " ∈ ",
        "notin" to " ∉ ",
        "subset" to " ⊂ ",
        "subseteq" to " ⊆ ",
        "cup" to " ∪ ",
        "cap" to " ∩ ",
        "to" to " → ",
        "rightarrow" to " → ",
        "Rightarrow" to " ⇒ ",
        "implies" to " ⇒ ",
        "leftrightarrow" to " ↔ ",
        "Leftrightarrow" to " ⇔ ",
        "iff" to " ⇔ "
    )

    private val SUBSCRIPT_DIGITS = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val SUPERSCRIPT_DIGITS = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ'
    )

    /**
     * Converts a raw LaTeX formula string into clean, readable mathematical Unicode.
     */
    fun formatLatexToUnicode(latex: String): String {
        var s = latex.trim()

        // Strip surrounding delimiters if passed with them
        if (s.startsWith("\\(") && s.endsWith("\\)")) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("\\[") && s.endsWith("\\]")) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("$$") && s.endsWith("$$")) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("$") && s.endsWith("$") && s.length > 2) {
            s = s.substring(1, s.length - 1).trim()
        }

        // Clean out sizing commands like \left, \right
        s = s.replace("\\left(", "(")
            .replace("\\right)", ")")
            .replace("\\left[", "[")
            .replace("\\right]", "]")
            .replace("\\left|", "|")
            .replace("\\right|", "|")
            .replace("\\left\\{", "{")
            .replace("\\right\\}", "}")
            .replace("\\left.", "")
            .replace("\\right.", "")

        // Clean text commands
        s = Regex("""\\text\{([^}]+)\}""").replace(s) { it.groupValues[1] }
        s = Regex("""\\mathrm\{([^}]+)\}""").replace(s) { it.groupValues[1] }
        s = Regex("""\\mathbf\{([^}]+)\}""").replace(s) { it.groupValues[1] }
        s = Regex("""\\textbf\{([^}]+)\}""").replace(s) { it.groupValues[1] }

        // Unit vector hats: \hat{i} -> î, \hat{j} -> ĵ, \hat{k} -> k̂
        s = s.replace("\\hat{i}", "î")
            .replace("\\hat{j}", "ĵ")
            .replace("\\hat{k}", "k̂")
            .replace("\\hat i", "î")
            .replace("\\hat j", "ĵ")
            .replace("\\hat k", "k̂")

        // Vectors: \vec{A} -> A⃗, \vec A -> A⃗
        s = Regex("""\\vec\{([A-Za-z0-9])\}""").replace(s) { "${it.groupValues[1]}⃗" }
        s = Regex("""\\vec\s+([A-Za-z])""").replace(s) { "${it.groupValues[1]}⃗" }
        s = Regex("""\\overrightarrow\{([^}]+)\}""").replace(s) { "${it.groupValues[1]}⃗" }

        // Fractions: \frac{a}{b} -> (a) / (b) or a/b
        s = resolveFractions(s)

        // Roots: \sqrt[n]{x} -> ⁿ√(x), \sqrt{x} -> √(x)
        s = Regex("""\\sqrt\[([^\]]+)\]\{([^}]+)\}""").replace(s) {
            val rootDegree = it.groupValues[1].map { ch -> SUPERSCRIPT_DIGITS[ch] ?: ch }.joinToString("")
            "$rootDegree√(${formatLatexToUnicode(it.groupValues[2])})"
        }
        s = Regex("""\\sqrt\{([^}]+)\}""").replace(s) {
            val inner = formatLatexToUnicode(it.groupValues[1])
            if (inner.length <= 3 && !inner.contains(" ") && !inner.contains("+") && !inner.contains("-")) {
                "√$inner"
            } else {
                "√($inner)"
            }
        }

        // Standard Math Functions: \sin, \cos, \tan, etc.
        val trigFunctions = listOf("sin", "cos", "tan", "sec", "csc", "cot", "arcsin", "arccos", "arctan", "ln", "log", "exp", "lim", "max", "min")
        for (fn in trigFunctions) {
            s = s.replace("\\$fn", fn)
        }

        // Greek Letters
        for ((name, symbol) in GREEK_MAP) {
            s = s.replace("\\$name", symbol)
        }

        // Operators & Symbols
        for ((name, symbol) in OPERATORS_MAP) {
            s = s.replace("\\$name", symbol)
        }

        // Multiline alignment breaks
        s = s.replace("\\\\", "\n")
        s = s.replace("&=", " = ")
        s = s.replace("&", " ")

        // Superscripts: ^{x} or ^2
        s = Regex("""\^\{([^}]+)\}""").replace(s) { match ->
            val content = match.groupValues[1]
            content.map { ch -> SUPERSCRIPT_DIGITS[ch] ?: ch }.joinToString("")
        }
        s = Regex("""\^([0-9a-zA-Z+-])""").replace(s) { match ->
            val ch = match.groupValues[1][0]
            (SUPERSCRIPT_DIGITS[ch] ?: "^$ch").toString()
        }

        // Subscripts: _{x} or _1
        s = Regex("""_\{([^}]+)\}""").replace(s) { match ->
            val content = match.groupValues[1]
            content.map { ch -> SUBSCRIPT_DIGITS[ch] ?: ch }.joinToString("")
        }
        s = Regex("""_([0-9a-zA-Z+-])""").replace(s) { match ->
            val ch = match.groupValues[1][0]
            (SUBSCRIPT_DIGITS[ch] ?: "_$ch").toString()
        }

        // Cleanup remaining unneeded escapes or double spaces
        s = s.replace("\\,", " ")
            .replace("\\;", " ")
            .replace("\\:", " ")
            .replace("\\quad", "   ")
            .replace("\\qquad", "     ")
            .replace("\\%", "%")
            .replace("\\$", "$")
            .replace("\\{", "{")
            .replace("\\}", "}")

        // Normalize multiple spaces into single space
        s = s.replace(Regex("""[ \t]+"""), " ").trim()

        return s
    }

    private fun resolveFractions(latex: String): String {
        var result = latex
        val fracRegex = Regex("""\\frac\{((?:[^{}]|\{[^{}]*\})*)\}\{((?:[^{}]|\{[^{}]*\})*)\}""")
        var match = fracRegex.find(result)
        var limit = 0
        while (match != null && limit < 10) {
            val num = formatLatexToUnicode(match.groupValues[1])
            val den = formatLatexToUnicode(match.groupValues[2])
            val cleanNum = if (num.contains("+") || num.contains("-") || num.contains(" ")) "($num)" else num
            val cleanDen = if (den.contains("+") || den.contains("-") || den.contains(" ")) "($den)" else den
            result = result.replaceRange(match.range, "$cleanNum / $cleanDen")
            match = fracRegex.find(result)
            limit++
        }
        return result
    }

    /**
     * Builds an AnnotatedString from a LaTeX math formula with styled Serif math font.
     */
    fun formatToAnnotatedString(latex: String): AnnotatedString {
        val formatted = formatLatexToUnicode(latex)
        return buildAnnotatedString {
            pushStyle(
                SpanStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Normal,
                    fontSize = 14.5.sp,
                    color = Color(0xFF0F172A)
                )
            )
            append(formatted)
            pop()
        }
    }
}

/**
 * Native Jetpack Compose component that renders Display Math equations in an elegant,
 * horizontal-scrollable container with soft contrast and a copy formula action.
 */
@Composable
fun DisplayMathCard(
    mathLatex: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatted = StudyBuddyMathEngine.formatLatexToUnicode(mathLatex)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("display_math_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Math Formula", formatted)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Formula copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy formula",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
