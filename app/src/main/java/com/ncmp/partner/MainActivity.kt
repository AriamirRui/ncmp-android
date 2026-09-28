package com.ncmp.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncmp.partner.ui.NcmpRoot
import com.ncmp.partner.ui.theme.NcmpTheme
import com.ncmp.partner.vm.NcmpViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NcmpTheme {
                val vm: NcmpViewModel = viewModel()
                NcmpRoot(vm)
            }
        }
    }
}
