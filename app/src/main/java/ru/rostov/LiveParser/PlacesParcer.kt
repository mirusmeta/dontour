package ru.rostov.LiveParser

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.rostov.database.SupaBaseConfig
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class PlacesParser(private val context: Context) {

    private val storageBaseUrl = "${SupaBaseConfig.supabaseUrl.supabaseUrl}/storage/v1/object/public/Images"

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

                    // Если ID — число, парсим строку
                    if (idStr.toIntOrNull() != null) {
                        val nameValue = getCellValueSafe(row.getCell(1))

                        // Генерируем ссылки только если имя не пустое
                        val photoUrls = if (nameValue.isNotEmpty()) {
                            generatePhotoUrls(nameValue)
                        } else {
                            emptyList()
                        }

                        val place = Place(
                            id = idStr,
                            name = nameValue,
                            adress = getCellValueSafe(row.getCell(2)),
                            docs = getCellValueSafe(row.getCell(3)),
                            type = getCellValueSafe(row.getCell(4)),
                            invalidnost = getCellValueSafe(row.getCell(15)),
                            photos = photoUrls
                        )
                        placesList.add(place)
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            Log.e("PlacesParser", "Parsing error: ${e.message}")
            throw IOException("Ошибка при парсинге файла: ${e.message}", e)
        }
        return placesList
    }

    private fun generatePhotoUrls(name: String): List<String> {
        val urls = mutableListOf<String>()
        for (i in 0..2) { // Берем основное фото и 2 копии
            val fileName = if (i == 0) "$name.png" else "$name($i).png"

            try {
                val encodedName = URLEncoder.encode(fileName, "UTF-8")
                    .replace("+", "%20")

                val finalUrl = "$storageBaseUrl/$encodedName"
                Log.d("SupabaseURL", "Generated: $finalUrl") // Проверка в логах
                urls.add(finalUrl)
            } catch (e: Exception) {
                Log.e("SupabaseURL", "Encoding error for $fileName")
            }
        }
        return urls
    }

    private fun getCellValueSafe(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.replace("\n", " ").trim()
            CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            else -> ""
        }
    }

    private fun downloadFile(url: String, destination: File) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")
            response.body?.byteStream()?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}