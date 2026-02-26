package ru.rostov.LiveParser

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.IOException

class PlacesParser(private val context: Context) {


    suspend fun parsePlaces(): MutableList<Place> {
        val placesList = mutableListOf<Place>()
        val url = "https://minzdrav.donland.ru/upload/uf/618/t6vs2904z81gs6xwr6pm7rjgtv6ze1ib/Monitoring-_-2.xlsx"

        val file = File(context.cacheDir, "monitoring.xlsx")

        try {
            downloadFile(url, file)

            WorkbookFactory.create(file).use { workbook ->
                val sheet = workbook.getSheetAt(0)

                for (row in sheet) {
                    val firstCell = row?.getCell(0) ?: continue

                    val idStr = when (firstCell.cellType) {
                        CellType.NUMERIC -> firstCell.numericCellValue.toInt().toString()
                        CellType.STRING -> firstCell.stringCellValue.trim().replace(".0", "")
                        else -> ""
                    }

                    if (idStr.toIntOrNull() != null) {
                        val place = Place(
                            id = idStr,
                            name = getCellValueSafe(row.getCell(1)),
                            adress = getCellValueSafe(row.getCell(2)),
                            docs = getCellValueSafe(row.getCell(3)),
                            type = getCellValueSafe(row.getCell(4)),
                            invalidnost = getCellValueSafe(row.getCell(15))
                        )
                        placesList.add(place)
                    }
                }
            }

            file.delete()

        } catch (e: Exception) {
            throw IOException("Ошибка при парсинге файла: ${e.message}", e)
        }

        return placesList
    }

    private fun getCellValueSafe(cell: org.apache.poi.ss.usermodel.Cell?): String {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue.replace("\n", " ").trim()
            CellType.NUMERIC -> {
                val value = cell.numericCellValue
                if (value == value.toInt().toDouble()) {
                    value.toInt().toString()
                } else {
                    value.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    when (cell.cachedFormulaResultType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        else -> ""
                    }
                } catch (e: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(url: String, destination: File) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .addHeader("Accept", "*/*")
            .addHeader("Referer", "https://minzdrav.donland.ru/documents/18265/")
            .addHeader("Connection", "keep-alive")
            .build()

        try {
            val response = client.newCall(request).execute()

            if (response.code == 403) {
                throw IOException("Доступ заблокирован (403). Попробуйте зайти через браузер или сменить IP.")
            }

            if (!response.isSuccessful) {
                throw IOException("Ошибка загрузки файла: ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Тело ответа пустое")

        } catch (e: IOException) {
            throw IOException("Ошибка сети: ${e.message}", e)
        }
    }
}