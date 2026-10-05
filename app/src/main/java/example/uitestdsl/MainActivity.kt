package example.uitestdsl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

object Tags {
    const val CODE = "promo_code"
    const val SUBMIT = "submit_promo"
    const val RESULT = "promo_result"
    const val LOAD_PRODUCTS = "load_products"
    const val PRODUCTS = "products"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val koin = GlobalContext.get()
        val addPromocode = koin.get<AddPromocode>()
        val getProducts = koin.get<GetProducts>()
        val flags = koin.get<FeatureFlags>()
        setContent {
            MaterialTheme {
                var code by remember { mutableStateOf("") }
                var result by remember { mutableStateOf("") }
                var products by remember { mutableStateOf("") }
                var submitting by remember { mutableStateOf(false) }
                var loading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                Scaffold { padding ->
                    Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Промокоды", style = MaterialTheme.typography.headlineMedium)
                        if (flags.isEnabled(Flags.PROMOCODE_ENABLED)) {
                            OutlinedTextField(code, { code = it }, label = { Text("Промокод") },
                                modifier = Modifier.testTag(Tags.CODE))
                            Button(enabled = code.isNotBlank() && !submitting, modifier = Modifier.testTag(Tags.SUBMIT),
                                onClick = {
                                    submitting = true
                                    scope.launch {
                                        try {
                                            result = when (val response = addPromocode(code)) {
                                                PromoResult.Accepted -> "Промокод применён"
                                                is PromoResult.Rejected -> response.message
                                            }
                                        } catch (e: CancellationException) { throw e }
                                        catch (_: Exception) { result = "Не удалось отправить промокод" }
                                        finally { submitting = false }
                                    }
                                }) { Text("Применить") }
                        } else Text("Промокоды недоступны")
                        Text(result, Modifier.testTag(Tags.RESULT))
                        Button(enabled = !loading, modifier = Modifier.testTag(Tags.LOAD_PRODUCTS), onClick = {
                            loading = true
                            scope.launch {
                                try { products = getProducts().joinToString("\n") }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { products = "Не удалось загрузить товары" }
                                finally { loading = false }
                            }
                        }) { Text("Загрузить товары") }
                        Text(products, Modifier.testTag(Tags.PRODUCTS))
                    }
                }
            }
        }
    }
}
