package com.example.autapp.ui.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.autapp.data.models.CourseMaterial


@Composable
fun CourseMaterialScreen(
    courseId: Int,
    viewModel: CourseMaterialViewModel,
    paddingValues: PaddingValues,
    isTeacher: Boolean,
    onEditMaterial: (CourseMaterial) -> Unit,
    onDeleteMaterial: (CourseMaterial) -> Unit

) {
    // Collect the list of course materials from the ViewModel
    val materialList by viewModel.materials.collectAsState()
    var materialToDelete by remember { mutableStateOf<CourseMaterial?>(null) }

    val context = LocalContext.current


    // Load materials whenever the courseId changes
    LaunchedEffect(courseId) {
        viewModel.loadMaterialsForCourse(courseId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Screen title
        Text(
            text = "Course Materials",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of materials in a scrollable list
        if (materialList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "No materials available for this course.",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(materialList) { material ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = material.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Type: ${material.type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = material.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!material.contentUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Open Resource",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(material.contentUrl)
                                        )
                                        context.startActivity(intent)
                                    }
                                )
                            }

                            if (isTeacher) {
                                Row {
                                    IconButton(onClick = { onEditMaterial(material) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { materialToDelete = material }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        materialToDelete?.let { material ->
            AlertDialog(
                onDismissRequest = { materialToDelete = null },
                title = { Text("Delete Material") },
                text = { Text("Are you sure you want to delete \"${material.title}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMaterial(material)
                        materialToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { materialToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
