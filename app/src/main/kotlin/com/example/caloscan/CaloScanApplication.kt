package com.example.caloscan

import android.app.Application
import com.example.caloscan.data.FoodRepository
import com.example.caloscan.data.local.AppDatabase
import com.example.caloscan.data.remote.OpenFoodFactsApi

/** Creates the database and the repository once, when the app starts. */
class CaloScanApplication : Application() {
  val repository: FoodRepository by lazy {
    FoodRepository(AppDatabase.create(this), OpenFoodFactsApi())
  }
}
