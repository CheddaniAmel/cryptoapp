package com.example.cryptosound


import android.media.AudioFormat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 Prevent screen capture
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        requestAudioPermission()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) tts.language = Locale.US
        }

        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "record") {
                composable("record") {
                    RecordingScreen(navController)
                }
                composable("result") {
                    ResultScreen(tts, navController)
                }
            }
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }


    @Composable
    fun RecordingScreen(navController: NavController) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var isRecording by remember { mutableStateOf(false) }
        var timerText by remember { mutableStateOf("00:00:00") }
        var startTime by remember { mutableStateOf(0L) }

        val rsaKeys = remember { HybridCrypto.generateRSAKeyPair() }

        var originalText by remember { mutableStateOf("") }
        var encryptedText by remember { mutableStateOf("") }
        var decryptedText by remember { mutableStateOf("") }

        // Timer logic
        LaunchedEffect(isRecording) {
            if (isRecording) {
                startTime = SystemClock.elapsedRealtime()
                while (isRecording) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    val seconds = (elapsed / 1000) % 60
                    val minutes = (elapsed / (1000 * 60)) % 60
                    val hours = (elapsed / (1000 * 60 * 60))
                    timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    kotlinx.coroutines.delay(1000)
                }
            }
        }

        FuturisticVoiceScreen(

            isRecording = isRecording,
            timerText = timerText,
            onRecordStart = {
                isRecording = true
                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!spokenText.isNullOrEmpty()) {
                            originalText = spokenText
                            isRecording = false
                            coroutineScope.launch {
                                val aesKey = HybridCrypto.generateAESKey()
                                val encrypted = HybridCrypto.encryptMessage(spokenText, aesKey)
                                val encryptedKey = HybridCrypto.encryptAESKey(aesKey, rsaKeys.public)
                                encryptedText = HybridCrypto.toBase64(encrypted)
                                val decryptedAESKey = HybridCrypto.decryptAESKey(encryptedKey, rsaKeys.private)
                                decryptedText = HybridCrypto.decryptMessage(encrypted, decryptedAESKey)
                                navController.navigate("result") {
                                    popUpTo("record") { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Could not understand speech", Toast.LENGTH_SHORT).show()
                            isRecording = false
                        }
                    }

                    override fun onError(error: Int) {
                        Toast.makeText(context, "Speech error: $error", Toast.LENGTH_SHORT).show()
                        isRecording = false
                    }

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer.startListening(recognizerIntent)
            },
            onRecordStop = {
                speechRecognizer.stopListening()
                isRecording = false
            }
        )

        // Store result in a shared ViewModel or Singleton (simplified here with rememberSaveable)
        LocalStorage.originalText = originalText
        LocalStorage.encryptedText = encryptedText
        LocalStorage.decryptedText = decryptedText
    }

    @Composable
    fun ResultScreen(tts: TextToSpeech, navController: NavController) {
        val original = LocalStorage.originalText
        val encrypted = LocalStorage.encryptedText
        val decrypted = LocalStorage.decryptedText

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🗣️ Original Text:", fontWeight = FontWeight.Bold, color = Color.Black)
            Text(original, color = Color.Black)

            Spacer(modifier = Modifier.height(16.dp))
            Text("🔐 Encrypted (Base64):", fontWeight = FontWeight.Bold, color = Color.Black)
            Text(encrypted, color = Color.Black)

            Spacer(modifier = Modifier.height(16.dp))
            Text("🔓 Decrypted Text:", fontWeight = FontWeight.Bold, color = Color.Black)
            Text(decrypted, color = Color.Black)

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                tts.speak(encrypted, TextToSpeech.QUEUE_FLUSH, null, null)
            }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Encrypted")
                Spacer(Modifier.width(8.dp))
                Text("Read Encrypted")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                tts.speak(decrypted, TextToSpeech.QUEUE_FLUSH, null, null)
            }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Decrypted")
                Spacer(Modifier.width(8.dp))
                Text("Read Decrypted")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                navController.navigate("record") {
                    popUpTo("record") { inclusive = true }
                }
            }) {
                Text("🔁 Record Again")
            }




        }
    }
}

object LocalStorage {
    var originalText: String = ""
    var encryptedText: String = ""
    var decryptedText: String = ""
}

@Composable
fun LiveWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 64,
    barColor: Color = Color(0xFF00B6D4)
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedHeights = List(barCount) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900 + (i * 10), easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Canvas(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val barWidth = size.width / (barCount * 1.5f)
        animatedHeights.forEachIndexed { i, height ->
            val x = i * (barWidth * 1.5f)
            val barHeight = size.height * height.value
            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
    }
}

@Composable
fun VideoBackground(modifier: Modifier = Modifier, videoUri: Uri) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            val playerView = PlayerView(context).apply {
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }

            playerView.player = player
            playerView
        }
    )
}
@Composable
fun GlowingPulseButton(
    onClick: () -> Unit,
    isRecording: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        // ✨ Glow animé autour
        Canvas(modifier = Modifier
            .size(120.dp * pulse)
        ) {
            drawCircle(
                color = Color(0x10A9C7).copy(alpha = 0.4f),
                radius = size.minDimension / 2,
                style = Stroke(width = 8f)
            )
        }

        // 🎤 Le vrai bouton au centre
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isRecording) Color.Red else Color(0xFF00B6D4)

            ),
            modifier = Modifier
                .size(80.dp)
                .shadow(12.dp, CircleShape)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}


@Composable
fun rememberAudioAmplitude(): State<Int> {
    val amplitude = remember { mutableStateOf(0) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()

            while (true) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val max = buffer.maxOf { it.toInt().absoluteValue }
                    amplitude.value = max
                }
            }
        }
    }

    return amplitude
}


// ✅ Corrected FuturisticVoiceScreen with VideoBackground
@Composable
fun FuturisticVoiceScreen(
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    isRecording: Boolean,
    timerText: String
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        )
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F111A), Color(0xFF1B1E2C))
    )

    val context = LocalContext.current
    val videoUri = Uri.parse("android.resource://${context.packageName}/raw/bgg")

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // 🎥 Vidéo d'arrière-plan
        VideoBackground(
            videoUri = videoUri,
            modifier = Modifier.fillMaxSize()
        )

        // 🎨 Contenu par-dessus la vidéo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 50.dp)

                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "Recording..." else "Hello, would you like to record now?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 40.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (isRecording) {
                    LiveWaveform(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(timerText, color = Color.White, fontSize = 18.sp)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp),

                horizontalArrangement = Arrangement.Center
            ) {
                val buttonColor = if (isRecording) Color.Red else Color(0xFF4E8CFF)

                GlowingPulseButton(
                    onClick = {
                        if (isRecording) onRecordStop() else onRecordStart()
                    },
                    isRecording = isRecording
                )

            }
        }
    }
}


