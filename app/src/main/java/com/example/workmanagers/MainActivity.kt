package com.example.workmanagers

import PhotoViewModel
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.example.workmanagers.ui.theme.WorkManagersTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val vm by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            WorkManagersTheme {
                val workerResult = vm.workId?.let {
                    workManager.getWorkInfoByIdLiveData(it).observeAsState().value
                }
                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        val filePath = workerResult.outputData.getString(PhotoCompressionWorker.KEY_RESULT_PATH)
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            vm.updateCompressedBitmap(bitmap)
                        }
                    }
                }

                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { startPeriodicWorker() }
                    ) {
                        Text(text="Start Periodic Worker")
                    }
                    vm.uncompressedUri?.let {
                        Text(text = "Uncompressed Photo:")
                        AsyncImage(
                            model = it,
                            contentDescription = ""
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    vm.compressedBitmap?.let {
                        Text(text = "Compressed Photo:")
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = ""
                        )
                    }
                }
            }

        }
    }

    private fun startPeriodicWorker() {
        val request = PeriodicWorkRequestBuilder<ExamplePeriodicWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueue(request)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        vm.updateUncompressedUri(uri)
        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                    PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .setConstraints(
                androidx.work.Constraints(
                    requiresStorageNotLow = true
                )
            )
            .build()
        vm.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}