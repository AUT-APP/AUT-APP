package com.example.autapp.ui.material

import android.content.ActivityNotFoundException
import android.content.Context
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
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.autapp.data.models.CourseMaterial
import com.example.autapp.util.MaterialValidator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope



@Composable
fun CourseMaterialScreen(
    courseId: String,
    viewModel: CourseMaterialViewModel,
    paddingValues: PaddingValues,
    isTeacher: Boolean,
    onEditMaterial: (CourseMaterial) -> Unit,
    onDeleteMaterial: (CourseMaterial) -> Unit

) {
    // Collect the list of course materials from the ViewModel
    val materialList by viewModel.materials.collectAsState()
    var materialToDelete by remember { mutableStateOf<CourseMaterial?>(null) }
    var materialToEdit by remember { mutableStateOf<CourseMaterial?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }



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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    when (material.type) {
                                        "PDF" -> {
                                            Button(onClick = {
                                                openInAppOrBrowser(context, material.contentUrl)
                                            }) {
                                                Text("Open PDF")
                                            }
                                        }
                                        "Video" -> {
                                            Button(onClick = {
                                                openInAppOrBrowser(context, material.contentUrl)
                                            }) {
                                                Text("Open Video")
                                            }
                                        }
                                        "Slides" -> {
                                            Button(onClick = {
                                                openInAppOrBrowser(context, material.contentUrl)
                                            }) {
                                                Text("Open Slide")
                                            }
                                        }
                                        "Link" -> {
                                            Button(onClick = {
                                                openInAppOrBrowser(context, material.contentUrl)
                                            }) {
                                                Text("Open Link")
                                            }
                                        }
                                    }
                                }
                            }


                            if (isTeacher) {
                                Row {
                                    IconButton(onClick = { materialToEdit = material }) {
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


        materialToEdit?.let { material ->
            EditMaterialDialog(
                material = material,
                onDismiss = { materialToEdit = null },
                onConfirm = {
                    viewModel.updateMaterial(it)
                    materialToEdit = null
                }
            )
        }


    }
}

fun openInAppOrBrowser(context: Context, url: String?, appPackage: String? = null) {
    if (url.isNullOrBlank()) return

    if (appPackage != null) {
        val appIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage(appPackage)
        }

        try {
            context.startActivity(appIntent)
            return
        } catch (_: Exception) {
            // Fallback to browser
        }
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(browserIntent)
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMaterialDialog(
    material: CourseMaterial,
    onDismiss: () -> Unit,
    onConfirm: (CourseMaterial) -> Unit
) {
    var title by remember { mutableStateOf(material.title) }
    var description by remember { mutableStateOf(material.description) }
    var type by remember { mutableStateOf(material.type) }
    var contentUrl by remember { mutableStateOf(material.contentUrl ?: "") }

    val typeOptions = listOf("PDF", "Link", "Video", "Slides")
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current




    Box(modifier = Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Material") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedTextField(
                            value = type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Material Type") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { expanded = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            typeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        type = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contentUrl,
                        onValueChange = { contentUrl = it },
                        label = { Text("Google Drive Link or URL") },
                        placeholder = { Text("Paste the link here...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (type != "Link") {
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://drive.google.com/drive/my-drive")
                                setPackage("com.google.android.apps.docs")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/my-drive"))
                                context.startActivity(fallbackIntent)
                            }
                        }) {
                            Text("Browse Google Drive")
                        }
                    }

                    if (contentUrl.isNotBlank() && !MaterialValidator.isValidContent(type, contentUrl)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invalid content format for selected type or invalid link.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (MaterialValidator.isValidContent(type, contentUrl)) {
                            onConfirm(material.copy(title = title, description = description, type = type, contentUrl = contentUrl))
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Invalid format or unsupported link type.")
                            }
                        }
                    },
                    enabled = title.isNotBlank() && description.isNotBlank() && contentUrl.isNotBlank() && MaterialValidator.isValidContent(type, contentUrl
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}




