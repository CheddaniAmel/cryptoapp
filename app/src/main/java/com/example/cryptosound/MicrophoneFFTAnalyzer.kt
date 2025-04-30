package com.example.cryptosound

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class MicrophoneFFTAnalyzer {
    private val bufferSize = AudioRecord.getMinBufferSize(
        44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        44100,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isRunning = false

    private val _fftData = MutableStateFlow(FloatArray(64))
    val fftData: StateFlow<FloatArray> = _fftData

    fun start() {
        if (isRunning) return
        isRunning = true
        audioRecord.startRecording()

        scope.launch {
            val buffer = ShortArray(bufferSize)
            while (isRunning) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                val real = FloatArray(read) { buffer[it].toFloat() }
                val imag = FloatArray(read) { 0f }

                // Basic FFT approximation (you can improve this with real FFT)
                val spectrum = FloatArray(64) { 0f }
                for (i in spectrum.indices) {
                    val j = i * read / spectrum.size
                    if (j < real.size) {
                        spectrum[i] = kotlin.math.abs(real[j])
                    }
                }

                _fftData.value = spectrum
                delay(16)
            }
        }
    }

    fun stop() {
        isRunning = false
        audioRecord.stop()
        audioRecord.release()
    }
}
