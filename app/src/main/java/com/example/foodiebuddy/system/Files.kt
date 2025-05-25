package com.example.foodiebuddy.system

import com.itextpdf.kernel.pdf.PdfWriter
import android.content.Context
import android.net.Uri
import android.os.Environment
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
import timber.log.Timber
import java.util.concurrent.CountDownLatch

/**
 * Creates and download a PDF file of the given recipe at the given path.
 *
 * @param context to access string resources
 * @param filePath path where the
 * @param recipe Recipe object with information to put in the PDF
 * @param username of the user who created the recipe
 * @param addImage whether or not to add the image on the PDF
 * @param addNotes whether or not to add the user's personal notes on the PDF
 * @param notes eventual notes to write on the PDF
 */
fun createRecipePdf(context: Context, filePath: String, recipe: Recipe, username: String, addImage: Boolean, addNotes: Boolean, notes: String = "") {
    // set up document
    val writer = PdfWriter(filePath)
    val pdfDoc = PdfDocument(writer)
    val document = Document(pdfDoc)

    val font = "res/font/sf_pro_display.ttf"
    val iTextFont = PdfFontFactory.createFont(font)
    document.setFont(iTextFont)

    // row for image, title and credits
    val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 3f))).useAllAvailableWidth()

    // add image
    if (addImage && recipe.picture != Uri.EMPTY) {
        // counter for parallel thread
        val latch = CountDownLatch(1)
        Thread {
            val image = uriToPdfImage(context, recipe.picture)
            if (image != null) {
                val width = 200f
                val height = width / (image.imageWidth / image.imageHeight)
                image.setWidth(width)
                image.setHeight(height)
                synchronized(document) { headerTable.addCell(Cell().add(image).setBorder(Border.NO_BORDER)) }
            } else {
                synchronized(document) {
                    document.add(Paragraph(context.getString(R.string.txt_pdfImageError)).setFontSize(18f).setItalic())
                }
            }
            latch.countDown()
        }.start()
        latch.await()
    }

    // title
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

    if (addImage) {
        val headerRightCell = Cell().add(title).add(credits).setBorder(Border.NO_BORDER)
        headerTable.addCell(headerRightCell)
        document.add(headerTable)
    } else {
        title.setTextAlignment(TextAlignment.CENTER)
        credits.setTextAlignment(TextAlignment.CENTER)
        document.add(title).add(credits)
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
 * Creates a filePath with a name that contains information about the recipe's title and the download options chosen.
 *
 * @param title of the recipe
 * @param image whether or not to add option "image" in the file name
 * @param notes whether or not to add option "notes" in the file name
 * @return directory and name of the new filePath
 */
fun getFilePath(title: String, image: Boolean, notes: Boolean): String {
    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    var fileName = title
    if (image) fileName += "_image"
    if (notes) fileName += "_notes"
    fileName += ".pdf"
    return File(directory, fileName).absolutePath
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
