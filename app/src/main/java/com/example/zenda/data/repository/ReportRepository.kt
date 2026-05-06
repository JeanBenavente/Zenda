package com.example.zenda.data.repository

import com.example.zenda.data.model.Report
import kotlinx.coroutines.delay
import android.util.Log

interface ReportRepository {
    suspend fun submitReport(report: Report): Result<Unit>
}

class MockReportRepository : ReportRepository {

    override suspend fun submitReport(report: Report): Result<Unit> {
        delay(600)
        Log.d(
            "ZendaApi",
            "POST /reports (mock) body={ id=${report.id}, title=${report.title}, " +
                "category=${report.category}, danger=${report.dangerLevel}, " +
                "lat=${report.latitude}, lng=${report.longitude}, " +
                "imageSource=${report.imageSource}, imageUri=${report.imageUri} }"
        )
        return Result.success(Unit)
    }
}
