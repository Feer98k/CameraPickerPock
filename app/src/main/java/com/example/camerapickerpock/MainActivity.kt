package com.example.camerapickerpock

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import com.example.camerapickerpock.ui.theme.CameraPickerPockTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.rememberImagePainter
import coil.size.Scale

private const val RESULT_CODE_PERMISSION = 5005
private const val RESULT_CODE_FILE_PICKER = 5006

class MainActivity : ComponentActivity() {

    // jeito errado de iniciar uma view model...
    private val viewModel: MainActivityViewModel by lazy {
        val lastViewModel = lastCustomNonConfigurationInstance as? MainActivityViewModel
        lastViewModel ?: MainActivityViewModel()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermission()

        if (savedInstanceState != null) {
            val uriString = savedInstanceState.getString("cameraImageUri")
            viewModel.outputFileUri.value = if (uriString != null) Uri.parse(uriString) else null
        }
        setContent {
            CameraPickerPockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraPickScreen(viewModel)
                }
            }
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                RESULT_CODE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RESULT_CODE_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissões concedidas, continue com a operação
                } else {
                    // Permissões negadas, trate o caso
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_CODE_FILE_PICKER && resultCode == Activity.RESULT_OK) {
            // Obter o Uri da imagem
            val imageUri: Uri? = data?.data ?: viewModel.outputFileUri.value

            imageUri?.let { uri ->
                viewModel.uri.value = uri
                viewModel.hasFile.value = true
                // Consultar o ContentResolver para obter os metadados do arquivo
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {

                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        viewModel.fileName.value = it.getString(nameIndex)


                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        viewModel.fileSize.value = it.getLong(sizeIndex).toString()

                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.outputFileUri?.let {
            outState.putString("cameraImageUri", it.toString())
        }
    }


}

@Composable
private fun CameraPickScreen(viewModel: MainActivityViewModel) {
    val context = LocalContext.current
    CameraPickerPockTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (!viewModel.hasFile.value) {
                Button(onClick = { sendArchive(context,viewModel) }) {
                    Row {
                        Text(text = "Enviar arquivo", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier =
                    Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = rememberImagePainter(
                            data = viewModel.uri.value,
                            builder = {
                                crossfade(true)
                                scale(Scale.FILL)

                            }
                        ),
                        contentDescription = "Descrição da imagem",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )

                    Text(
                        text = "Nome do Arquivo: ${viewModel.fileName.value}",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Tamanho do Arquivo: ${viewModel.fileSize.value} Kbs",
                        color = Color.Black,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { viewModel.clearData() }) {
                        Text(
                            text = "Tentar novamente",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                }
            }
        }
    }
}

fun sendArchive(context: Context,mainActivityViewModel: MainActivityViewModel) {
    context.apply {
        this.getActivity()?.startActivityForResult(
            getPickImageChooserIntent(this,mainActivityViewModel),
            RESULT_CODE_FILE_PICKER
        )
    }
}

fun getCaptureImageOutputUri(context: Context): Uri? {
    // Verifica se o armazenamento externo está disponível
    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        val mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Cria um arquivo de imagem
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(mediaStorageDir, "IMG_$timeStamp.jpg")

        // Retorna o Uri do arquivo
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }
    return null
}


fun getPickImageChooserIntent(context: Context,mainActivityViewModel: MainActivityViewModel): Intent {
    // Determine Uri of camera image to save.
     mainActivityViewModel.outputFileUri.value = getCaptureImageOutputUri(context)

    val allIntents = arrayListOf<Intent>()
    val packageManager = context.packageManager

    // collect all camera intents
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val listCam = packageManager.queryIntentActivities(captureIntent, 0)
    for (res in listCam) {
        val intent = Intent(captureIntent)
        intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
        intent.`package` = res.activityInfo.packageName
        mainActivityViewModel.outputFileUri.value?.let {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
        }
        allIntents.add(intent)
    }

    // collect all gallery intents
    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
    galleryIntent.type = "image/*"
    val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
    for (res in listGallery) {
        val intent = Intent(galleryIntent)
        intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
        intent.`package` = res.activityInfo.packageName
        allIntents.add(intent)
    }

    // the main intent is the last in the list so pick up the useless one
    var mainIntent = if (allIntents.isNotEmpty()) {
        allIntents.last()
    } else Intent()
    for (intent in allIntents) {
        if (intent.component?.className == "com.android.documentsui.DocumentsActivity") {
            mainIntent = intent
            break
        }
    }
    allIntents.remove(mainIntent)

    // Create a chooser from the main intent
    val chooserIntent = Intent.createChooser(mainIntent, "Selecione a origem da imagem")

    // Add all other intents
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())

    return chooserIntent
}

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

data class FileStateValue(
    var hasValue: MutableLiveData<Boolean>? = null,
    var fileName: MutableLiveData<String>? = null,
    var fileSize: MutableLiveData<String>? = null,
)
