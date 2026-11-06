package com.example.caloscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.caloscan.ui.CaloScanApp
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.theme.CaloScanTheme

class MainActivity : ComponentActivity() {
  private val viewModel: CaloScanViewModel by viewModels { CaloScanViewModel.Factory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CaloScanTheme {
        CaloScanApp(viewModel)
      }
    }
  }
}
