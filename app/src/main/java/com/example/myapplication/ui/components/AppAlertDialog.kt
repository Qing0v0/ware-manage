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
import com.example.myapplication.ui.theme.AppDimensions
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
                .widthIn(max = AppDimensions.Size.Dp560)
                .clip(AppDimensions.Corner.large)
                .background(AppColors.White)
                .padding(start = AppDimensions.Padding.Dp20, top = AppDimensions.Padding.Dp20, end = AppDimensions.Padding.Dp20, bottom = AppDimensions.Padding.Dp12)
        ) {
            Text(
                text = dialog.title,
                color = AppColors.Gray900,
                style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold)
            )

            dialog.message?.takeIf(String::isNotBlank)?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = AppDimensions.Padding.Dp12),
                    color = AppColors.Gray500,
                    style = AppTypography.Size12
                )
            }

            dialog.options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimensions.Padding.Dp12)
                        .clip(AppDimensions.Corner.small)
                        .background(AppColors.Gray50)
                        .clickable {
                            onDismissRequest()
                            option.onClick()
                        }
                        .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp16)
                ) {
                    Text(
                        text = option.label,
                        color = AppColors.Gray800,
                        style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (dialog.progressMessage != null) {
                val progressModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimensions.Padding.Dp20)
                if (dialog.progress != null) {
                    LinearProgressIndicator(
                        progress = dialog.progress.coerceIn(0f, 1f),
                        modifier = progressModifier,
                        color = AppColors.Green800,
                        trackColor = AppColors.Green50
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = progressModifier,
                        color = AppColors.Green800,
                        trackColor = AppColors.Green50
                    )
                }
                Text(
                    text = dialog.progressMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimensions.Padding.Dp8),
                    color = AppColors.Gray500,
                    style = AppTypography.Size12,
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
                                contentColor = AppColors.Gray500
                            )
                        ) {
                            Text(text = label, style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    dialog.confirmLabel?.let { label ->
                        Button(
                            onClick = {
                                onDismissRequest()
                                dialog.onConfirm?.invoke()
                            },
                            shape = AppDimensions.Corner.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Green800,
                                contentColor = AppColors.White
                            )
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.Size12.copy(
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
