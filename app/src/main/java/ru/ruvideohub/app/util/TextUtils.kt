package ru.ruvideohub.app.util

import java.util.Locale
import kotlin.math.max

fun normalize(text: String): String = text.lowercase(Locale.getDefault())
    .replace('ё', 'е')
    .replace("[\\p{Punct}\\p{S}]".toRegex(), " ")
    .replace("\\s+".toRegex(), " ")
    .trim()

fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val prev = IntArray(b.length + 1) { it }
    val cur = IntArray(b.length + 1)
    for (i in a.indices) {
        cur[0] = i + 1
        for (j in b.indices) {
            val cost = if (a[i] == b[j]) 0 else 1
            cur[j + 1] = minOf(cur[j] + 1, prev[j + 1] + 1, prev[j] + cost)
        }
        for (j in prev.indices) prev[j] = cur[j]
    }
    return prev[b.length]
}

fun relevance(query: String, title: String): Int {
    val q = normalize(query)
    val t = normalize(title)
    if (t == q) return 1000
    if (t.startsWith(q)) return 900
    if (t.contains(q)) return 800
    val qt = q.split(' ').filter { it.isNotBlank() }
    val tt = t.split(' ').filter { it.isNotBlank() }
    val common = qt.count { qWord -> tt.any { it == qWord || it.startsWith(qWord) } }
    var score = common * 100
    val distance = levenshtein(q.replace(" ", ""), t.replace(" ", "")).coerceAtMost(12)
    score += max(0, 60 - distance * 5)
    return score
}

fun formatDuration(seconds: Long?): String? {
    if (seconds == null || seconds < 0) return null
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
