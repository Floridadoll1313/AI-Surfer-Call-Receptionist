package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.OceanWaveHeader
import com.example.ui.theme.OceanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val products by viewModel.products.collectAsStateWithLifecycle()
  val faqs by viewModel.faqs.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Products, 1: Business FAQ
  var showAddProductDialog by remember { mutableStateOf(false) }
  var showAddFaqDialog by remember { mutableStateOf(false) }

  // Add Product form state
  var newName by remember { mutableStateOf("") }
  var newPrice by remember { mutableStateOf("") }
  var newCategory by remember { mutableStateOf("AI Implementation Sprint") }
  var newDesc by remember { mutableStateOf("") }
  var newLevel by remember { mutableStateOf("Stage: Implement") }

  // Add FAQ form state
  var newFaqCat by remember { mutableStateOf("General") }
  var newFaqQ by remember { mutableStateOf("") }
  var newFaqA by remember { mutableStateOf("") }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.abyss)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      OceanWaveHeader(
        title = "AI Products & Knowledge Hub",
        subtitle = "Official Catalog from https://otdaisurfer.surf"
      )

      SecondaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = colors.deepBlue,
        contentColor = colors.turquoise,
        indicator = {
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(selectedTab),
            color = colors.turquoise
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("AI Solutions (${products.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
          selectedContentColor = colors.turquoise,
          unselectedContentColor = colors.textSecondary
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Business Q&A (${faqs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
          selectedContentColor = colors.turquoise,
          unselectedContentColor = colors.textSecondary
        )
      }

      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
              border = androidx.compose.foundation.BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.5f))
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = colors.cyan.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(20.dp))
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Live Catalog: otdaisurfer.surf",
                      color = colors.textPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                    Text(
                      text = "AI Receptionist is grounded in our official business solutions",
                      color = colors.cyan,
                      fontSize = 11.sp
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { viewModel.syncProductsFromWebsite() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Catalog", color = Color.White, fontSize = 11.sp)
                  }

                  Button(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://otdaisurfer.surf"))
                      try { context.startActivity(intent) } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.turquoise)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Visit Website", color = colors.turquoise, fontSize = 11.sp)
                  }
                }
              }
            }
          }

          items(products, key = { it.id }) { product ->
            ProductItemCard(
              product = product,
              onAskAi = {
                viewModel.simulateIncomingCall(
                  callerName = "Prospective Client",
                  callerNumber = "+1 (555) 789-WAVE",
                  initialQuery = "Aloha! I saw ${product.name} on otdaisurfer.surf. Can you tell me what it includes and how it works?"
                )
              },
              onOpenLink = { url ->
                val targetUrl = url.ifBlank { "https://otdaisurfer.surf" }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                try { context.startActivity(intent) } catch (e: Exception) {}
              }
            )
          }

          item {
            Spacer(modifier = Modifier.height(64.dp))
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(faqs, key = { it.id }) { faq ->
            FaqItemCard(
              faq = faq,
              onAskAi = {
                viewModel.simulateIncomingCall(
                  callerName = "Curious Surfer",
                  callerNumber = "+1 (555) 432-WAVE",
                  initialQuery = faq.question
                )
              }
            )
          }

          item {
            Spacer(modifier = Modifier.height(64.dp))
          }
        }
      }
    }

    // Floating Action Button to Add Product or FAQ
    FloatingActionButton(
      onClick = {
        if (selectedTab == 0) showAddProductDialog = true else showAddFaqDialog = true
      },
      containerColor = colors.turquoise,
      contentColor = colors.abyss,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .testTag("add_item_fab")
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Item")
    }
  }

  // Add Product Dialog
  if (showAddProductDialog) {
    AlertDialog(
      onDismissRequest = { showAddProductDialog = false },
      containerColor = colors.deepBlue,
      title = { Text("Add Surf Product / Service", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Product Name") },
            placeholder = { Text("e.g. Sunset Surfboard Wax") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newPrice,
              onValueChange = { newPrice = it },
              label = { Text("Price ($)") },
              placeholder = { Text("35.00") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            OutlinedTextField(
              value = newCategory,
              onValueChange = { newCategory = it },
              label = { Text("Category") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = newDesc,
            onValueChange = { newDesc = it },
            label = { Text("Description") },
            placeholder = { Text("Explain features, specs, and benefits for the AI...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val priceVal = newPrice.toDoubleOrNull() ?: 25.0
            viewModel.addNewProduct(newName, priceVal, newCategory, newDesc, newLevel)
            newName = ""
            newPrice = ""
            newDesc = ""
            showAddProductDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise)
        ) {
          Text("Add to AI Catalog", color = colors.abyss, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddProductDialog = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      }
    )
  }

  // Add FAQ Dialog
  if (showAddFaqDialog) {
    AlertDialog(
      onDismissRequest = { showAddFaqDialog = false },
      containerColor = colors.deepBlue,
      title = { Text("Add Business Q&A", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = newFaqCat,
            onValueChange = { newFaqCat = it },
            label = { Text("Category") },
            placeholder = { Text("e.g. Parking, Rentals, Weather") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = newFaqQ,
            onValueChange = { newFaqQ = it },
            label = { Text("Question") },
            placeholder = { Text("What question will callers ask?") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = newFaqA,
            onValueChange = { newFaqA = it },
            label = { Text("Official Business Answer") },
            placeholder = { Text("What should the AI receptionist answer?") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.addNewFaq(newFaqCat, newFaqQ, newFaqA, newFaqCat.lowercase())
            newFaqCat = "General"
            newFaqQ = ""
            newFaqA = ""
            showAddFaqDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise)
        ) {
          Text("Save to AI Knowledge", color = colors.abyss, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddFaqDialog = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      }
    )
  }
}

@Composable
fun ProductItemCard(
  product: ProductEntity,
  onAskAi: () -> Unit,
  onOpenLink: (String) -> Unit
) {
  val colors = OceanTheme.colors
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("product_item_${product.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
    border = androidx.compose.foundation.BorderStroke(1.dp, colors.teal.copy(alpha = 0.4f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (product.popularBadge.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = colors.goldenSun.copy(alpha = 0.2f),
              modifier = Modifier.padding(bottom = 4.dp)
            ) {
              Text(
                text = "★ ${product.popularBadge}",
                color = colors.goldenSun,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = product.name,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "${product.category} • ${product.surfLevel}",
            color = colors.cyan,
            fontSize = 11.sp
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = colors.card,
          border = androidx.compose.foundation.BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.5f))
        ) {
          val priceText = if (product.price == 0.0) "FREE" else "$${"%.0f".format(product.price)}"
          Text(
            text = priceText,
            color = if (product.price == 0.0) colors.seaGreen else colors.turquoise,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = product.description,
        color = colors.textSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onAskAi,
          colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier.weight(1.1f)
        ) {
          Icon(imageVector = Icons.Default.Surfing, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Test AI on Product", color = colors.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (product.directUrl.isNotBlank()) {
          Button(
            onClick = { onOpenLink(product.directUrl) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(0.9f)
          ) {
            val buttonLabel = when {
              product.ctaText.isNotBlank() -> product.ctaText
              product.price == 0.0 -> "Start Free"
              product.directUrl.contains("stripe") -> "Buy ($${"%.0f".format(product.price)})"
              product.directUrl.contains("calendly") -> "Book Call"
              else -> "Website"
            }
            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(buttonLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun FaqItemCard(
  faq: FaqEntity,
  onAskAi: () -> Unit
) {
  val colors = OceanTheme.colors
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
    border = androidx.compose.foundation.BorderStroke(1.dp, colors.teal.copy(alpha = 0.4f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = colors.cyan.copy(alpha = 0.15f),
        modifier = Modifier.padding(bottom = 6.dp)
      ) {
        Text(
          text = faq.category.uppercase(),
          color = colors.cyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      Text(
        text = faq.question,
        color = colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = faq.answer,
        color = colors.textSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onAskAi,
        colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(imageVector = Icons.Default.QuestionAnswer, contentDescription = null, tint = colors.turquoise, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Simulate Call Inquiring About This", color = colors.turquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
