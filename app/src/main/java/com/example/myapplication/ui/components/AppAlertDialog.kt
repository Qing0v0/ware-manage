package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography

data class AppDialogOption(
    val label: String,
    val onClick: () -> Unit
)

data class AppDialogSpec(
    val title: String,
    val message: String? = null,
    val options: List<AppDialogOption> = emptyList(),
    val confirmLabel: String? = null,
    val onConfirm: (() -> Unit)? = null,
    val dismissLabel: String? = null,
    val progress: Float? = null,
    val progressMessage: String? = null,
    val dismissible: Boolean = true
)

@Composable
fun AppAlertDialog(
    dialog: AppDialogSpec?,
    onDismissRequest: () -> Unit
) {
    if (dialog == null) return

    Dialog(
        onDismissRequest = {
            if (dialog.dismissible) onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = dialog.dismissible,
            dismissOnClickOutside = dialog.dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AppColors.White)
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Text(
                text = dialog.title,
                color = AppColors.Ink19251F,
                style = AppTypography.Title2
            )

            dialog.message?.takeIf(String::isNotBlank)?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 10.dp),
                    color = AppColors.Green66736A,
                    style = AppTypography.BodyLarge
                )
            }

            dialog.options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.GrayF6F8F6)
                        .clickable {
                            onDismissRequest()
                            option.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                ) {
                    Text(
                        text = option.label,
                        color = AppColors.Green36443B,
                        style = AppTypography.BodyEmphasis
                    )
                }
            }

            if (dialog.progressMessage != null) {
                val progressModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                if (dialog.progress != null) {
                    LinearProgressIndicator(
                        progress = dialog.progress.coerceIn(0f, 1f),
                        modifier = progressModifier,
                        color = AppColors.Green246B50,
                        trackColor = AppColors.GreenE8F2EC
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = progressModifier,
                        color = AppColors.Green246B50,
                        trackColor = AppColors.GreenE8F2EC
                    )
                }
                Text(
                    text = dialog.progressMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = AppColors.Gray87918B,
                    style = AppTypography.Body,
                    textAlign = TextAlign.End
                )
            }

            if (dialog.confirmLabel != null || dialog.dismissLabel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dialog.dismissLabel?.let { label ->
                        TextButton(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppColors.Green66736A
                            )
                        ) {
                            Text(text = label, style = AppTypography.BodyEmphasis)
                        }
                    }
                    dialog.confirmLabel?.let { label ->
                        Button(
                            onClick = {
                                onDismissRequest()
                                dialog.onConfirm?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Green246B50,
                                contentColor = AppColors.White
                            )
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.BodyEmphasis.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
