package com.kotlincalendar.client.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FormMaxWidth = 440.dp

/**
 * 로그인·회원가입 같은 단일 폼 화면의 반응형 컨테이너.
 *
 * - 폼 너비는 [FormMaxWidth]로 제한하고 가운데 정렬한다 (넓은 웹·데스크톱 창에서 입력란이 늘어나지 않게).
 * - Compact 너비(모바일)에서는 화면 전체를 쓰고, 그보다 넓으면 카드로 감싼다.
 * - 높이가 부족하면(모바일 가로, 키보드 표시) 세로 스크롤된다.
 */
@Composable
fun ResponsiveFormContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().imePadding(),
    ) {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        val outerPadding = if (widthClass == WindowWidthClass.Compact) 16.dp else 32.dp

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(outerPadding),
            contentAlignment = Alignment.Center,
        ) {
            val formModifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth()
            if (widthClass == WindowWidthClass.Compact) {
                FormColumn(modifier = formModifier.padding(8.dp), content = content)
            } else {
                Card(
                    modifier = formModifier,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    FormColumn(modifier = Modifier.padding(32.dp), content = content)
                }
            }
        }
    }
}

@Composable
private fun FormColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
