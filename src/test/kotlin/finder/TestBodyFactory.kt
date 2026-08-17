package finder

import java.util.Random

fun randomBody(seed: Long, length: Int): String {
    val rnd = Random(seed)
    val sb = StringBuilder(length + 16)
    while (sb.length < length) {
        repeat(3 + rnd.nextInt(6)) { sb.append('a' + rnd.nextInt(26)) }
        sb.append(' ')
    }
    return sb.substring(0, length)
}

fun nearMissOf(base: String, cutRatio: Int = 8): String {
    val cut = base.length / cutRatio
    val at = base.length / 3
    val filler = "qjxzv"
    return base.take(at) + filler.repeat(cut / filler.length + 1).take(cut) + base.substring(at + cut)
}