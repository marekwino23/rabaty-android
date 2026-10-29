package pl.vidad.rabaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

private const val GROUPS_URL = "https://vidad.webd.pl/rabaty/wp-json/rabaty/v1/groups"

private val Ink = Color(0xFF17211D)
private val Green = Color(0xFF16794B)
private val PaleGreen = Color(0xFFE5F5EC)
private val Page = Color(0xFFF6F7F5)
private val Muted = Color(0xFF68736D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RabatyApp() }
    }
}

data class GroupOffer(
    val id: Int,
    val title: String,
    val status: String,
    val productName: String,
    val productUrl: String,
    val regularPrice: String,
    val groupPrice: String,
    val currency: String,
    val memberCount: Int,
    val memberLimit: Int,
    val expires: String
) {
    val progress: Float get() = if (memberLimit > 0) (memberCount.toFloat() / memberLimit).coerceIn(0f, 1f) else 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun RabatyApp() {
    var offers by remember { mutableStateOf<List<GroupOffer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshed by remember { mutableStateOf(0) }

    LaunchedEffect(refreshed) {
        loading = true
        error = null
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val result = fetchGroups()
                offers = result
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Nie udało się pobrać ofert."
            } finally {
                loading = false
                executor.shutdown()
            }
        }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Green,
            background = Page,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Page,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(38.dp).background(Green, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("R", color = Color.White, fontWeight = FontWeight.Black, fontSize = 23.sp) }
                            Column {
                                Text("Rabaty", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink)
                                Text("Razem taniej", fontSize = 12.sp, color = Muted)
                            }
                        }
                    },
                    actions = {
                        Button(
                            onClick = { refreshed++ },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = PaleGreen, contentColor = Green),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Odśwież", modifier = Modifier.size(18.dp))
                            Text(" Odśwież")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Page)
                )
            }
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(8.dp))
                Surface(color = Ink, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("LEPSZA CENA W GRUPIE", color = Color(0xFF9FE0B9), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Kupuj razem.\nPłać mniej.", color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Dołącz do grupy i skorzystaj z ceny grupowej.", color = Color(0xFFD4DED8), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Aktywne grupy", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink)
                    Text(if (loading) "Ładowanie…" else "${offers.size} ofert", color = Muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Green)
                            Spacer(Modifier.height(12.dp))
                            Text("Pobieram oferty ze sklepu…", color = Muted)
                        }
                    }
                    error != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nie udało się pobrać ofert", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(error ?: "", color = Muted)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { refreshed++ }) { Text("Spróbuj ponownie") }
                        }
                    }
                    offers.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Na razie brak aktywnych grup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Sprawdź ponownie za chwilę.", color = Muted)
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(offers, key = { it.id }) { offer -> OfferCard(offer) }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
                Text("Oferty pobierane bezpośrednio ze sklepu vidad.webd.pl", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun OfferCard(offer: GroupOffer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(82.dp).background(PaleGreen, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(offer.productName.take(1).uppercase(), color = Green, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f)) {
                    Text(offer.productName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("${offer.memberCount} z ${offer.memberLimit} uczestników", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${offer.groupPrice} ${offer.currency}", color = Green, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        if (offer.regularPrice.isNotBlank()) {
                            Text("${offer.regularPrice} ${offer.currency}", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { offer.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Green,
                trackColor = PaleGreen
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Postęp grupy", color = Muted, fontSize = 12.sp)
                Text("${(offer.progress * 100).toInt()}%", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Surface(color = Page, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Green, CircleShape))
                    Text("  ${statusLabel(offer.status)}", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text("Grupa #${offer.id}", fontSize = 11.sp, color = Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Termin: ${formatExpiry(offer.expires)}", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Szczegóły i dołączanie będą dostępne w kolejnej wersji aplikacji.",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

private fun fetchGroups(): List<GroupOffer> {
    val connection = (URL(GROUPS_URL).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("Accept", "application/json")
    }
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("Serwer zwrócił HTTP $code")
        val array = parseArray(body)
        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val product = obj.optJSONObject("product") ?: JSONObject()
            GroupOffer(
                id = obj.optInt("id"),
                title = obj.optString("title", "Grupa zakupowa"),
                status = obj.optString("status", ""),
                productName = product.optString("name", obj.optString("title", "Produkt")),
                productUrl = product.optString("permalink", ""),
                regularPrice = product.optString("price", ""),
                groupPrice = obj.optString("price", "—"),
                currency = product.optString("currency", "PLN"),
                memberCount = obj.optInt("member_count", 0),
                memberLimit = obj.optInt("member_limit", 0),
                expires = obj.optString("expires_gmt", "")
            )
        }
    } finally {
        connection.disconnect()
    }
}

private fun parseArray(body: String): JSONArray {
    val trimmed = body.trim()
    if (trimmed.startsWith("[")) return JSONArray(trimmed)
    val root = JSONObject(trimmed)
    val candidates = listOf("groups", "data", "items")
    for (key in candidates) {
        val value = root.optJSONArray(key)
        if (value != null) return value
    }
    throw IllegalStateException("API zwróciło nieoczekiwany format JSON.")
}

private fun statusLabel(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "group-processing", "processing", "active" -> "Grupa w trakcie"
    "completed", "group-completed" -> "Grupa zakończona"
    "expired", "cancelled", "canceled" -> "Grupa nieaktywna"
    else -> if (status.isBlank()) "Grupa zakupowa" else status
}

private fun formatExpiry(value: String): String {
    if (value.isBlank()) return "brak danych"
    return try {
        val inputPatterns = listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd HH:mm:ss")
        var date: Date? = null
        for (pattern in inputPatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
                date = parser.parse(value)
                if (date != null) break
            } catch (_: Exception) { }
        }
        if (date == null) value else SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("pl", "PL")).format(date)
    } catch (_: Exception) { value }
}
