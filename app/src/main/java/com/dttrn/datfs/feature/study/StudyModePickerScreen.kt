package com.dttrn.datfs.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dttrn.datfs.core.data.local.entity.StudyMode

/**
 * Màn hình chọn chế độ học trước khi bắt đầu session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModePickerScreen(
    deckTitle: String,
    onBack: () -> Unit,
    onSelectMode: (StudyMode) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chọn chế độ học", fontWeight = FontWeight.Bold)
                        Text(
                            deckTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Lựa chọn cách học phù hợp với bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            studyModes.forEach { modeInfo ->
                StudyModeCard(
                    info = modeInfo,
                    onClick = { onSelectMode(modeInfo.mode) },
                )
            }
        }
    }
}

data class StudyModeInfo(
    val mode: StudyMode,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val badge: String? = null,
)

private val studyModes = listOf(
    StudyModeInfo(
        mode = StudyMode.SPACED_REPETITION,
        title = "Lặp lại ngắt quãng",
        subtitle = "Thuật toán SM-2",
        description = "Ôn tập thông minh — thẻ khó xuất hiện thường xuyên hơn. Hiệu quả nhất để ghi nhớ lâu dài.",
        icon = Icons.Default.Psychology,
        gradientColors = listOf(Color(0xFF4A90E2), Color(0xFF7B61FF)),
        badge = "Khuyến nghị",
    ),
    StudyModeInfo(
        mode = StudyMode.LEARN,
        title = "Học tuần tự",
        subtitle = "Flash Cards",
        description = "Lật thẻ xem mặt trước/sau theo thứ tự. Phù hợp khi học lần đầu.",
        icon = Icons.Default.Style,
        gradientColors = listOf(Color(0xFF00C853), Color(0xFF00BCD4)),
    ),
    StudyModeInfo(
        mode = StudyMode.QUIZ,
        title = "Trắc nghiệm",
        subtitle = "4 lựa chọn",
        description = "Chọn đáp án đúng từ 4 lựa chọn. Kiểm tra kiến thức hiệu quả.",
        icon = Icons.Default.Quiz,
        gradientColors = listOf(Color(0xFFFF6D00), Color(0xFFFF6B6B)),
    ),
    StudyModeInfo(
        mode = StudyMode.WRITE,
        title = "Viết đáp án",
        subtitle = "Tự điền",
        description = "Tự gõ câu trả lời không cần nhìn gợi ý. Luyện tập chủ động và kiểm tra sâu nhất.",
        icon = Icons.Default.Edit,
        gradientColors = listOf(Color(0xFF9C27B0), Color(0xFFE91E63)),
    ),
    StudyModeInfo(
        mode = StudyMode.MATCH,
        title = "Nối từ",
        subtitle = "Ghép đôi",
        description = "Nối mặt trước với mặt sau của thẻ. Vui và nhanh chóng để ôn tập.",
        icon = Icons.Default.GridView,
        gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),
    ),
    StudyModeInfo(
        mode = StudyMode.DICTATION,
        title = "Nghe chính tả",
        subtitle = "Luyện nghe & viết",
        description = "TTS đọc từ hoặc cụm từ, bạn gõ lại những gì nghe được. Luyện kỹ năng nghe và chính tả.",
        icon = Icons.Default.HeadsetMic,
        gradientColors = listOf(Color(0xFF009688), Color(0xFF00BCD4)),
    ),
    StudyModeInfo(
        mode = StudyMode.EXAMINATION,
        title = "Kiểm tra",
        subtitle = "Đánh giá kiến thức",
        description = "Làm bài kiểm tra đánh giá kiến thức với nhiều dạng câu hỏi, giới hạn thời gian và chấm điểm.",
        icon = Icons.Default.Assignment,
        gradientColors = listOf(Color(0xFF1565C0), Color(0xFF1E88E5)),
        badge = "Mới",
    ),
)

@Composable
private fun StudyModeCard(
    info: StudyModeInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(info.gradientColors)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        info.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    info.badge?.let {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    info.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
