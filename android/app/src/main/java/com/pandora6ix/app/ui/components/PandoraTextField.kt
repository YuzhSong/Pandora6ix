package com.pandora6ix.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/** 输入框配色：Surface 底、InputBorder 未聚焦边框、Primary 聚焦、Error 错误（不只依赖低对比卡片边框） */
@Composable
fun pandoraTextFieldColors(): TextFieldColors {
    val c = PandoraTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        disabledTextColor = c.textDisabled,
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface,
        disabledContainerColor = c.surface,
        errorContainerColor = c.surface,
        cursorColor = c.primary,
        focusedBorderColor = c.focus,
        unfocusedBorderColor = c.inputBorder,
        disabledBorderColor = c.divider,
        errorBorderColor = c.error,
        focusedLabelColor = c.primary,
        unfocusedLabelColor = c.textSecondary,
        disabledLabelColor = c.textDisabled,
        errorLabelColor = c.error,
        focusedPlaceholderColor = c.textTertiary,
        unfocusedPlaceholderColor = c.textTertiary,
        focusedSupportingTextColor = c.textSecondary,
        unfocusedSupportingTextColor = c.textSecondary,
        errorSupportingTextColor = c.error,
        focusedTrailingIconColor = c.textSecondary,
        unfocusedTrailingIconColor = c.textSecondary,
        focusedLeadingIconColor = c.textSecondary,
        unfocusedLeadingIconColor = c.textSecondary
    )
}

/**
 * 统一输入框（规范第 8 节）：单行最小 56dp、12dp 角、BodyLarge 正文；参数与 OutlinedTextField 一致。
 */
@Composable
fun PandoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = PandoraTypography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = PandoraShapes.medium,
    colors: TextFieldColors = pandoraTextFieldColors()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = PandoraDimensions.textFieldMinHeight),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}
