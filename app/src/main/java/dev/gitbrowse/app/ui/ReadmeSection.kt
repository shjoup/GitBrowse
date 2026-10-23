package dev.gitbrowse.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gitbrowse.app.RepositoryUiState

@Composable
fun ReadmeSection(
    state: RepositoryUiState,
    onRetry: () -> Unit
) {
    var expanded by rememberSaveable(state.fullName) { mutableStateOf(false) }
    val readme = state.readme
    val error = state.readmeError

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("README", style = MaterialTheme.typography.titleLarge)

        when {
            state.isLoadingReadme -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading README...")
            }
            error != null -> {
                Text(error)
                OutlinedButton(
                    onClick = onRetry,
                    shape = RectangleShape
                ) {
                    Text("Retry README")
                }
            }
            readme.isNullOrBlank() -> Text("No README available.")
            else -> {
                val previewLimit = if (expanded) 30_000 else 1_800

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape
                ) {
                    SelectionContainer {
                        Text(
                            text = readme.take(previewLimit),
                            modifier = Modifier.padding(16.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                if (readme.length > 1_800) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        shape = RectangleShape
                    ) {
                        Text(if (expanded) "Show less" else "Show more")
                    }
                }

                if (expanded && readme.length > 30_000) {
                    Text(
                        text = "Preview limited to 30,000 characters.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
