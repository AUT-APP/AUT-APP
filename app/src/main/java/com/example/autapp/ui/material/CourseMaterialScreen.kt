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


@Composable
fun CourseMaterialScreen(
    courseId: Int,
    viewModel: CourseMaterialViewModel,
    paddingValues: PaddingValues
) {
    val materialList by viewModel.materials.collectAsState()

    val context = LocalContext.current


    LaunchedEffect(courseId) {
        viewModel.loadMaterialsForCourse(courseId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Course Materials",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(materialList) { material ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Type: ${material.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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

                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.contentUrl))
                                    context.startActivity(intent)

                                    }
                            )
                        }
                    }
                }
            }
        }

        if (materialList.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No materials available for this course.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}