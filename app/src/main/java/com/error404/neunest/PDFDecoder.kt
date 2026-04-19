package com.error404.neunest

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

data class Chunk(
    val id: String,
    val text: String,
    val pageStart: Int,
    val pageEnd: Int
)

class Chunker(
    private val maxChars: Int = 1200,
    private val overlap: Int = 200
) {

    fun chunk(pages: List<PDFDecoder.PageText>): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        val buffer = StringBuilder()

        var startPage = pages.firstOrNull()?.page ?: 1
        var lastPage = startPage
        var idx = 0

        fun flush() {
            if (buffer.isNotEmpty()) {
                chunks.add(
                    Chunk(
                        id = "chunk_${idx++}",
                        text = buffer.toString().trim(),
                        pageStart = startPage,
                        pageEnd = lastPage
                    )
                )
            }
        }

        for (p in pages) {
            val text = p.text

            if (buffer.isEmpty()) startPage = p.page
            lastPage = p.page

            if (buffer.length + text.length > maxChars) {
                // flush current
                flush()

                // keep overlap from previous chunk
                val overlapText = buffer.takeLast(overlap)
                buffer.clear()
                buffer.append(overlapText)
                startPage = p.page
            }

            buffer.append("\n\n").append(text)
        }

        flush()
        return chunks
    }
}

class KeywordRetriever {

    fun search(query: String, chunks: List<Chunk>, k: Int = 3): List<Chunk> {
        val terms = query.lowercase().split("\\W+".toRegex())
            .filter { it.length > 2 }

        return chunks.map { chunk ->

            val text = chunk.text.lowercase()

            var score = 0

            for (term in terms) {
                if (text.contains(term)) score += 2
            }

            // bonus: shorter chunks preferred
            score += (500 - chunk.text.length).coerceAtLeast(0) / 100

            chunk to score

        }.sortedByDescending { it.second }
            .take(k)
            .map { it.first }
    }

}


class PDFDecoder {
    data class PageText(val page: Int, val text: String)

    fun extractAll(filePath: String): List<PageText> {
        val doc = PDDocument.load(File(filePath))
        val stripper = PDFTextStripper()

        val pages = mutableListOf<PageText>()
        for (i in 1..doc.numberOfPages) {
            stripper.startPage = i
            stripper.endPage = i
            val raw = stripper.getText(doc)
            val cleaned = normalize(raw)
            if (cleaned.isNotBlank()) {
                pages.add(PageText(i, cleaned))
            }
        }

        doc.close()
        return pages
    }

    private fun normalize(s: String): String {
        return s
            .replace(Regex("-\\n"), "")        // fix hyphen line breaks
            .replace(Regex("\\n{2,}"), "\n\n") // collapse excessive newlines
            .trim()
    }
}