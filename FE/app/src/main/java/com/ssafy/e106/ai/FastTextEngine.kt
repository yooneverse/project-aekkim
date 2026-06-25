package com.ssafy.e106.ai
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
class FastTextEngine {
    private companion object {
        private const val TAG_FT = "AEKKIM_AI_FT"
        private const val SUBSCRIPTION_LABEL = "__label__subscription"
    }
    // Args 파싱 결과 — initialize() 에서 확정됨
    private var dim: Int = 0
    private var bucket: Int = 0
    private var minn: Int = 0
    private var maxn: Int = 0
    // 입력/출력 행렬 (row-major Array<FloatArray>)
    private var inputMatrix: Array<FloatArray> = emptyArray()
    private var outputMatrix: Array<FloatArray> = emptyArray()
    // Dictionary 레이블 목록 및 vocabSize
    private var labels: List<String> = emptyList()
    private var vocabSize: Int = 0
    private var initialized = false
    // resolve()가 단일 코루틴 컨텍스트(Dispatchers.Default)에서 순차 실행되므로
    // hiddenVecBuffer/scoreBuffer 재사용이 스레드 안전하다.
    private var hiddenVecBuffer = FloatArray(0)
    private var scoreBuffer = FloatArray(0)
    fun initialize(ftModelPath: String) {
        Log.i(TAG_FT, "FastText 모델 로드 시작: ft_model.bin")
        try {
            val bytes = File(ftModelPath).readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            // ── 파일 헤더 (매직 넘버 4B + 버전 4B = 8바이트) ─────────────
            val magic   = buf.int  // 매직 넘버 (793712314) — 건너뜀
            val version = buf.int  // 버전 (12) — 건너뜀
            Log.d(TAG_FT, "ft_model.bin 헤더: magic=${magic}, version=${version}")
            check(magic == 793712314) {
                "ft_model.bin 매직 넘버 불일치: ${magic} (기대값: 793712314) — 잘못된 파일"
            }
            // ── Args 파싱 (12 × Int32 + 1 × Double = 56바이트) ──────────
            dim             = buf.int
            buf.int   // ws
            buf.int   // epoch
            buf.int   // minCount
            buf.int   // neg
            buf.int   // wordNgrams
            buf.int   // loss
            buf.int   // model
            bucket          = buf.int
            minn            = buf.int
            maxn            = buf.int
            buf.int   // lrUpdateRate
            buf.double // t — 반드시 8바이트 double. 이전 버그의 원인이었던 필드
            Log.d(TAG_FT, "Args 파싱 완료: dim=$dim, bucket=$bucket, minn=$minn, maxn=$maxn")
            // dim 확정 후 hiddenVecBuffer 초기화
            hiddenVecBuffer = FloatArray(dim)
            // ── Dictionary 파싱 ──────────────────────────────────────────
            val size_         = buf.int
            val nwords_       = buf.int
            val nlabels_      = buf.int
            buf.long  // ntokens_
            val pruneidxSize_ = buf.long  // 반드시 8바이트 int64. 이전 버그의 원인이었던 필드
            Log.d(TAG_FT, "Dict 파싱 완료: size=$size_, nwords=$nwords_, nlabels=$nlabels_")
            val parsedLabels = mutableListOf<String>()
            for (i in 0 until size_) {
                // null-terminated UTF-8 문자열 읽기
                val wordBytes = mutableListOf<Byte>()
                while (true) {
                    val b = buf.get()
                    if (b == 0.toByte()) break
                    wordBytes.add(b)
                }
                val word = String(wordBytes.toByteArray(), Charsets.UTF_8)
                buf.long  // count (int64)
                val type = buf.get().toInt()  // type: 1바이트 int8 (0=word, 1=label)
                if (type == 1) parsedLabels.add(word)
            }
            if (pruneidxSize_ > 0L) {
                for (i in 0 until pruneidxSize_) {
                    buf.int  // key
                    buf.int  // value
                }
            }
            labels = parsedLabels
            scoreBuffer = FloatArray(nlabels_)
            buf.get()  // quant_ 플래그 1바이트 skip (FastText C++ Dictionary::load() 참조)
            // ── Input Matrix 파싱 ────────────────────────────────────────
            val inRows = buf.long.toInt()
            val inCols = buf.long.toInt()
            Log.d(TAG_FT, "Input Matrix 파싱 완료: rows=$inRows, cols=$inCols")
            inputMatrix = Array(inRows) { FloatArray(inCols) }
            for (r in 0 until inRows) for (c in 0 until inCols) inputMatrix[r][c] = buf.float
            vocabSize = inRows
            buf.get()  // quant_ 플래그 1바이트 skip (Input Matrix ~ Output Matrix 사이)
            // ── Output Matrix 파싱 ───────────────────────────────────────
            val outRows = buf.long.toInt()
            val outCols = buf.long.toInt()
            Log.d(TAG_FT, "Output Matrix 파싱 완료: rows=$outRows, cols=$outCols")
            outputMatrix = Array(outRows) { FloatArray(outCols) }
            for (r in 0 until outRows) for (c in 0 until outCols) outputMatrix[r][c] = buf.float
            // ── 파싱 완료 검증 ───────────────────────────────────────────
            val remaining = buf.remaining()
            if (remaining != 0) {
                Log.w(TAG_FT, "ft_model.bin 파싱 후 잔여 바이트: ${remaining}B — 파싱 오류 가능성")
            } else {
                Log.i(TAG_FT, "FastText 모델 로드 완료: ft_model.bin")
            }
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG_FT, "FastText 모델 로드 실패", e)
            initialized = false
            throw e
        }
    }
    fun predictSubscriptionProbability(normalizedText: String): Float {
        Log.d(TAG_FT, "FastText 입력: '$normalizedText'")
        if (!initialized) {
            Log.w(TAG_FT, "FastText 미초기화 상태, 0.0 반환")
            return 0.0f
        }
        return try {
            val prob = inferSubscriptionProbability(normalizedText)
            Log.d(TAG_FT, "FastText 결과: sub_prob=$prob")
            prob
        } catch (e: Exception) {
            Log.e(TAG_FT, "FastText 예측 실패, 0.0 반환", e)
            0.0f
        }
    }
    /**
     * 학습 파이프라인(pipeline_fasttext_training.py normalize_merchant())과 동일한 전처리.
     * Python: re.sub(r'[^A-Za-z0-9가-힣]', ' ', raw.upper()).strip()
     *
     * uppercase() 사용이 의도된 동작임. lowercase()로 변경 시 임베딩 미스매치로 성능 저하.
     * (파이프라인 내 raw.lower()는 merchantRaw 컬럼 저장용이며 FastText 입력과 무관)
     */
    fun normalizeText(raw: String): String {
        return raw
            .uppercase()
            .replace(Regex("[^A-Za-z0-9가-힣]"), " ")
            .replace(Regex(" +"), " ")
            .trim()
    }
    private fun inferSubscriptionProbability(text: String): Float {
        val inputVecs = inputMatrix.takeIf { it.isNotEmpty() } ?: return 0.0f
        val outputVecs = outputMatrix.takeIf { it.isNotEmpty() } ?: return 0.0f
        val labelList = labels.takeIf { it.isNotEmpty() } ?: return 0.0f
        val tokens = text.split(" ").filter { it.isNotBlank() }
        hiddenVecBuffer.fill(0f)
        val hiddenVec = hiddenVecBuffer
        var count = 0
        for (token in tokens) {
            // 서브워드 n-gram 해시로 inputMatrix 인덱스 조회
            val ngrams = computeSubwordHashes(token)
            for (h in ngrams) {
                val idx = (h % vocabSize).toInt()
                if (idx >= 0 && idx < inputVecs.size) {
                    addToVec(hiddenVec, inputVecs[idx])
                    count++
                }
            }
        }
        if (count == 0) return 0.0f
        for (i in hiddenVec.indices) hiddenVec[i] /= count.toFloat()
        for (lIdx in scoreBuffer.indices) {
            scoreBuffer[lIdx] = if (lIdx < outputVecs.size) dot(hiddenVec, outputVecs[lIdx]) else 0.0f
        }
        val scores = scoreBuffer
        val softmaxProbs = softmax(scores)
        val subIdx = labelList.indexOf(SUBSCRIPTION_LABEL)
        return if (subIdx >= 0) softmaxProbs[subIdx] else 0.0f
    }
    private fun computeSubwordHashes(word: String): List<Long> {
        val padded = "<$word>"
        val result = mutableListOf<Long>()
        for (start in padded.indices) {
            for (n in minn..maxn) {
                val end = start + n
                if (end > padded.length) break
                result.add(fnv1aHash(padded.substring(start, end)))
            }
        }
        return result
    }
    private fun fnv1aHash(s: String): Long {
        var h = 2166136261L
        for (c in s) {
            h = h xor c.code.toLong()
            h = (h * 16777619L) and 0xFFFFFFFFL
        }
        return h and Long.MAX_VALUE
    }
    private fun addToVec(dst: FloatArray, src: FloatArray) {
        val len = minOf(dst.size, src.size)
        for (i in 0 until len) dst[i] += src[i]
    }
    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0.0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) sum += a[i] * b[i]
        return sum
    }
    private fun softmax(scores: FloatArray): FloatArray {
        val max = scores.maxOrNull() ?: 0.0f
        val exps = FloatArray(scores.size) { exp((scores[it] - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return FloatArray(exps.size) { exps[it] / sum }
    }
}
