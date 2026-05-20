# TÀI LIỆU YÊU CẦU SẢN PHẨM (PRD)

# Ứng Dụng Học Flashcard Android — Offline-First

**Phiên bản:** 1.0.0
**Ngày:** 2025
**Tác giả:** Senior Product Manager + Senior Android Architect
**Trạng thái:** Sẵn sàng phát triển (Implementation-Ready)

---

## MỤC LỤC

1. [Tóm Tắt Điều Hành](#1-tóm-tắt-điều-hành)
2. [Mục Tiêu Sản Phẩm](#2-mục-tiêu-sản-phẩm)
3. [Chân Dung Người Dùng](#3-chân-dung-người-dùng)
4. [Yêu Cầu Chức Năng](#4-yêu-cầu-chức-năng)
5. [Yêu Cầu Phi Chức Năng](#5-yêu-cầu-phi-chức-năng)
6. [Kiến Trúc Kỹ Thuật](#6-kiến-trúc-kỹ-thuật)
7. [Schema Cơ Sở Dữ Liệu](#7-schema-cơ-sở-dữ-liệu)
8. [Luồng Điều Hướng Ứng Dụng](#8-luồng-điều-hướng-ứng-dụng)
9. [Đặc Tả UI/UX](#9-đặc-tả-uiux)
10. [Lộ Trình Phát Triển](#10-lộ-trình-phát-triển)
11. [Rủi Ro & Ràng Buộc](#11-rủi-ro--ràng-buộc)
12. [Phạm Vi MVP](#12-phạm-vi-mvp)
13. [Cải Tiến Tương Lai](#13-cải-tiến-tương-lai)

---

## 1. TÓM TẮT ĐIỀU HÀNH

### 1.1 Giới Thiệu Sản Phẩm

**FlashMind** là ứng dụng học flashcard Android native, hoàn toàn offline, lấy cảm hứng từ Quizlet nhưng được tối ưu hoá cho hiệu năng Android thuần và trải nghiệm người dùng mượt mà. Toàn bộ dữ liệu được lưu trữ cục bộ thông qua Room Database, không yêu cầu đăng nhập hay kết nối internet.

### 1.2 Vấn Đề Cần Giải Quyết

- Quizlet và các ứng dụng flashcard phổ biến yêu cầu tài khoản và kết nối internet liên tục.
- Người dùng bị ràng buộc vào hệ sinh thái cloud, lo ngại về quyền riêng tư và mất dữ liệu.
- Không có giải pháp native Android chất lượng cao, offline-first, hỗ trợ import/export Excel và thuật toán lặp ngắt quãng (spaced repetition) thực sự.

### 1.3 Giải Pháp

Ứng dụng Android native được xây dựng với Kotlin + Jetpack Compose, triển khai đầy đủ tính năng quản lý bộ thẻ, 5+ chế độ học, thuật toán SM-2, thống kê chi tiết và import/export Excel via Apache POI — tất cả hoạt động hoàn toàn offline.

### 1.4 Stack Công Nghệ

| Thành phần    | Công nghệ                    |
| ------------- | ---------------------------- |
| Ngôn ngữ      | Kotlin                       |
| UI            | Jetpack Compose + Material 3 |
| Cơ sở dữ liệu | Room Database                |
| Kiến trúc     | MVVM + Clean Architecture    |
| Async         | Coroutines + Flow            |
| DI            | Hilt                         |
| Điều hướng    | Navigation Compose           |
| Cài đặt       | DataStore Preferences        |
| Import/Export | Apache POI                   |
| Min SDK       | API 26 (Android 8.0)         |
| Target SDK    | API 36                       |

---

## 2. MỤC TIÊU SẢN PHẨM

### 2.1 Mục Tiêu Kinh Doanh

- Đạt 4.5+ sao trên Google Play Store trong 6 tháng đầu.
- Xây dựng cộng đồng người dùng tin tưởng vào quyền riêng tư và sở hữu dữ liệu.
- Tiếp cận phân khúc người dùng tại các vùng có kết nối internet kém hoặc không ổn định.

### 2.2 Mục Tiêu Sản Phẩm

- **Offline-first:** 100% tính năng hoạt động không cần internet.
- **Hiệu năng:** Thời gian khởi động < 1 giây, chuyển màn hình < 200ms.
- **Dễ dùng:** Người mới có thể tạo bộ thẻ đầu tiên trong < 2 phút.
- **Smart Learning:** Thuật toán SM-2 giúp người dùng ghi nhớ hiệu quả hơn 40% so với ôn tập ngẫu nhiên.
- **Khả năng mở rộng:** Hỗ trợ tối thiểu 10,000 thẻ, 500 bộ thẻ mà không giảm hiệu năng.

### 2.3 KPI Thành Công

| Chỉ số                   | Mục tiêu                               |
| ------------------------ | -------------------------------------- |
| Daily Active Users (DAU) | Retention 40% sau 30 ngày              |
| Session Length           | > 5 phút/phiên                         |
| Study Streak             | 30% người dùng duy trì streak > 7 ngày |
| Import/Export Usage      | 20% người dùng sử dụng tính năng Excel |
| Crash Rate               | < 0.1%                                 |

---

## 3. CHÂN DUNG NGƯỜI DÙNG

### Persona 1: Sinh Viên Đại Học — "Minh"

- **Tuổi:** 20
- **Hoàn cảnh:** Sinh viên năm 3 ngành Công nghệ thông tin, chuẩn bị thi cuối kỳ
- **Mục tiêu:** Ôn nhanh 200+ thuật ngữ kỹ thuật trong 2 tuần
- **Điểm đau:** Quizlet yêu cầu trả phí, ứng dụng offline hiện tại quá xấu
- **Thiết bị:** Samsung Galaxy A54, Android 13
- **Kỳ vọng:** Import file Excel ghi chú có sẵn, quiz nhiều lựa chọn, thống kê tiến độ

### Persona 2: Người Học Ngoại Ngữ — "Lan"

- **Tuổi:** 28
- **Hoàn cảnh:** Nhân viên văn phòng, học tiếng Nhật buổi tối
- **Mục tiêu:** Học 2000 từ vựng Kanji trong 6 tháng
- **Điểm đau:** Cần học từng chút một mỗi ngày, lo quên từ cũ khi học từ mới
- **Thiết bị:** Pixel 7, Android 14
- **Kỳ vọng:** Spaced repetition thông minh, hỗ trợ phiên âm, ôn tập nhắc nhở hàng ngày

### Persona 3: Chuyên Gia Luyện Thi — "Tuấn"

- **Tuổi:** 35
- **Hoàn cảnh:** Bác sĩ ôn thi chuyên khoa, lưu lượng thông tin rất lớn
- **Mục tiêu:** Quản lý 50+ bộ thẻ theo chuyên đề, chia sẻ với đồng nghiệp qua file
- **Điểm đau:** Không có ứng dụng nào cho phép xuất toàn bộ dữ liệu ra Excel để backup
- **Thiết bị:** Máy tính bảng Android 12.4"
- **Kỳ vọng:** Export/import Excel toàn bộ, phân loại bộ thẻ, thống kê hiệu suất học tập

### Persona 4: Học Sinh THPT — "An"

- **Tuổi:** 16
- **Hoàn cảnh:** Ôn thi THPTQG, học ở vùng có internet kém
- **Mục tiêu:** Học từ vựng tiếng Anh và các công thức Toán, Hóa
- **Điểm đau:** Ứng dụng online không dùng được khi mất mạng
- **Thiết bị:** Redmi Note 11, Android 11, RAM 4GB
- **Kỳ vọng:** Hoạt động hoàn toàn offline, nhẹ, không quảng cáo

---

## 4. YÊU CẦU CHỨC NĂNG

### 4.1 Quản Lý Bộ Thẻ (Deck Management)

#### Tính Năng Bắt Buộc

| Mã    | Tính năng          | Mô tả chi tiết                                       |
| ----- | ------------------ | ---------------------------------------------------- |
| DM-01 | Tạo bộ thẻ         | Form tạo deck với tên, mô tả, danh mục, tag, màu sắc |
| DM-02 | Chỉnh sửa bộ thẻ   | Cập nhật toàn bộ metadata của deck                   |
| DM-03 | Xóa bộ thẻ         | Xóa mềm (soft delete) với xác nhận, cascade xóa thẻ  |
| DM-04 | Nhân bản bộ thẻ    | Tạo bản sao hoàn chỉnh với tên mới                   |
| DM-05 | Lưu trữ bộ thẻ     | Ẩn deck khỏi màn hình chính, có thể khôi phục        |
| DM-06 | Tìm kiếm bộ thẻ    | Tìm theo tên, mô tả, tag với debounce 300ms          |
| DM-07 | Sắp xếp bộ thẻ     | Theo tên, ngày tạo, ngày cập nhật, tiến độ học       |
| DM-08 | Lọc bộ thẻ         | Lọc theo danh mục, tag, trạng thái yêu thích         |
| DM-09 | Đánh dấu yêu thích | Toggle favorite, hiển thị ưu tiên trên home          |

#### Trường Dữ Liệu Deck

```
id: UUID (Primary Key)
title: String (max 100 ký tự, bắt buộc)
description: String? (max 500 ký tự)
category: String? (danh mục tùy chỉnh)
tags: List<String> (tối đa 10 tag)
colorHex: String (mã màu HEX, mặc định #4A90E2)
isFavorite: Boolean (default: false)
isArchived: Boolean (default: false)
cardCount: Int (computed từ cards table)
studyProgress: Float (0.0 - 1.0, % thẻ đã thuộc)
createdAt: Long (timestamp milliseconds)
updatedAt: Long (timestamp milliseconds)
```

---

### 4.2 Quản Lý Thẻ (Flashcard Management)

#### Tính Năng Bắt Buộc

| Mã    | Tính năng           | Mô tả chi tiết                                   |
| ----- | ------------------- | ------------------------------------------------ |
| FM-01 | Thêm thẻ            | Form thêm thẻ đơn lẻ với đầy đủ trường           |
| FM-02 | Chỉnh sửa thẻ       | Cập nhật nội dung, hình ảnh, metadata            |
| FM-03 | Xóa thẻ             | Xóa đơn hoặc theo batch với xác nhận             |
| FM-04 | Chỉnh sửa hàng loạt | Chọn nhiều thẻ, xóa/di chuyển/gắn tag cùng lúc   |
| FM-05 | Nhân bản thẻ        | Sao chép thẻ trong cùng deck hoặc sang deck khác |
| FM-06 | Sắp xếp thẻ         | Kéo thả để thay đổi thứ tự hiển thị              |
| FM-07 | Tìm kiếm thẻ        | Tìm trong frontText, backText, note, example     |
| FM-08 | Thêm hình ảnh       | Chọn từ gallery hoặc chụp camera, lưu local      |

#### Trường Dữ Liệu Flashcard

```
id: UUID (Primary Key)
deckId: UUID (Foreign Key → Deck.id)
frontText: String (bắt buộc, max 500 ký tự)
backText: String (bắt buộc, max 500 ký tự)
imagePath: String? (đường dẫn file local)
pronunciation: String? (phiên âm, max 200 ký tự)
exampleSentence: String? (câu ví dụ, max 1000 ký tự)
note: String? (ghi chú cá nhân, max 500 ký tự)
difficultyLevel: Int (1=Dễ, 2=Trung bình, 3=Khó)
orderIndex: Int (thứ tự hiển thị trong deck)
createdAt: Long
updatedAt: Long
-- Review Metadata (SM-2) --
easeFactor: Float (2.5 mặc định, min 1.3)
intervalDays: Int (0 = chưa học)
repetitionCount: Int (số lần đã ôn)
dueDate: Long? (timestamp ngày cần ôn tiếp)
failureStreak: Int (số lần sai liên tiếp)
lastReviewedAt: Long?
isKnown: Boolean (người dùng đánh dấu đã thuộc)
```

---

### 4.3 Chế Độ Học (Study Modes)

#### SM-01: Flashcard Swipe Mode

- Hiển thị mặt trước, tap để lật xem mặt sau
- Swipe phải = Biết, Swipe trái = Không biết
- Animation lật thẻ 3D mượt mà (180° trên trục Y)
- Hiển thị tiến độ (X/Total), thanh progress
- Nút undo cho lần swipe trước đó
- Tóm tắt kết quả cuối phiên học

#### SM-02: Learn Mode (Chế độ học thích nghi)

- Hệ thống tự chọn câu hỏi dựa trên lịch sử lỗi
- Hiển thị thẻ sai nhiều lần trước rồi mới tiến
- Thẻ trả lời sai được đưa trở lại queue
- Phiên học kết thúc khi tất cả thẻ được trả lời đúng ít nhất 1 lần
- Hỗ trợ "hint" (gợi ý) với 1 chữ cái đầu

#### SM-03: Write Mode (Gõ câu trả lời)

- Hiển thị mặt trước, người dùng gõ câu trả lời
- So sánh thông minh: bỏ qua hoa/thường, khoảng trắng thừa, dấu câu tùy chọn
- Highlight ký tự đúng/sai trực quan
- Điểm số mỗi câu: Đúng hoàn toàn / Gần đúng / Sai

#### SM-04: Multiple Choice Quiz Mode

- 4 lựa chọn cho mỗi câu hỏi
- Distractors (đáp án nhiễu) được auto-generate từ các thẻ trong cùng deck
- Thời gian đếm ngược tùy chọn (15/30/60 giây)
- Hiển thị đáp án đúng ngay sau khi chọn sai
- Điểm số và rank cuối phiên

#### SM-05: Match Mode (Kéo thả ghép đôi)

- Grid 4x2 hoặc 3x2 hiển thị từ ngữ trộn lẫn
- Kéo thả hoặc tap-to-match
- Đồng hồ bấm giờ (timed game)
- Combo multiplier khi ghép liên tiếp đúng
- High score được lưu theo deck

#### SM-06: Review Incorrect Answers Mode

- Tự động tạo queue từ các thẻ đã sai trong phiên trước
- Sử dụng lại cơ chế của Learn Mode
- Filter được: "Sai hôm nay", "Sai tuần này", "Sai nhiều nhất"

---

### 4.4 Hệ Thống Lặp Ngắt Quãng (Spaced Repetition - SM-2)

#### Thuật Toán SM-2

```kotlin
/**
 * SM-2 Algorithm Implementation
 * quality: 0-5 (0-1=sai, 2=sai gần đúng, 3=đúng khó, 4=đúng, 5=đúng dễ)
 */
fun calculateNextReview(card: Flashcard, quality: Int): ReviewResult {
    var easeFactor = card.easeFactor
    var interval = card.intervalDays
    var repetition = card.repetitionCount

    if (quality >= 3) {
        interval = when (repetition) {
            0 -> 1
            1 -> 6
            else -> (interval * easeFactor).roundToInt()
        }
        repetition++
    } else {
        repetition = 0
        interval = 1
    }

    easeFactor = maxOf(1.3f, easeFactor + 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))

    val dueDate = System.currentTimeMillis() + interval * 24 * 60 * 60 * 1000L
    return ReviewResult(easeFactor, interval, repetition, dueDate)
}
```

#### Queue Tự Động

| Queue            | Điều kiện                                        |
| ---------------- | ------------------------------------------------ |
| Today's Review   | `dueDate <= endOfToday AND isKnown == false`     |
| Upcoming Reviews | `dueDate > endOfToday AND dueDate <= 7 ngày tới` |
| Overdue Reviews  | `dueDate < startOfToday AND isKnown == false`    |
| New Cards        | `repetitionCount == 0`                           |

---

### 4.5 Dashboard Thống Kê

| Chỉ số           | Mô tả                              | Loại biểu đồ         |
| ---------------- | ---------------------------------- | -------------------- |
| Study Streak     | Số ngày liên tiếp học ≥ 1 thẻ      | Badge + số ngày      |
| Total Study Time | Tổng thời gian các phiên           | Line chart theo tuần |
| Cards Mastered   | Tổng thẻ có `isKnown == true`      | Progress ring        |
| Cards Due Today  | Số thẻ cần ôn hôm nay              | Badge đỏ             |
| Accuracy Rate    | % trả lời đúng trong 30 ngày       | Percentage card      |
| Daily Progress   | Số thẻ học theo ngày               | Bar chart            |
| Weekly Heatmap   | Mật độ học theo ngày trong 12 tuần | Calendar heatmap     |
| Deck Performance | Tiến độ % từng deck                | Horizontal bar list  |

---

### 4.6 Import / Export Excel

#### Import (.xlsx)

**Cấu trúc cột bắt buộc:**

| Cột | Tên tiêu đề   | Bắt buộc | Kiểu dữ liệu    |
| --- | ------------- | -------- | --------------- |
| A   | Front         | Có       | String          |
| B   | Back          | Có       | String          |
| C   | Pronunciation | Không    | String          |
| D   | Example       | Không    | String          |
| E   | Note          | Không    | String          |
| F   | Difficulty    | Không    | Integer (1/2/3) |

**Quy trình Import:**

1. Người dùng chọn file .xlsx từ bộ nhớ máy
2. App đọc file, validate cấu trúc
3. Hiển thị preview bảng (tối đa 20 dòng đầu)
4. Hiển thị báo cáo lỗi theo dòng (nếu có)
5. Người dùng xác nhận import → tạo deck mới hoặc thêm vào deck có sẵn
6. Hiển thị kết quả: X thẻ thành công, Y thẻ bỏ qua, Z lỗi

**Validation Rules:**

- `Front` và `Back` không được trống
- `Front` hoặc `Back` không vượt quá 500 ký tự
- `Difficulty` phải là 1, 2 hoặc 3 nếu có
- Cảnh báo (không block) nếu trùng lặp front text

#### Export (.xlsx)

- Export deck được chọn hoặc tất cả deck
- Mỗi deck = 1 sheet riêng trong file
- Sheet "Statistics" tổng hợp thống kê
- Tên file: `FlashMind_Export_YYYY-MM-DD.xlsx`
- Bao gồm metadata: ngày xuất, tổng số thẻ, tiến độ

#### Template Mẫu

- Nút "Tải template mẫu" tạo file `.xlsx` trống với tiêu đề cột và 2-3 dòng ví dụ

---

### 4.7 Sao Lưu & Khôi Phục

| Loại         | Định dạng | Mô tả                                                   |
| ------------ | --------- | ------------------------------------------------------- |
| Full Backup  | JSON      | Toàn bộ dữ liệu: decks, cards, review history, settings |
| DB Backup    | .db file  | Copy trực tiếp file Room database                       |
| Excel Backup | .xlsx     | Tất cả deck dưới dạng Excel                             |

**Khôi Phục:**

- Validate file trước khi import (kiểm tra version schema)
- Preview số lượng deck/thẻ sẽ được restore
- Tùy chọn: Ghi đè toàn bộ hoặc Merge (thêm vào dữ liệu hiện có)
- Rollback nếu restore thất bại

---

### 4.8 Tìm Kiếm Toàn Cục

- Tìm kiếm đồng thời trên: tên deck, frontText, backText, tag, note, exampleSentence
- Fuzzy search: bỏ qua hoa/thường, dấu câu, tìm gần đúng
- Debounce 300ms, tối thiểu 2 ký tự
- Kết quả nhóm theo: Deck, Thẻ
- Highlight từ khóa trong kết quả
- Lịch sử tìm kiếm (lưu 10 từ gần nhất)

---

### 4.9 Cài Đặt

| Mục                 | Tùy chọn                             |
| ------------------- | ------------------------------------ |
| Giao diện           | Sáng / Tối / Theo hệ thống           |
| Cỡ chữ              | Nhỏ / Vừa / Lớn                      |
| Animation thẻ       | Bật / Tắt                            |
| Nhắc nhở ôn tập     | Bật/Tắt + Chọn giờ                   |
| Tùy chọn export     | Định dạng mặc định (JSON/Excel)      |
| Chế độ học mặc định | Swipe / Learn / Quiz / Match / Write |
| Hệ thống nhắc nhở   | Bật/Tắt                              |

---

### 4.10 Thông Báo Local

| Loại             | Kích hoạt                   | Nội dung                                      |
| ---------------- | --------------------------- | --------------------------------------------- |
| Daily Review     | Hàng ngày theo giờ cài đặt  | "Bạn có X thẻ cần ôn hôm nay!"                |
| Overdue Reminder | Khi có thẻ quá hạn > 2 ngày | "X thẻ đang chờ! Đừng để quên nhé."           |
| Streak Reminder  | Khi sắp mất streak (23:00)  | "Học ít nhất 1 thẻ để duy trì streak N ngày!" |

---

## 5. YÊU CẦU PHI CHỨC NĂNG

### 5.1 Hiệu Năng

| Chỉ số                   | Mục tiêu                                   |
| ------------------------ | ------------------------------------------ |
| App startup (cold)       | < 1.5 giây                                 |
| App startup (warm)       | < 500ms                                    |
| Screen transition        | < 200ms                                    |
| Database query (100 thẻ) | < 50ms                                     |
| Excel import (500 dòng)  | < 3 giây                                   |
| Excel export (1000 thẻ)  | < 5 giây                                   |
| Memory usage (idle)      | < 80MB                                     |
| Frame rate               | 60fps ổn định, 120fps trên thiết bị hỗ trợ |

### 5.2 Độ Tin Cậy

- Crash rate < 0.1%
- Data integrity: Sử dụng Room transactions cho mọi batch operation
- Không mất dữ liệu khi app bị kill đột ngột
- Auto-save khi người dùng rời màn hình chỉnh sửa

### 5.3 Khả Năng Mở Rộng

- Hỗ trợ tối thiểu 10,000 thẻ, 500 deck
- Phân trang (pagination) tất cả danh sách dài
- Lazy loading hình ảnh thẻ
- Room index tối ưu cho tìm kiếm

### 5.4 Bảo Mật & Quyền Riêng Tư

- Không thu thập dữ liệu người dùng
- Không kết nối internet
- Hình ảnh lưu trong app-specific storage (không cần READ_EXTERNAL_STORAGE trên API 29+)
- Quyền truy cập storage chỉ khi import/export

### 5.5 Khả Năng Tiếp Cận (Accessibility)

- Hỗ trợ TalkBack (contentDescription cho tất cả phần tử)
- Dynamic font size với `sp` unit
- Contrast ratio tối thiểu 4.5:1
- Touch target tối thiểu 48dp

### 5.6 Hỗ Trợ Thiết Bị

- Min SDK: API 26 (Android 8.0) — phủ 95%+ thiết bị
- Target SDK: API 35 (Android 15)
- Hỗ trợ: Phone, Tablet, Foldable (adaptive layout)
- Hướng màn hình: Portrait chính, Landscape hỗ trợ

---

## 6. KIẾN TRÚC KỸ THUẬT

### 6.1 Cấu Trúc Thư Mục

```
app/
├── src/main/java/com/flashmind/
│   ├── FlashMindApplication.kt
│   │
│   ├── core/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   │   ├── DeckDao.kt
│   │   │   │   │   ├── FlashcardDao.kt
│   │   │   │   │   ├── ReviewSessionDao.kt
│   │   │   │   │   └── StudyStatisticsDao.kt
│   │   │   │   └── entity/
│   │   │   │       ├── DeckEntity.kt
│   │   │   │       ├── FlashcardEntity.kt
│   │   │   │       ├── ReviewSessionEntity.kt
│   │   │   │       └── StudyStatisticsEntity.kt
│   │   │   └── datastore/
│   │   │       └── SettingsDataStore.kt
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Deck.kt
│   │   │   │   ├── Flashcard.kt
│   │   │   │   ├── ReviewSession.kt
│   │   │   │   └── StudyStatistics.kt
│   │   │   └── repository/
│   │   │       ├── DeckRepository.kt (interface)
│   │   │       ├── FlashcardRepository.kt (interface)
│   │   │       └── ReviewRepository.kt (interface)
│   │   │
│   │   └── ui/
│   │       ├── theme/
│   │       │   ├── Color.kt
│   │       │   ├── Theme.kt
│   │       │   └── Type.kt
│   │       └── component/
│   │           ├── FlashcardItem.kt
│   │           ├── DeckCard.kt
│   │           └── StatChart.kt
│   │
│   ├── feature/
│   │   ├── home/
│   │   │   ├── data/repository/HomeRepositoryImpl.kt
│   │   │   ├── domain/usecase/
│   │   │   │   ├── GetDecksUseCase.kt
│   │   │   │   └── GetStudySummaryUseCase.kt
│   │   │   ├── presentation/
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   └── HomeUiState.kt
│   │   │
│   │   ├── deck/
│   │   │   ├── data/repository/DeckRepositoryImpl.kt
│   │   │   ├── domain/usecase/
│   │   │   │   ├── CreateDeckUseCase.kt
│   │   │   │   ├── UpdateDeckUseCase.kt
│   │   │   │   ├── DeleteDeckUseCase.kt
│   │   │   │   ├── DuplicateDeckUseCase.kt
│   │   │   │   └── ArchiveDeckUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── DeckDetailViewModel.kt
│   │   │       ├── DeckDetailScreen.kt
│   │   │       ├── CreateEditDeckViewModel.kt
│   │   │       └── CreateEditDeckScreen.kt
│   │   │
│   │   ├── card/
│   │   │   ├── data/repository/FlashcardRepositoryImpl.kt
│   │   │   ├── domain/usecase/
│   │   │   │   ├── AddCardUseCase.kt
│   │   │   │   ├── UpdateCardUseCase.kt
│   │   │   │   ├── DeleteCardUseCase.kt
│   │   │   │   └── BulkEditCardsUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── CardEditorViewModel.kt
│   │   │       └── CardEditorScreen.kt
│   │   │
│   │   ├── study/
│   │   │   ├── domain/
│   │   │   │   ├── algorithm/
│   │   │   │   │   └── SM2Algorithm.kt
│   │   │   │   └── usecase/
│   │   │   │       ├── GetStudyQueueUseCase.kt
│   │   │   │       └── SubmitReviewUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── swipe/SwipeStudyViewModel.kt
│   │   │       ├── swipe/SwipeStudyScreen.kt
│   │   │       ├── learn/LearnModeViewModel.kt
│   │   │       ├── learn/LearnModeScreen.kt
│   │   │       ├── write/WriteModeViewModel.kt
│   │   │       ├── write/WriteModeScreen.kt
│   │   │       ├── quiz/QuizViewModel.kt
│   │   │       ├── quiz/QuizScreen.kt
│   │   │       ├── match/MatchGameViewModel.kt
│   │   │       └── match/MatchGameScreen.kt
│   │   │
│   │   ├── statistics/
│   │   │   ├── domain/usecase/GetStatisticsUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── StatisticsViewModel.kt
│   │   │       └── StatisticsScreen.kt
│   │   │
│   │   ├── importexport/
│   │   │   ├── data/
│   │   │   │   ├── parser/ExcelParser.kt
│   │   │   │   └── exporter/ExcelExporter.kt
│   │   │   ├── domain/usecase/
│   │   │   │   ├── ImportExcelUseCase.kt
│   │   │   │   └── ExportExcelUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── ImportExportViewModel.kt
│   │   │       └── ImportExportScreen.kt
│   │   │
│   │   ├── search/
│   │   │   ├── domain/usecase/SearchUseCase.kt
│   │   │   └── presentation/
│   │   │       ├── SearchViewModel.kt
│   │   │       └── SearchScreen.kt
│   │   │
│   │   ├── settings/
│   │   │   └── presentation/
│   │   │       ├── SettingsViewModel.kt
│   │   │       └── SettingsScreen.kt
│   │   │
│   │   └── backup/
│   │       ├── domain/usecase/
│   │       │   ├── BackupUseCase.kt
│   │       │   └── RestoreUseCase.kt
│   │       └── presentation/
│   │           └── BackupScreen.kt
│   │
│   ├── navigation/
│   │   ├── AppNavGraph.kt
│   │   └── Screen.kt
│   │
│   └── di/
│       ├── DatabaseModule.kt
│       ├── RepositoryModule.kt
│       └── UseCaseModule.kt
│
└── src/main/res/
    ├── values/strings.xml
    ├── values/colors.xml
    └── ...
```

### 6.2 Luồng Dữ Liệu (Data Flow)

```
UI (Compose) ←→ ViewModel ←→ UseCase ←→ Repository (interface)
                                              ↓
                                    RepositoryImpl
                                              ↓
                                    Room DAO / DataStore / FileSystem
```

### 6.3 Dependency Injection (Hilt)

```kotlin
// DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "flashmind.db")
            .build()

    @Provides fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()
    @Provides fun provideFlashcardDao(db: AppDatabase): FlashcardDao = db.flashcardDao()
}
```

### 6.4 Pattern Xử Lý Lỗi

```kotlin
// Sealed class cho kết quả
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// AppException hierarchy
sealed class AppException : Exception() {
    class DatabaseException(override val message: String?) : AppException()
    class ImportException(override val message: String?, val rowNumber: Int?) : AppException()
    class ExportException(override val message: String?) : AppException()
    class ValidationException(override val message: String?) : AppException()
    class FileNotFoundException(override val message: String?) : AppException()
}
```

### 6.5 UI State Pattern

```kotlin
// Mỗi màn hình có sealed UiState
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val decks: List<Deck>,
        val todayReviewCount: Int,
        val studyStreak: Int,
        val recentDecks: List<Deck>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

// ViewModel sử dụng StateFlow
class HomeViewModel @Inject constructor(
    private val getDecksUseCase: GetDecksUseCase,
    private val getStudySummaryUseCase: GetStudySummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getDecksUseCase(),
                getStudySummaryUseCase()
            ) { decks, summary ->
                HomeUiState.Success(
                    decks = decks,
                    todayReviewCount = summary.todayCount,
                    studyStreak = summary.streak,
                    recentDecks = decks.take(5)
                )
            }.catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "Lỗi không xác định")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
```

---

## 7. SCHEMA CƠ SỞ DỮ LIỆU

### 7.1 Entity: deck_table

```sql
CREATE TABLE deck_table (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    category TEXT,
    tags TEXT NOT NULL DEFAULT '[]',  -- JSON array
    colorHex TEXT NOT NULL DEFAULT '#4A90E2',
    isFavorite INTEGER NOT NULL DEFAULT 0,
    isArchived INTEGER NOT NULL DEFAULT 0,
    studyProgress REAL NOT NULL DEFAULT 0.0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

CREATE INDEX idx_deck_title ON deck_table(title);
CREATE INDEX idx_deck_archived ON deck_table(isArchived);
CREATE INDEX idx_deck_favorite ON deck_table(isFavorite);
```

### 7.2 Entity: flashcard_table

```sql
CREATE TABLE flashcard_table (
    id TEXT PRIMARY KEY NOT NULL,
    deckId TEXT NOT NULL,
    frontText TEXT NOT NULL,
    backText TEXT NOT NULL,
    imagePath TEXT,
    pronunciation TEXT,
    exampleSentence TEXT,
    note TEXT,
    difficultyLevel INTEGER NOT NULL DEFAULT 2,
    orderIndex INTEGER NOT NULL DEFAULT 0,
    easeFactor REAL NOT NULL DEFAULT 2.5,
    intervalDays INTEGER NOT NULL DEFAULT 0,
    repetitionCount INTEGER NOT NULL DEFAULT 0,
    dueDate INTEGER,
    failureStreak INTEGER NOT NULL DEFAULT 0,
    lastReviewedAt INTEGER,
    isKnown INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (deckId) REFERENCES deck_table(id) ON DELETE CASCADE
);

CREATE INDEX idx_card_deckId ON flashcard_table(deckId);
CREATE INDEX idx_card_dueDate ON flashcard_table(dueDate);
CREATE INDEX idx_card_isKnown ON flashcard_table(isKnown);
CREATE INDEX idx_card_front ON flashcard_table(frontText);
```

### 7.3 Entity: review_session_table

```sql
CREATE TABLE review_session_table (
    id TEXT PRIMARY KEY NOT NULL,
    deckId TEXT NOT NULL,
    studyMode TEXT NOT NULL,      -- SWIPE, LEARN, WRITE, QUIZ, MATCH
    startedAt INTEGER NOT NULL,
    endedAt INTEGER,
    totalCards INTEGER NOT NULL DEFAULT 0,
    correctCount INTEGER NOT NULL DEFAULT 0,
    incorrectCount INTEGER NOT NULL DEFAULT 0,
    durationSeconds INTEGER,
    FOREIGN KEY (deckId) REFERENCES deck_table(id) ON DELETE CASCADE
);

CREATE INDEX idx_session_deckId ON review_session_table(deckId);
CREATE INDEX idx_session_startedAt ON review_session_table(startedAt);
```

### 7.4 Entity: study_statistics_table

```sql
CREATE TABLE study_statistics_table (
    id TEXT PRIMARY KEY NOT NULL,
    date TEXT NOT NULL UNIQUE,        -- Format: YYYY-MM-DD
    cardsStudied INTEGER NOT NULL DEFAULT 0,
    minutesStudied INTEGER NOT NULL DEFAULT 0,
    correctAnswers INTEGER NOT NULL DEFAULT 0,
    totalAnswers INTEGER NOT NULL DEFAULT 0,
    streakCount INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_stats_date ON study_statistics_table(date);
```

### 7.5 DAO Definitions

```kotlin
@Dao
interface DeckDao {
    @Query("SELECT * FROM deck_table WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM deck_table WHERE id = :deckId")
    fun getDeckById(deckId: String): Flow<DeckEntity?>

    @Query("""SELECT * FROM deck_table WHERE isArchived = 0 AND
        (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')""")
    fun searchDecks(query: String): Flow<List<DeckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("UPDATE deck_table SET isArchived = :archived WHERE id = :deckId")
    suspend fun archiveDeck(deckId: String, archived: Boolean)

    @Query("DELETE FROM deck_table WHERE id = :deckId")
    suspend fun deleteDeck(deckId: String)

    @Transaction
    suspend fun duplicateDeck(sourceDeckId: String, newDeckId: String) {
        // Implementation với transaction
    }
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_table WHERE deckId = :deckId ORDER BY orderIndex ASC")
    fun getCardsByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("""SELECT * FROM flashcard_table WHERE
        dueDate <= :todayEnd AND isKnown = 0
        ORDER BY dueDate ASC""")
    fun getCardsForReview(todayEnd: Long): Flow<List<FlashcardEntity>>

    @Query("""SELECT * FROM flashcard_table WHERE
        (frontText LIKE '%' || :query || '%' OR
         backText LIKE '%' || :query || '%' OR
         note LIKE '%' || :query || '%')""")
    fun searchCards(query: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Query("UPDATE flashcard_table SET easeFactor = :ef, intervalDays = :interval, " +
           "repetitionCount = :rep, dueDate = :due, lastReviewedAt = :reviewed " +
           "WHERE id = :cardId")
    suspend fun updateReviewMetadata(
        cardId: String, ef: Float, interval: Int,
        rep: Int, due: Long, reviewed: Long
    )

    @Query("DELETE FROM flashcard_table WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("SELECT COUNT(*) FROM flashcard_table WHERE deckId = :deckId")
    fun getCardCount(deckId: String): Flow<Int>
}
```

---

## 8. LUỒNG ĐIỀU HƯỚNG ỨNG DỤNG

### 8.1 Navigation Graph

```kotlin
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object DeckDetail : Screen("deck_detail/{deckId}") {
        fun createRoute(deckId: String) = "deck_detail/$deckId"
    }
    object CreateEditDeck : Screen("create_edit_deck?deckId={deckId}") {
        fun createRoute(deckId: String? = null) =
            if (deckId != null) "create_edit_deck?deckId=$deckId" else "create_edit_deck"
    }
    object CardEditor : Screen("card_editor/{deckId}?cardId={cardId}") {
        fun createRoute(deckId: String, cardId: String? = null) =
            if (cardId != null) "card_editor/$deckId?cardId=$cardId" else "card_editor/$deckId"
    }
    object StudyMode : Screen("study/{deckId}/{mode}") {
        fun createRoute(deckId: String, mode: StudyMode) = "study/$deckId/${mode.name}"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object ImportExport : Screen("import_export")
    object Search : Screen("search")
    object Backup : Screen("backup")
}
```

### 8.2 Sơ Đồ Điều Hướng

```
Splash
  └→ Home
       ├→ DeckDetail(deckId)
       │     ├→ CreateEditDeck(deckId) [edit]
       │     ├→ CardEditor(deckId) [add card]
       │     ├→ CardEditor(deckId, cardId) [edit card]
       │     └→ StudyMode(deckId, mode)
       │             └→ [back to DeckDetail với kết quả]
       ├→ CreateEditDeck() [tạo mới]
       ├→ Search
       │     ├→ DeckDetail
       │     └→ CardEditor
       ├→ Statistics
       ├→ Settings
       │     └→ Backup
       └→ ImportExport
```

---

## 9. ĐẶC TẢ UI/UX

### 9.1 Design System

```
Màu sắc chính (Material 3 Dynamic Color + Seed):
  - Primary: #4A90E2 (Blue)
  - Secondary: #7B61FF (Purple)
  - Tertiary: #00C853 (Green - success)
  - Error: #F44336

Typography:
  - Display: Nunito ExtraBold 32sp
  - Headline: Nunito Bold 24sp
  - Title: Nunito SemiBold 20sp
  - Body: Nunito Regular 16sp
  - Label: Nunito Regular 14sp

Spacing: 4dp grid (4, 8, 12, 16, 24, 32)
Corner Radius: 8dp (card), 16dp (sheet), 24dp (FAB)
Elevation: 0dp (flat), 1dp (card), 8dp (modal)
```

### 9.2 Màn Hình Splash

**Mục đích:** Khởi tạo database, load settings, điều hướng đến Home

**Components:**

- Logo FlashMind (vector animated)
- Tagline: "Học thông minh, nhớ lâu hơn"

**State:** Loading → navigate to Home

**Edge cases:**

- Database migration failure → hiện thông báo lỗi và option reset

---

### 9.3 Màn Hình Home

**Mục đích:** Tổng quan, truy cập nhanh

**Components:**

- TopAppBar: Logo + nút Search + nút Settings
- Banner thống kê nhanh: Streak / Cards due today
- Section "Học hôm nay": Deck có thẻ cần ôn, ưu tiên overdue
- Section "Bộ thẻ yêu thích": Horizontal scroll
- Section "Tất cả bộ thẻ": LazyColumn với DeckCard
- FAB: Thêm deck mới

**State:** HomeUiState (Loading / Success / Error / Empty)

**Interactions:**

- Tap deck card → DeckDetail
- Long press deck → ContextMenu (sửa/xóa/nhân bản/lưu trữ)
- Tap FAB → CreateEditDeck
- Pull-to-refresh để reload

**Edge cases:**

- Không có deck → Empty state với CTA "Tạo bộ thẻ đầu tiên"
- Không có thẻ due → Banner ẩn hoặc hiển thị "Bạn đã ôn xong hôm nay! 🎉"

---

### 9.4 Màn Hình Deck Detail

**Mục đích:** Xem và quản lý thẻ trong deck, chọn chế độ học

**Components:**

- CollapsingTopBar: Tên deck + màu sắc, tiến độ %
- Study Mode Selector: 5 icon chế độ học (horizontal scroll)
- Quick Stats: Tổng thẻ / Đã thuộc / Cần ôn
- LazyColumn: Danh sách thẻ (hiển thị front + back rút gọn)
- FAB: Thêm thẻ mới

**State:** DeckDetailUiState

**Interactions:**

- Tap chế độ học → Study Session tương ứng
- Swipe thẻ → Quick delete
- Tap thẻ → Card Editor
- Long press → Multi-select mode

---

### 9.5 Màn Hình Card Editor

**Mục đích:** Tạo hoặc chỉnh sửa thẻ

**Components:**

- TopAppBar: "Tạo thẻ" / "Sửa thẻ" + nút Save + nút Delete
- TextField: Mặt trước (với ký tự đếm)
- TextField: Mặt sau
- TextField: Phiên âm (tùy chọn)
- TextField: Câu ví dụ (tùy chọn, multiline)
- TextField: Ghi chú (tùy chọn)
- Hình ảnh picker: Preview + nút thêm/xóa ảnh
- Difficulty selector: 3 nút (Dễ/Vừa/Khó)

**State:** CardEditorUiState (Idle / Saving / Error)

**Edge cases:**

- Front hoặc Back trống → hiện lỗi inline
- Ảnh quá lớn (>5MB) → tự scale down hoặc báo lỗi

---

### 9.6 Màn Hình Study Session (Swipe Mode)

**Mục đích:** Ôn thẻ bằng cách lật và phân loại

**Components:**

- Progress bar: X/Total + %
- Tiêu đề deck
- FlashCard 3D (center, chiếm 60% viewport)
  - Mặt trước: frontText + pronunciation
  - Mặt sau: backText + example + note
- Hint nút lật (fade out sau 3 giây)
- Bottom actions: Nút "Không biết" (đỏ) / "Biết" (xanh)
- Nút Undo phía trên

**Animation:**

- Card flip: 300ms, animateFloatAsState với FastOutSlowIn easing
- Swipe: DraggableState với velocity threshold
- Card appear: Slide up + fade in

**Edge cases:**

- Không có thẻ → "Deck chưa có thẻ, thêm ngay!"
- Hoàn thành → Result Screen (điểm, số biết/không biết, thời gian)

---

### 9.7 Màn Hình Quiz

**Mục đích:** Kiểm tra kiến thức bằng trắc nghiệm

**Components:**

- Progress + timer countdown
- Câu hỏi (frontText của thẻ)
- 4 lựa chọn (RadioButton style, full width)
- Sau khi chọn: highlight đúng (xanh) / sai (đỏ) + hiện đáp án đúng
- Nút "Câu tiếp theo"

**Logic Distractor Generation:**

- Lấy ngẫu nhiên 3 thẻ khác trong cùng deck làm đáp án sai
- Shuffle 4 lựa chọn mỗi câu

---

### 9.8 Màn Hình Thống Kê

**Mục đích:** Hiện thị tổng quan tiến độ học tập

**Components:**

- Streak badge (lửa + số ngày)
- 4 summary cards: Tổng thẻ / Đã thuộc / Accuracy / Thời gian học
- Bar chart: Thẻ học theo ngày (7 ngày)
- Calendar heatmap: 12 tuần gần nhất
- Deck performance list: Progress bar từng deck

**Library:** Vẽ chart thuần Compose Canvas (không dùng lib ngoài để tránh dependency)

---

### 9.9 Màn Hình Import/Export

**Mục đích:** Nhập xuất dữ liệu Excel

**Components:**

- Tab: Import | Export
- **Import tab:**
  - Nút chọn file (ActivityResultContract)
  - Preview bảng (LazyColumn + header)
  - Error list theo dòng
  - Chọn deck đích (dropdown)
  - Nút xác nhận Import
- **Export tab:**
  - Danh sách deck với checkbox
  - Toggle "Tất cả deck"
  - Toggle "Bao gồm thống kê"
  - Nút Export (tạo file + share intent)

---

### 9.10 Màn Hình Cài Đặt

**Mục đích:** Tuỳ chỉnh ứng dụng

**Components:**

- Grouped preferences list:
  - **Giao diện:** Theme switch, Font size slider
  - **Học tập:** Chế độ mặc định, Animation
  - **Nhắc nhở:** Toggle + TimePicker
  - **Dữ liệu:** Backup/Restore, Export preferences
  - **Thông tin:** Phiên bản, License

---

## 10. LỘ TRÌNH PHÁT TRIỂN

### Phase 1: Foundation (Tuần 1-3)

**Mục tiêu:** Core infrastructure sẵn sàng

- [ ] Setup project: Hilt, Room, Navigation Compose, DataStore
- [ ] Room schema: DeckEntity, FlashcardEntity, ReviewSessionEntity
- [ ] Implement DAOs với unit tests
- [ ] Repository pattern + Clean Architecture base
- [ ] Material 3 Theme, Typography, Color system
- [ ] Navigation Graph cơ bản
- [ ] Splash Screen

**Deliverable:** App chạy được với navigation shell

---

### Phase 2: Core Features (Tuần 4-7)

**Mục tiêu:** CRUD hoàn chỉnh

- [ ] Home Screen + DeckCard component
- [ ] Create/Edit Deck Screen
- [ ] Deck Detail Screen
- [ ] Card Editor Screen (text + image)
- [ ] Bulk edit cards
- [ ] Search (trong deck và toàn cục)
- [ ] Favorites, Archive, Sort, Filter

**Deliverable:** User có thể tạo và quản lý deck/thẻ hoàn chỉnh

---

### Phase 3: Study Engine (Tuần 8-11)

**Mục tiêu:** Tất cả chế độ học hoạt động

- [ ] SM-2 Algorithm implementation + unit tests
- [ ] Swipe Study Mode (animation 3D)
- [ ] Learn Mode (adaptive)
- [ ] Write Mode (string comparison)
- [ ] Multiple Choice Quiz Mode (distractor generation)
- [ ] Match Game Mode
- [ ] Review Incorrect Mode
- [ ] Session result screen

**Deliverable:** Người dùng có thể học theo 5 chế độ với SM-2

---

### Phase 4: Data Intelligence (Tuần 12-13)

**Mục tiêu:** Thống kê và spaced repetition queue

- [ ] StudyStatistics tracking (tự động ghi nhận mỗi phiên)
- [ ] Statistics Screen (charts, heatmap)
- [ ] Today's Review queue
- [ ] Overdue queue
- [ ] Streak calculation

**Deliverable:** Dashboard thống kê đầy đủ

---

### Phase 5: Import/Export (Tuần 14-15)

**Mục tiêu:** Excel import/export hoạt động

- [ ] Apache POI integration
- [ ] ExcelParser (import + validation)
- [ ] Import preview screen
- [ ] ExcelExporter
- [ ] Sample template generation
- [ ] JSON Backup/Restore
- [ ] DB file backup

**Deliverable:** Import từ Excel và export thành công

---

### Phase 6: Polish & Notifications (Tuần 16-17)

**Mục tiêu:** Production-ready

- [ ] Local notifications (WorkManager)
- [ ] Settings Screen đầy đủ
- [ ] Accessibility improvements
- [ ] Performance profiling + optimization
- [ ] Edge cases handling
- [ ] UI animations polish
- [ ] Error handling toàn diện
- [ ] Dark mode

**Deliverable:** Ứng dụng sẵn sàng production

---

### Phase 7: Testing & Release (Tuần 18-19)

- [ ] Unit tests: UseCases, ViewModels, Repository
- [ ] Integration tests: Database, Parser
- [ ] UI tests: Critical flows (Espresso / Compose Testing)
- [ ] Beta testing với 20 người dùng thực
- [ ] Bug fixes từ beta
- [ ] Google Play Store release

---

## 11. RỦI RO & RÀNG BUỘC

### 11.1 Rủi Ro Kỹ Thuật

| Rủi ro                                    | Khả năng   | Tác động   | Biện pháp giảm thiểu                                      |
| ----------------------------------------- | ---------- | ---------- | --------------------------------------------------------- |
| Apache POI quá nặng, tăng APK size        | Cao        | Trung bình | Dùng POI-OOXML-lite hoặc SimplePOI, proguard rules        |
| Room migration phức tạp khi update schema | Trung bình | Cao        | Viết migration scripts ngay từ đầu, test kỹ               |
| Performance chậm với 10k+ thẻ             | Trung bình | Cao        | Sử dụng Paging 3, index DB, lazy loading                  |
| Memory leak với Compose animation         | Thấp       | Trung bình | LeakCanary integration ngay từ đầu                        |
| File permission thay đổi theo API level   | Cao        | Cao        | Test trên nhiều API levels, dùng Storage Access Framework |

### 11.2 Rủi Ro Sản Phẩm

| Rủi ro                                | Biện pháp                              |
| ------------------------------------- | -------------------------------------- |
| Người dùng không hiểu SM-2            | Giải thích inline, tutorial onboarding |
| Import Excel format không tương thích | Validation rõ ràng + sample template   |
| Mất dữ liệu khi update app            | Auto-backup trước khi migration        |
| UX phức tạp với người dùng mới        | Onboarding flow 3 bước                 |

### 11.3 Ràng Buộc

- **Ngôn ngữ:** Chỉ Kotlin, không sử dụng Java
- **UI:** Chỉ Jetpack Compose, không XML layout
- **Network:** Không có bất kỳ API call nào
- **Analytics:** Không thu thập dữ liệu người dùng
- **Min SDK:** API 26 (để đảm bảo phủ 95%+ thiết bị)
- **APK size:** Mục tiêu < 30MB (sau khi split APK)

---

## 12. PHẠM VI MVP

### In Scope (MVP)

- Deck CRUD (tạo, sửa, xóa, duplicate)
- Flashcard CRUD + hình ảnh
- Swipe Study Mode
- Multiple Choice Quiz Mode
- SM-2 Algorithm cơ bản
- Room Database đầy đủ
- Import Excel (.xlsx) cơ bản
- Export deck ra Excel
- Theme sáng/tối
- Thống kê cơ bản (streak, accuracy, cards due)
- Local notification nhắc nhở

### Out of Scope (MVP)

- Match Game Mode (Phase 2)
- Write Mode (Phase 2)
- Learn Mode nâng cao (Phase 2)
- Calendar heatmap (Phase 2)
- DB Backup/Restore (Phase 2)
- Tablet layout (Phase 3)
- Foldable support (Phase 3)
- Export thống kê (Phase 2)
- Fuzzy search (Phase 2)

---

## 13. CẢI TIẾN TƯƠNG LAI

### V2.0 — Chia Sẻ & Cộng Đồng (6 tháng sau launch)

- Xuất bộ thẻ thành QR code để chia sẻ offline
- Import từ URL (tải file Excel từ link)
- Google Drive Backup (tùy chọn, opt-in)
- Widget Android hiển thị thẻ ngẫu nhiên

### V2.5 — AI Enhancement

- Gợi ý câu ví dụ tự động (on-device LLM)
- Tự động phân loại độ khó dựa trên lịch sử trả lời
- Phát hiện thẻ trùng lặp thông minh
- Text-to-speech phát âm (offline, Android TTS)

### V3.0 — Học Tập Cộng Tác

- Bluetooth sharing: Chia sẻ deck giữa 2 thiết bị gần nhau
- Local network sync (cùng WiFi)

### Cải Tiến UX Liên Tục

- Chế độ luyện tập "Mỗi ngày 10 thẻ" với gamification
- Thẻ âm thanh (ghi âm phát âm)
- Hỗ trợ LaTeX cho công thức toán học
- Markdown trong nội dung thẻ

---

## PHỤ LỤC

### A. Thư Viện & Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Apache POI (Excel)
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Coil (Image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WorkManager (Notifications)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

### B. Chiến Lược Testing

| Loại Test        | Phạm vi                       | Framework       | Mục tiêu Coverage |
| ---------------- | ----------------------------- | --------------- | ----------------- |
| Unit Test        | UseCases, ViewModels, SM-2    | JUnit 4 + Mockk | 80%               |
| Integration Test | Room DAO, Repository          | Room Testing    | 70%               |
| UI Test          | Luồng tạo deck, study session | Compose Testing | Critical paths    |
| Performance Test | 10k thẻ, large Excel          | Macrobenchmark  | Baseline profile  |

### C. Chiến Lược Tối Ưu Hiệu Năng

- **Room:** Sử dụng `Flow` thay `LiveData`, tạo index đúng chỗ, tránh query N+1
- **Compose:** `remember`, `derivedStateOf`, tránh recomposition không cần thiết, `key()` trong LazyList
- **Image:** Coil với cache disk, scale ảnh về max 1024px trước khi lưu
- **Excel:** Parse trong IO dispatcher, stream processing tránh load toàn bộ file vào RAM
- **Animation:** Chạy trên Main thread nhưng dùng Choreographer, 60fps target
- **Startup:** App startup library, lazy DI initialization

---

_Tài liệu này được tạo ra để sẵn sàng triển khai ngay bởi đội phát triển Android senior. Mọi quyết định kỹ thuật đã được cân nhắc kỹ với trade-off rõ ràng._

_Phiên bản: 1.0.0 | Cập nhật lần cuối: 2025_
