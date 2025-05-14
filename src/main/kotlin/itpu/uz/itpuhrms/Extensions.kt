package itpu.uz.itpuhrms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


fun MultipartFile.fileName() = originalFilename!!.substring(0, originalFilename?.lastIndexOf(".") ?: 0)

fun File.fileName() = name.substring(0, name.lastIndexOf("."))

fun MultipartFile.fileExtension() = originalFilename!!.substring((originalFilename?.lastIndexOf(".") ?: 0) + 1)




inline fun <reified R : Any> R.logger(): Logger = LoggerFactory.getLogger(R::class.java.canonicalName)


fun convertToDate(localDate: LocalDate, localTime: LocalTime): Date {
    val dateTime = LocalDateTime.of(localDate, localTime)
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
}


fun TourniquetEmployeeType.toLower(): String {
    return this.name.lowercase()
}

fun Gender.toLower(): String {
    return this.name.lowercase()
}



fun String.isNumeric(): Boolean {
    return this.toLongOrNull() != null
}

fun LocalDate.weekDay() = DayOfWeek.valueOf(this.dayOfWeek.name)


class CustomMultipartFile(
    private val imgContent: ByteArray,
    private val fileName: String, private val contentType: String,
    private val fileOriginalName: String,
) : MultipartFile {

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(imgContent)
    }

    override fun getName(): String {
        return fileName
    }

    override fun getOriginalFilename(): String {
        return fileOriginalName
    }

    override fun getContentType(): String {
        return contentType
    }

    override fun isEmpty(): Boolean {
        return imgContent.isEmpty()
    }

    override fun getSize(): Long {
        return imgContent.size.toLong()
    }

    override fun getBytes(): ByteArray {
        return imgContent
    }

    override fun transferTo(dest: File) {
        FileOutputStream(dest).write(imgContent)
    }

    fun fileName(): String {
        return fileName
    }
}

fun FileAsset.multipartFile(): CustomMultipartFile {
    return CustomMultipartFile(
        File("${uploadFolder}/${uploadFileName}").readBytes(),
        uploadFileName,
        fileContentType,
        fileName
    )
}

fun Long.localDateTime(): String {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), TimeZone.getDefault().toZoneId())
        .truncatedTo(ChronoUnit.SECONDS).toString()
}

fun Long.localDate(): LocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
fun Long.localDateWithUTC(): LocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(this), ZoneId.of("UTC"))


fun Date.minuteDuration(lateDate: Date): Int {
    val duration = lateDate.time - this.time
    return (duration / (60 * 1000)).toInt()
}

fun Date.midnight(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 0
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    return calendar.time
}

fun Date.plusDay(day: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.DAY_OF_YEAR, day)
    return calendar.time
}

fun Date.plusMonth(month: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.MONTH, month)
    return calendar.time
}

fun Date.plusHour(hour: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, hour)
    return calendar.time
}

fun Date.plusMin(min: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.MINUTE, min)
    return calendar.time
}


fun Date.startOfMonth(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 0
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    calendar[Calendar.DAY_OF_MONTH] = calendar.getActualMinimum(Calendar.DAY_OF_MONTH)
    return calendar.time
}

fun Date.weekDay(): DayOfWeek {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val weekDay = calendar.get(Calendar.DAY_OF_WEEK)
    val adjustedDayOfWeek = if (weekDay == Calendar.SUNDAY) 7 else weekDay - 1
    return DayOfWeek.of(adjustedDayOfWeek.toShort())
}

fun String.extractPasswordAndUsername(): List<String> {
    if (!this.startsWith("Basic")) throw InvalidAuthorizationTypeException()
    val credentials = String(Base64.getDecoder().decode(this.removePrefix("Basic ")))
    val split = credentials.split(":")
    if (split.size != 2) throw InvalidAuthorizationTypeException()
    return split
}

fun Boolean.runIfTrue(func: () -> Unit) {
    if (this) func()
}

fun Boolean.runIfFalse(func: () -> Unit) {
    if (!this) func()
}

fun Any.hash(): String {
    val bytes = this.toString().toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun Employee.isBusy(): Boolean {
    return this.phStatus == PositionHolderStatus.BUSY
}

fun Employee.isVacant(): Boolean {
    return this.phStatus == PositionHolderStatus.VACANT
}

fun Month.nameUzb(): String {
    return when (this) {
        Month.JANUARY -> "Yanvar"
        Month.FEBRUARY -> "Fevral"
        Month.MARCH -> "Mart"
        Month.APRIL -> "Aprel"
        Month.MAY -> "May"
        Month.JUNE -> "Iyun"
        Month.JULY -> "Iyul"
        Month.AUGUST -> "Avgust"
        Month.SEPTEMBER -> "Sentabr"
        Month.OCTOBER -> "Oktabr"
        Month.NOVEMBER -> "Noyabr"
        Month.DECEMBER -> "Dekabr"
    }
}

fun Date.daysBetween(date: Date): Long {
    val diffInMillis = abs(this.time - date.time)
    return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
}

fun Long.timeWithUTC(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}

fun Long.monthNameWithUTC(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("MMMM", Locale("uz")) // "MMMM" gives full month name
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}

fun Long.yearWithUTC(): Int {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val date = Date(this)
    utcCalendar.time = date
    return utcCalendar.get(Calendar.YEAR)
}

fun Long.monthWithUTC(): Int {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val date = Date(this)
    utcCalendar.time = date
    return utcCalendar.get(Calendar.MONTH)
}

fun Long.dayWithUTC(): Int {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val date = Date(this)
    utcCalendar.time = date
    return utcCalendar.get(Calendar.DAY_OF_MONTH)
}

fun Date.elevenAM(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 11
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    return calendar.time
}

fun Date.threePM(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 15
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    return calendar.time
}

fun Date.endOfDay(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar[Calendar.HOUR_OF_DAY] = 23
    calendar[Calendar.MINUTE] = 59
    calendar[Calendar.SECOND] = 59
    calendar[Calendar.MILLISECOND] = 0
    return calendar.time
}

fun LocalTime.prettyString(): String {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return this.format(timeFormatter)
}

fun LocalDateTime.prettyString(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return "\uD83D\uDD53${this.format(formatter)}"
}

fun Date.prettyDateTimeString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
    return formatter.format(this)
}

fun Date.prettyDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd")
    return formatter.format(this)
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun TelegramApiRequestException.isBlockedByUser(): Boolean {
    return errorCode == 403 && apiResponse.equals("Forbidden: bot was blocked by the user")
}

fun Long.toYYYYMMDD(format: String = "yyyy-MM-dd"): String {
    return Date(this).prettyDateString()
}
