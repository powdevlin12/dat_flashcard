package com.dttrn.datfs.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Onboarding 3 bước — chỉ hiển thị lần đầu sử dụng.
 * 1. Welcome + giải thích SM-2
 * 2. Hướng dẫn tạo deck
 * 3. Hướng dẫn import Excel
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Psychology,
            title = "Chào mừng đến FlashMind!",
            subtitle = "Học thông minh, nhớ lâu hơn",
            description = "FlashMind sử dụng thuật toán SM-2 (Spaced Repetition) — " +
                "hệ thống lặp ngắt quãng khoa học giúp bạn ghi nhớ hiệu quả hơn 40% " +
                "so với ôn tập ngẫu nhiên. Thẻ bạn nhớ tốt sẽ xuất hiện ít hơn, " +
                "thẻ khó sẽ được ôn thường xuyên hơn.",
            gradient = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
            ),
        ),
        OnboardingPage(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Tạo bộ thẻ đầu tiên",
            subtitle = "Bắt đầu chỉ trong 30 giây",
            description = "Nhấn nút \"+\" trên trang chủ để tạo bộ thẻ mới. " +
                "Đặt tên, chọn màu sắc, sau đó thêm các thẻ với mặt trước (câu hỏi) " +
                "và mặt sau (câu trả lời). Bạn có thể thêm phiên âm, ví dụ, và ghi chú cho mỗi thẻ.",
            gradient = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
            ),
        ),
        OnboardingPage(
            icon = Icons.Default.FileUpload,
            title = "Import từ Excel",
            subtitle = "Sử dụng file .xlsx có sẵn",
            description = "Bạn đã có danh sách từ vựng trong Excel? Vào Cài đặt → Import/Export " +
                "để nhập file .xlsx. FlashMind hỗ trợ import hàng loạt với preview và kiểm tra lỗi tự động. " +
                "Cột A = Mặt trước, Cột B = Mặt sau. Đơn giản vậy thôi!",
            gradient = listOf(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary,
            ),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinish) {
                    Text("Bỏ qua", fontWeight = FontWeight.SemiBold)
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Indicators
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .animateContentSize(tween(300)),
                    )
                }
            }

            // Bottom button
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = if (pagerState.currentPage < 2) "Tiếp tục" else "Bắt đầu học!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

// ===== Data Class =====

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val gradient: List<androidx.compose.ui.graphics.Color>,
)

// ===== Page Content =====

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon with gradient background
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = page.gradient.first().copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = page.gradient.first().copy(alpha = 0.2f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = page.gradient.first(),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )
    }
}
