@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.app_firebase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarProdutosScreen(onBack: () -> Unit) {
    val db = Firebase.firestore
    val produtos = remember { mutableStateListOf<Produto>() }

    LaunchedEffect(Unit) {
        try {
            val result = db.collection("produtos").get().await()
            val listaProdutos = result.map { document ->
                Produto(
                    document.getString("nome") ?: "",
                    document.getLong("quantidade")?.toInt() ?: 0,
                    document.getString("descricao") ?: ""
                )
            }
            produtos.addAll(listaProdutos)
        } catch (e: Exception) {
            // Tratar exceção (ex: exibir mensagem de erro)
            println("Erro ao buscar produtos: ${e.message}")
        }
    }

    var produtoEmEdicao by remember { mutableStateOf<Produto?>(null) }
    var nome by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        modifier = Modifier.background(brush = gradient)
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
        ) {
            items(produtos, key = { it.nome }) { produto ->
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
                            db.collection("produtos").document(produto.nome) // Assumindo que o nome é o ID
                                .delete()
                                .addOnSuccessListener {
                                    produtos.remove(produto)
                                    // Produto excluído com sucesso
                                }
                                .addOnFailureListener { e ->
                                    // Erro ao excluir produto
                                    println("Erro ao excluir produto: ${e.message}")
                                }
                            val productData = hashMapOf(
                                "nome" to produto.nome,
                                "quantidade" to produto.quantidade,
                                "descricao" to produto.descricao
                            )
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }
        }
    }

    if (produtoEmEdicao != null) {
        AlertDialog(
            onDismissRequest = { produtoEmEdicao = null },
            title = { Text("Editar Produto") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Produto") }
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
                    val index = produtos.indexOf(produtoEmEdicao)
                    val produtoAtualizado = Produto(
                        nome,
                        quantidade.toIntOrNull() ?: 0,
                        descricao
                    )
                    
                    if (index >= 0) { 
                        val productData = hashMapOf(
                            "nome" to produtoAtualizado.nome,
                            "quantidade" to produtoAtualizado.quantidade,
                            "descricao" to produtoAtualizado.descricao
                        )
                        db.collection("produtos").document(produtoEmEdicao!!.nome) // Assumindo que o nome é o ID
                            .set(productData)
                            .addOnSuccessListener {
                                produtos[index] = produtoAtualizado
                                produtoEmEdicao = null
                                // Produto atualizado com sucesso
                            }
                            .addOnFailureListener { e ->
                                // Erro ao atualizar produto
                                println("Erro ao atualizar produto: ${e.message}")
                            }
                    } else {
                        produtoEmEdicao = null
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { produtoEmEdicao = null }) { Text("Cancelar") }
            }
        )
    }
}

data class Produto(
    val nome: String,
    val quantidade: Int,
    val descricao: String
)

@Preview
@Composable
fun ListarProdutosScreenPreview() {
    ListarProdutosScreen(onBack = {})
}