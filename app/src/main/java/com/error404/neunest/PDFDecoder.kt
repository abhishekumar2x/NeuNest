package com.error404.neunest

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

// ────────────────────────────────────────────────────────────────────────────
// DATA TYPES
// ────────────────────────────────────────────────────────────────────────────

data class Chunk(
    val id: String,
    val text: String,
    val pageStart: Int,
    val pageEnd: Int
)

// ────────────────────────────────────────────────────────────────────────────
// CHUNKER
//
// Strategy: flatten all pages into one stream, split on sentence boundaries,
// then pack sentences into fixed-size windows with overlap.
// This avoids the old bug where a page larger than maxChars was never split.
// ────────────────────────────────────────────────────────────────────────────

class Chunker(
    private val maxChars: Int = 800,
    private val overlap: Int = 120
) {
    fun chunk(pages: List<PDFDecoder.PageText>): List<Chunk> {
        if (pages.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var idx = 0

        // Tag each sentence with the page it came from
        data class TaggedSentence(val text: String, val page: Int)

        val sentences = mutableListOf<TaggedSentence>()
        for (p in pages) {
            p.text
                .split(Regex("(?<=[.!?])\\s+|\\n\\n+"))
                .map { it.trim() }
                .filter { it.length > 10 } // skip header/footer fragments
                .forEach { sentences.add(TaggedSentence(it, p.page)) }
        }

        if (sentences.isEmpty()) return emptyList()

        val buffer = StringBuilder()
        var startPage = sentences.first().page
        var endPage = startPage

        fun flush() {
            val text = buffer.toString().trim()
            if (text.isNotEmpty()) {
                chunks.add(Chunk("chunk_${idx++}", text, startPage, endPage))
            }
        }

        for (s in sentences) {
            if (buffer.isEmpty()) {
                startPage = s.page
            }
            endPage = s.page

            val addition = if (buffer.isEmpty()) s.text else " ${s.text}"

            if (buffer.length + addition.length > maxChars && buffer.isNotEmpty()) {
                flush()
                // Carry overlap forward
                val prev = buffer.toString()
                val overlapText = if (prev.length > overlap) prev.takeLast(overlap) else prev
                buffer.clear()
                buffer.append(overlapText)
                startPage = s.page
            }

            buffer.append(if (buffer.isEmpty()) s.text else " ${s.text}")
        }

        flush()
        return chunks
    }
}

// ────────────────────────────────────────────────────────────────────────────
// KEYWORD RETRIEVER
//
// Fixes vs. original:
//  • Stop-words filtered so they don't pollute scores
//  • Term-frequency counted (not just presence)
//  • Basic suffix stemming: "run" matches "running", "runs"
//  • If NO chunk scores > 0 (terms simply not in the doc), return the first k
//    chunks as a fallback so the model still gets *some* context instead of
//    seeing an empty prompt and saying "Not found in document"
// ────────────────────────────────────────────────────────────────────────────

class KeywordRetriever {

    private val stopWords = setOf(
        "the", "and", "for", "are", "was", "were", "that", "this",
        "with", "has", "have", "had", "its", "from", "but", "not",
        "can", "all", "will", "been", "also", "into", "more", "than",
        "what", "which", "when", "where", "who", "how", "does", "did",
        "about", "their", "there", "they", "you", "your"
    )

    fun search(query: String, chunks: List<Chunk>, k: Int = 3): List<Chunk> {
        if (chunks.isEmpty()) return emptyList()

        val terms = query.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 && it !in stopWords }

        // No meaningful terms → return first k chunks (positional fallback)
        if (terms.isEmpty()) return chunks.take(k)

        val scored = chunks.map { chunk ->
            val text = chunk.text.lowercase()
            var score = 0

            for (term in terms) {
                // Exact term frequency
                var pos = 0
                while (true) {
                    val found = text.indexOf(term, pos)
                    if (found == -1) break
                    score += 3
                    pos = found + 1
                }
                // Stem match (prefix, min 4 chars) gives +1 per hit
                if (term.length >= 4) {
                    val stem = term.dropLast(1) // e.g. "running" → "runnin" catches "runs" won't but "runn" will
                    val stemShort = term.take(term.length - 2) // more aggressive stem
                    var s = 0
                    while (true) {
                        val found = text.indexOf(stemShort, s)
                        if (found == -1) break
                        score += 1
                        s = found + 1
                    }
                }
            }

            chunk to score
        }

        val hasMatch = scored.any { it.second > 0 }

        return if (hasMatch) {
            scored.sortedByDescending { it.second }.take(k).map { it.first }
        } else {
            // Fallback: first k chunks — the model can still try to answer
            chunks.take(k)
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// PDF DECODER
//
// Fix: use .use { } so PDDocument is always closed even if extraction throws.
// Also strips ligature / control characters that PDFBox sometimes emits on
// Android, which would silently corrupt chunk text and break keyword matching.
// ────────────────────────────────────────────────────────────────────────────

class PDFDecoder {
    data class PageText(val page: Int, val text: String)

    fun extractAll(filePath: String): List<PageText> {
        val pages = mutableListOf<PageText>()

        PDDocument.load(File(filePath)).use { doc ->
            val stripper = PDFTextStripper()
            for (i in 1..doc.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i
                val raw = stripper.getText(doc)
                val cleaned = normalize(raw)
                if (cleaned.isNotBlank()) {
                    pages.add(PageText(i, cleaned))
                }
            }
        }

        return pages
    }

    private fun normalize(s: String): String = s
        .replace(Regex("-\\n"), "")                          // fix hyphenated line-breaks
        .replace(Regex("\\r\\n|\\r"), "\n")                  // normalise line endings
        .replace(Regex("\\n{3,}"), "\n\n")                   // collapse blank lines
        .replace(Regex("[ \\t]+"), " ")                      // collapse horizontal whitespace
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "") // strip control chars
        .trim()
}