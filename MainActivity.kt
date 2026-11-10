package com.taxivn.dispatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taxivn.dispatch.data.model.UserRole
import com.taxivn.dispatch.ui.screens.DispatcherHomeScreen
import com.taxivn.dispatch.ui.screens.DriverHomeScreen
import com.taxivn.dispatch.ui.screens.LoginScreen
import com.taxivn.dispatch.ui.screens.SplashScreen
import com.taxivn.dispatch.ui.theme.TaxiVnTheme
import com.taxivn.dispatch.ui.viewmodel.TaxiVnViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaxiVnTheme {
                val vm: TaxiVnViewModel = viewModel()
                val state by vm.uiState.collectAsState()

                when {
                    !state.isSplashDone -> {
                        SplashScreen(serverReachable = state.serverReachable)
                    }
                    state.currentUser == null -> {
                        LoginScreen(
                            serverUrl = state.serverUrl,
                            serverReachable = state.serverReachable,
                            isLoading = state.isLoading,
                            authError = state.authError,
                            onUpdateServerUrl = vm::updateServerUrl,
                            onLogin = vm::login
                        )
                    }
                    state.currentUser?.role == UserRole.DRIVER -> {
                        DriverHomeScreen(
                            state = state,
                            onToggleGps = { enable ->
                                if (enable) vm.startGpsTracking() else vm.stopGpsTracking()
                            },
                            onChangeStatus = vm::setDriverStatus,
                            onAdvanceTrip = vm::advanceTripStep,
                            onRefresh = vm::refreshSnapshot,
                            onLogout = vm::logout,
                            onClearMessage = vm::clearStatusMessage
                        )
                    }
                    else -> {
                        DispatcherHomeScreen(
                            state = state,
                            onCreateTrip = vm::createTrip,
                            onAssignDriver = vm::assignDriverToTrip,
                            onUpdateTripStatus = vm::advanceTripStep,
                            onChangeDriverStatus = vm::setDriverStatus,
                            onSettleDebt = vm::settleDriverDebt,
                            onRefresh = vm::refreshSnapshot,
                            onLogout = vm::logout,
                            onClearMessage = vm::clearStatusMessage
                        )
                    }
                }
            }
        }
    }
}
