package com.example.networkscanner.ui.viewmodel

import com.example.networkscanner.data.repository.LanScannerRepository
import com.example.networkscanner.domain.service.PortScannerService
import com.example.networkscanner.ui.state.ScanUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val lanScannerRepository = mockk<LanScannerRepository>()
    private val portScannerService = mockk<PortScannerService>()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test UI state transitions during network scan`() = runTest {
        coEvery { lanScannerRepository.scanLocalNetwork() } returns emptyList()
        
        val viewModel = LanScannerViewModel(lanScannerRepository, portScannerService)
        
        assertTrue(viewModel.uiState.value is ScanUiState.Idle)
        
        viewModel.scanNetwork()
        
        // Before dispatcher runs the coroutine fully, it should be in scanning state
        assertTrue(viewModel.uiState.value is ScanUiState.Scanning)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value is ScanUiState.Success)
    }
}
