package com.example.plantbender

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plantbender.ui.theme.PlantBenderTheme
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.android.service.MqttService
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import kotlin.math.min
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantBenderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val data by GroundHumidityViewModel().data.observeAsState(initial = emptyList())
                    val lastData = if (data.isNotEmpty())
                        data.last().humidityValue.toDoubleOrNull() ?: 0.0
                    else
                        0.0

                    NonRegularGrayscaleWavesBackground()
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "PlantBender",
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.parkinsans_regular)),
                                fontSize = 36.sp,
                                letterSpacing = 2.sp,
                                color = Color(0xFF3D7337),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Soil\nHumidity:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 24.sp,
                                fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            CircularPercentageIndicator(
                                data = lastData,
                                modifier = Modifier.size(120.dp),
                                context = this@MainActivity
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ToggleButton(wateringLevel = lastData, context = this@MainActivity)
                        Spacer(modifier = Modifier.height(16.dp))
                        HistoricalRecordsTableHorizontal()
                    }
                }
            }
        }
    }
}

@Composable
fun CircularPercentageIndicator(data: Double, modifier: Modifier = Modifier, context: Context) {
    val percentage = data.toFloat()

    Canvas(modifier = modifier) {
        val size = min(size.width, size.height)
        val strokeWidth = size * 0.1f
        val radius = (size - strokeWidth) / 2

        val startColor = lerp(
            start = Color.Red,
            stop = Color.Green,
            fraction = percentage / 100f
        )
        val endColor = lerp(
            start = startColor.copy(alpha = 0.7f),
            stop = startColor.copy(alpha = 1f),
            fraction = 0.5f
        )
        val gradientBrush = Brush.sweepGradient(
            colors = listOf(startColor, endColor),
            center = Offset(size / 2, size / 2)
        )

        // Draw background circle
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = Offset(size / 2, size / 2),
            style = Stroke(width = strokeWidth)
        )

        // Draw gradient arc
        drawArc(
            brush = gradientBrush,
            startAngle = -90f,
            sweepAngle = 360f * (percentage / 100f),
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(width = strokeWidth)
        )

        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 40f
            color = android.graphics.Color.BLACK
        }


        // Draw percentage text
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "${percentage.toBigDecimal().setScale(2, RoundingMode.UP)}%",
                size / 2,
                size / 2 - (paint.ascent() + paint.descent()) / 2,
                android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = size * 0.2f
                    color = android.graphics.Color.BLACK
                    typeface = Typeface.createFromAsset(context.assets, "fonts/parkinsans_light.ttf")
                }
            )
        }
    }
}

@Composable
fun HistoricalRecordsTableHorizontal() {
    val data by GroundHumidityViewModel().data.observeAsState(initial = emptyList())
    val loading by GroundHumidityViewModel().loading.observeAsState(initial = false)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xA1C7C7C7)),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x803D7337))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Date",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Time",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Humidity (%)",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                            textAlign = TextAlign.Center
                        )
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(data.size) { index ->
                            val record = data.reversed()[index]

                            val rowBackgroundColor = if (index % 2 == 0) {
                                Color(0xFFE0E0E0)
                            } else {
                                Color(0xFFF5F5F5)
                            }

                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = rowBackgroundColor)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = record.date,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = record.time,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = record.humidityValue,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily(Font(R.font.parkinsans_light)),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NonRegularGrayscaleWavesBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val colors = listOf(
            Color(0xFFE7E7E7), // Very light
            Color(0xFFD3D3D3), // Light
            Color(0xFF9D9D9D), // Slightly darker
            Color(0xFF727272), // Medium light
            Color(0xFF575757)  // Darkest
        )

        val random = Random(42)

        val waveSpacing = height / (colors.size + 1)

        for (i in colors.indices) {
            val yOffset = (i + 1) * waveSpacing
            val path = Path().apply {
                moveTo(0f, yOffset)
                var currentX = 0f

                while (currentX < width) {
                    val waveWidth = random.nextInt(150, 300).toFloat()
                    val waveHeight = random.nextInt(50, 150).toFloat()
                    val controlX = currentX + waveWidth / 2
                    val controlY = random.nextInt(-50, 50).toFloat()

                    quadraticBezierTo(
                        controlX, yOffset + controlY,
                        currentX + waveWidth, yOffset + waveHeight
                    )
                    currentX += waveWidth
                }

                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors[i],
                        colors[i].copy(alpha = 0.7f)
                    )
                ),
                alpha = 0.8f
            )
        }
    }
}

@Composable
fun ToggleButton(wateringLevel: Double, context: Context) {
    val isOn = remember { mutableStateOf(false) }

    Button(
        onClick = {
            val newState = !isOn.value
            isOn.value = newState
            sendRequest(if (newState) "1" else "0", context)
        },
        enabled = wateringLevel < 50,
        colors = ButtonColors(
            if (isOn.value) Color(0xFF772821) else Color(0xFF3D7337),
            Color.White,
            Color.LightGray,
            Color.DarkGray
        )
    ) {
        Text(
            text = if (isOn.value) "Stop watering" else "Water the plant!",
            fontFamily = FontFamily(Font(R.font.parkinsans_light)),
            fontSize = 24.sp
        )
    }
}

fun sendRequest(value: String, context: Context) {
    RetrofitClient.apiService.sendActivation(value).enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) {
                Toast.makeText(context, "Request successful with value: $value", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            Toast.makeText(context, "Network failure: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlantBenderTheme {
        HistoricalRecordsTableHorizontal()
    }
}