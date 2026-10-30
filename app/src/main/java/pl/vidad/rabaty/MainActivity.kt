
package pl.vidad.rabaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

private const val PRODUCTS_URL =
    "https://vidad.webd.pl/rabaty/wp-json/wc/store/v1/products"

private const val GROUPS_URL =
    "https://vidad.webd.pl/rabaty/wp-json/rabaty/v1/groups"

private val Green = Color(0xFF16794B)
private val DarkGreen = Color(0xFF153D2C)
private val PaleGreen = Color(0xFFE6F3EA)
private val Page = Color(0xFFF5F7F5)
private val Ink = Color(0xFF1B2921)
private val Muted = Color(0xFF748078)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RabatyHome()
        }
    }
}

data class StoreProduct(
    val id: Int,
    val name: String,
    val price: String,
    val regularPrice: String,
    val currency: String,
    val image: String,
    val url: String
)

data class GroupOffer(
    val id: Int,
    val name: String,
    val price: String,
    val regularPrice: String,
    val currency: String,
    val members: Int,
    val limit: Int,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabatyHome() {
    var products by remember { mutableStateOf<List<StoreProduct>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupOffer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Start") }

    LaunchedEffect(Unit) {
        loading = true
        error = null

        try {
            val result = withContext(Dispatchers.IO) {
                Pair(fetchProducts(), fetchGroups())
            }
            products = result.first
            groups = result.second
        } catch (e: Exception) {
            error = e.message ?: "Nie udało się pobrać danych."
        } finally {
            loading = false
        }
    }

    val filteredProducts = products.filter {
        it.name.contains(search, ignoreCase = true)
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            secondary = DarkGreen,
            background = Page,
            surface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Page,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Rabaty",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkGreen
                                )
                                Text(
                                    "Kupuj razem. Płać mniej.",
                                    fontSize = 11.sp,
                                    color = Muted
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectedTab = "Konto"
                            }) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = "Konto",
                                    tint = Green,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Page
                        )
                    )

                    ScrollableTabRow(
                        selectedTabIndex = listOf(
                            "Start", "Produkty", "Grupy", "Konto"
                        ).indexOf(selectedTab).coerceAtLeast(0),
                        containerColor = Page,
                        contentColor = Green,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        listOf("Start", "Produkty", "Grupy", "Konto")
                            .forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = {
                                        Text(
                                            tab,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                    }
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Szukaj produktów i promocji…")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Szukaj"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color(0xFFE0E7E1)
                        )
                    )
                }

                item {
                    PromoBanner()
                }

                if (error != null) {
                    item {
                        Text(
                            "Nie udało się pobrać danych: $error",
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                        Text(
                            "Sprawdź połączenie i spróbuj ponownie później.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }

                if (selectedTab == "Start" || selectedTab == "Produkty") {
                    item {
                        SectionTitle(
                            "Polecane produkty",
                            "${filteredProducts.size} produktów"
                        )
                    }

                    if (loading) {
                        item {
                            LoadingMessage()
                        }
                    } else if (filteredProducts.isEmpty()) {
                        item {
                            EmptyMessage("Nie znaleziono produktów.")
                        }
                    } else {
                        items(filteredProducts.chunked(2)) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { product ->
                                    ProductCard(
                                        product,
                                        Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (selectedTab == "Start" || selectedTab == "Grupy") {
                    item {
                        SectionTitle(
                            "Aktywne grupy zakupowe",
                            "${groups.size} grup"
                        )
                    }

                    if (loading) {
                        item {
                            LoadingMessage()
                        }
                    } else if (groups.isEmpty()) {
                        item {
                            EmptyMessage(
                                "Obecnie nie ma aktywnych grup."
                            )
                        }
                    } else {
                        items(groups, key = { it.id }) { group ->
                            GroupCard(group)
                        }
                    }
                }

                if (selectedTab == "Konto") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Green,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Twoje konto",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Logowanie i rejestrację podłączymy w kolejnym etapie.",
                                    color = Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen
        )
    ) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                "RABATY GRUPOWE",
                color = Color(0xFFA9E6BC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp
            )
            Text(
                "Więcej osób.\nNiższa cena.",
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Dołącz do zakupów i oszczędzaj razem z innymi.",
                color = Color(0xFFD7E6DC),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Text(
            subtitle,
            fontSize = 11.sp,
            color = Muted
        )
    }
}

@Composable
private fun ProductCard(
    product: StoreProduct,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            AsyncImage(
                model = product.image.ifBlank { null },
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(PaleGreen)
            )

            Spacer(Modifier.height(9.dp))

            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                color = Ink
            )

            Spacer(Modifier.height(5.dp))

            Text(
                product.price,
                color = Green,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )

            if (product.regularPrice.isNotBlank() &&
                product.regularPrice != product.price
            ) {
                Text(
                    product.regularPrice,
                    color = Muted,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupOffer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = Green
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    group.name,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }

            Text(
                "Cena grupowa: ${group.price} ${group.currency}",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Green
            )

            Text(
                "${group.members} z ${group.limit} uczestników",
                color = Muted,
                fontSize = 13.sp
            )

            LinearProgressIndicator(
                progress = {
                    if (group.limit > 0) {
                        (group.members.toFloat() / group.limit)
                            .coerceIn(0f, 1f)
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = Green,
                trackColor = PaleGreen
            )

            Text(
                "Grupa #${group.id} • ${group.status}",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun LoadingMessage() {
    Box(
        Modifier.fillMaxWidth().padding(25.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Green)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(
        message,
        color = Muted,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

private fun fetchProducts(): List<StoreProduct> {
    val array = requestArray(PRODUCTS_URL)

    return (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index)
            ?: return@mapNotNull null

        val prices = obj.optJSONObject("prices") ?: JSONObject()
        val images = obj.optJSONArray("images") ?: JSONArray()
        val image = images.optJSONObject(0)?.optString("src", "") ?: ""

        val currency = prices.optString("currency_code", "PLN")
        val minorUnit = prices.optInt("currency_minor_unit", 2)

        StoreProduct(
            id = obj.optInt("id"),
            name = obj.optString("name", "Produkt"),
            price = formatStorePrice(
                prices.optString("price", ""),
                minorUnit,
                currency
            ),
            regularPrice = formatStorePrice(
                prices.optString("regular_price", ""),
                minorUnit,
                currency
            ),
            currency = currency,
            image = image,
            url = obj.optString("permalink", "")
        )
    }
}

private fun fetchGroups(): List<GroupOffer> {
    val array = requestArray(GROUPS_URL)

    return (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index)
            ?: return@mapNotNull null

        val product = obj.optJSONObject("product") ?: JSONObject()

        GroupOffer(
            id = obj.optInt("id"),
            name = product.optString(
                "name",
                obj.optString("title", "Grupa zakupowa")
            ),
            price = obj.optString("price", "—"),
            regularPrice = product.optString("price", ""),
            currency = product.optString("currency", "PLN"),
            members = obj.optInt("member_count", 0),
            limit = obj.optInt("member_limit", 0),
            status = obj.optString("status", "")
        )
    }
}

private fun requestArray(address: String): JSONArray {
    val connection =
        (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }

    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val body = stream.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }

        if (code !in 200..299) {
            throw IllegalStateException("Błąd serwera: HTTP $code")
        }

        val trimmed = body.trim()

        if (trimmed.startsWith("[")) {
            return JSONArray(trimmed)
        }

        val root = JSONObject(trimmed)

        for (key in listOf("products", "groups", "data", "items")) {
            root.optJSONArray(key)?.let { return it }
        }

        throw IllegalStateException("Nieprawidłowa odpowiedź API.")
    } finally {
        connection.disconnect()
    }
}

private fun formatStorePrice(
    raw: String,
    minorUnit: Int,
    currency: String
): String {
    if (raw.isBlank()) return "Cena niedostępna"

    return try {
        val amount = raw.toBigDecimal()
            .movePointLeft(minorUnit)

        val formatter = NumberFormat.getNumberInstance(
            Locale("pl", "PL")
        ).apply {
            minimumFractionDigits = minorUnit
            maximumFractionDigits = minorUnit
        }

        "${formatter.format(amount)} $currency"
    } catch (_: Exception) {
        raw
    }
}
