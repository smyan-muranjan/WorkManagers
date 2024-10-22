package com.example.workmanagers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ExamplePeriodicWorker(
    context: Context,
    params: WorkerParameters
    ) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        println("Worker started")
        println("Simulating work")
        delay(5000L)
        println("Worker stopped")
        return Result.success()
    }
}