package com.example.foodiebuddy.system

import com.itextpdf.kernel.pdf.PdfWriter
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.formatQuantity
import com.example.foodiebuddy.data.formatUnit
import com.example.foodiebuddy.errors.handleError
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.properties.HorizontalAlignment
import timber.log.Timber
import java.io.OutputStream
import java.util.concurrent.CountDownLatch

/**
 * Creates and download a PDF file of the given recipe at the given path.
 *
 * @param context to access string resources
 * @param outputStream to write the PDF file
 * @param recipe Recipe object with information to put in the PDF
 * @param username of the user who created the recipe
 * @param pictureIndexes which pictures to add to the PDF (can be empty)
 * @param addNotes whether or not to add the user's personal notes on the PDF
 * @param notes eventual notes to write on the PDF
 */
fun createRecipePdf(context: Context, outputStream: OutputStream, recipe: Recipe, username: String, pictureIndexes: List<Int>, addNotes: Boolean, notes: String = "") {
    Log.d("Debug", "received ${pictureIndexes.size}")
    // set up document
    val writer = PdfWriter(outputStream)
    val pdfDoc = PdfDocument(writer)
    val document = Document(pdfDoc)

    val font = "res/font/sf_pro_display.ttf"
    val iTextFont = PdfFontFactory.createFont(font)
    document.setFont(iTextFont)

    // row for image, title and credits
    val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 3f))).useAllAvailableWidth()

    // title and credits
    val title = Paragraph(recipe.name)
        .setFontSize(32f)
        .setFixedLeading(36f)
        .setBold()
        .setMarginBottom(18f)

    val credits = Paragraph(
        context.getString(
            R.string.txt_credits,
            username,
            context.getString(R.string.app_name)
        ))
        .setFontSize(18f)
        .setItalic()

    title.setTextAlignment(TextAlignment.CENTER)
    credits.setTextAlignment(TextAlignment.CENTER)
    document.add(title).add(credits)


    // add image
    if (pictureIndexes.isNotEmpty() && recipe.pictures.isNotEmpty()) {
        val imagesCount = pictureIndexes.size.coerceAtMost(3)
        val images = mutableListOf<Image?>()
        val latch = CountDownLatch(imagesCount)

        for (index in pictureIndexes.take(3)) {
            Thread {
                val img = uriToPdfImage(context, recipe.pictures[index])
                synchronized(images) { images.add(img) }
                latch.countDown()
            }.start()
        }
        latch.await()

        val maxImageWidth = 160f
        val totalTableWidth = imagesCount * maxImageWidth

        // Define table with as many columns as images
        val columnWidths = FloatArray(imagesCount) { 1f }
        val imageTable = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPointValue(totalTableWidth))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)

        for (img in images) {
            if (img != null) {
                val ratio = img.imageHeight / img.imageWidth
                img.setWidth(maxImageWidth)
                img.setHeight(maxImageWidth * ratio)
                imageTable.addCell(
                    Cell().add(img)
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            } else {
                imageTable.addCell(
                    Cell().add(Paragraph(context.getString(R.string.txt_pdfImageError)))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            }
        }

        document.add(imageTable)
    }


    document.add(Paragraph(" ").setFontSize(12f))

    // ingredients
    document.add(Paragraph(context.getString(R.string.title_ingredients)).setFontSize(26f).setBold())
    // portion
    val portionUnit = if (recipe.perPerson) {
        if (recipe.portion > 1) context.getString(R.string.txt_people)
        else context.getString(R.string.txt_person)
    } else {
        if (recipe.portion > 1) context.getString(R.string.txt_pieces)
        else context.getString(R.string.txt_piece)
    }
    document.add(Paragraph(
        context.getString(
            R.string.txt_quantitiesFor,
            recipe.portion.toString(),
            portionUnit
        )).setFontSize(18f))
    // list of ingredients
    val ingredientTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f))).useAllAvailableWidth()
    for (ingredient in recipe.ingredients) {
        val quantity = formatQuantity(ingredient.quantity)
        val unit = formatUnit(ingredient.unit, ingredient.quantity, context)
        ingredientTable.addCell(
            Cell().add(Paragraph("$quantity $unit").setFontSize(18f).setBold().setFixedLeading(22f)).setBorder(Border.NO_BORDER)
        )
        ingredientTable.addCell(
            Cell().add(Paragraph(ingredient.displayedName).setFontSize(18f).setFixedLeading(22f)).setBorder(Border.NO_BORDER)
        )
    }
    document.add(ingredientTable)
    document.add(Paragraph(" ").setFontSize(22f))


    // instructions
    document.add(Paragraph(context.getString(R.string.title_recipeInstructions)).setFontSize(26f).setBold())
    recipe.instructions.forEachIndexed { index, step ->
        document.add(Paragraph(context.getString(R.string.title_stepNb, index+1)).setFontSize(22f).setBold())
        document.add(Paragraph(step).setFontSize(18f).setFixedLeading(22f))
    }
    
    // notes
    if (addNotes && notes.isNotBlank()) {
        document.add(Paragraph(" ").setFontSize(22f))
        document.add(Paragraph(context.getString(R.string.title_notes)).setFontSize(26f).setBold())
        document.add(Paragraph(notes).setFontSize(18f).setFixedLeading(22f))
    }

    document.close()
}

/**
 * Creates a file name for the recipe PDF file.
 *
 * @param title of the recipe
 * @param image whether or not to add option "image" in the file name
 * @param notes whether or not to add option "notes" in the file name
 * @return new file object at the given path in the Downloads folder
 */
fun getFileName(title: String, image: Boolean, notes: Boolean): String {
    var fileName = title
    if (image) fileName += "_image"
    if (notes) fileName += "_notes"
    fileName += ".pdf"
    return fileName
}

/**
 * Converts an image URI into an Image file that can be added on the PDF.
 *
 * @param context to access files
 * @param imageUri URI of the image to convert
 * @return PDF-usable image
 */
private fun uriToPdfImage(context: Context, imageUri: Uri): Image? {
    return try {
        val imagePath = if (imageUri.scheme == "http" || imageUri.scheme == "https") {
            downloadRemoteImage(context, imageUri.toString())
        } else {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val tempFile = File.createTempFile("image", ".jpg", context.cacheDir)
            tempFile.outputStream().use { output -> inputStream?.copyTo(output) }
            tempFile.absolutePath
        }
        Image(ImageDataFactory.create(imagePath))
    } catch (e: Exception) {
        Timber.tag("Error").d( "$e")
        handleError(context, "Could not convert uri to pdf image")
        null
    }
}

private fun downloadRemoteImage(context: Context, url: String): String {
    val tempFile = File.createTempFile("remote_image", ".jpg", context.cacheDir)
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Failed to download image: $response")
        response.body?.byteStream()?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return tempFile.absolutePath
}
