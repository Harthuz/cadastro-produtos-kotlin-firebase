@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.app_firebase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@ExperimentalMaterial3Api
@Composable
fun ListarProdutosScreen(onBack: () -> Unit) {
    val db = Firebase.firestore
    val produtos = remember { mutableStateListOf<Produto>() }
    val context = LocalContext.current

    // LaunchedEffect para buscar os dados do Firestore apenas uma vez
    LaunchedEffect(Unit) {
        db.collection("produtos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Erro ao ouvir atualizações: $e")
                    Toast.makeText(context, "Erro ao carregar produtos.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaProdutos = snapshot.documents.map { document ->
                        Produto(
                            id = document.id,
                            nome = document.getString("nome") ?: "",
                            quantidade = document.getLong("quantidade")?.toInt() ?: 0,
                            descricao = document.getString("descricao") ?: ""
                        )
                    }
                    produtos.clear()
                    produtos.addAll(listaProdutos)
                }
            }
    }

    // Estado para o diálogo de edição
    var produtoEmEdicao by remember { mutableStateOf<Produto?>(null) }
    var nome by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

    fun limparCamposEdicao() {
        produtoEmEdicao = null
        nome = ""
        quantidade = ""
        descricao = ""
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        modifier = Modifier.background(brush = gradient)
    ) { innerPadding ->
        if (produtos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhum produto cadastrado.")
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = gradient)
            ) {
                items(produtos, key = { it.id }) { produto -> // Usar ID como chave
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = produto.nome,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Quantidade: ${produto.quantidade}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (produto.descricao.isNotBlank()) {
                                    Text(
                                        text = produto.descricao,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Botão editar
                            IconButton(onClick = {
                                produtoEmEdicao = produto
                                nome = produto.nome
                                quantidade = produto.quantidade.toString()
                                descricao = produto.descricao
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }

                            // Botão excluir
                            IconButton(onClick = {
                                db.collection("produtos").document(produto.id) // Usar o ID real do documento
                                    .delete()
                                    .addOnSuccessListener {
                                        // A lista será atualizada automaticamente pelo SnapshotListener
                                        Toast.makeText(context, "Produto excluído.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        println("Erro ao excluir produto: ${e.message}")
                                        Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                            }
                        }
                    }
                }
            }
        }
    }

    if (produtoEmEdicao != null) {
        AlertDialog(
            onDismissRequest = { limparCamposEdicao() },
            title = { Text("Editar Produto") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Produto") }
                        // O nome (ID antigo) não deve ser editável se for a chave primária.
                        // Agora que usamos o ID, podemos permitir a edição do nome.
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantidade,
                        onValueChange = { quantidade = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val produtoOriginal = produtoEmEdicao
                    if (produtoOriginal != null) {
                        val dadosAtualizados = mapOf(
                            "nome" to nome,
                            "quantidade" to (quantidade.toIntOrNull() ?: 0),
                            "descricao" to descricao
                        )

                        db.collection("produtos").document(produtoOriginal.id) // Usar o ID real
                            .update(dadosAtualizados)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Produto atualizado!", Toast.LENGTH_SHORT).show()
                                limparCamposEdicao()
                            }
                            .addOnFailureListener { e ->
                                println("Erro ao atualizar produto: ${e.message}")
                                Toast.makeText(context, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { limparCamposEdicao() }) { Text("Cancelar") }
            }
        )
    }
}

data class Produto(
    val id: String, // Adicionar ID para referenciar o documento corretamente
    val nome: String,
    val quantidade: Int,
    val descricao: String
)

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ListarProdutosScreenPreview() {
    // Esta preview não vai buscar dados reais, é apenas para layout
    ListarProdutosScreen(onBack = {})
}