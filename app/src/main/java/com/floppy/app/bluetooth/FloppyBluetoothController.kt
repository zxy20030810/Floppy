package com.floppy.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FloppyBluetoothDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    val isConnectable: Boolean,
    val source: FloppyBluetoothDeviceSource,
    val bluetoothType: Int = BluetoothDevice.DEVICE_TYPE_UNKNOWN,
    val bondState: Int = BluetoothDevice.BOND_NONE,
    val lastSeenMillis: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "未命名设备"

    val canUseGatt: Boolean
        get() = source == FloppyBluetoothDeviceSource.BleScan ||
            bluetoothType == BluetoothDevice.DEVICE_TYPE_LE ||
            bluetoothType == BluetoothDevice.DEVICE_TYPE_DUAL

    val isBonded: Boolean
        get() = bondState == BluetoothDevice.BOND_BONDED
}

enum class FloppyBluetoothDeviceSource(val label: String) {
    BleScan("BLE"),
    ClassicScan("经典蓝牙"),
    Bonded("已配对")
}

data class FloppyBluetoothState(
    val status: FloppyBluetoothStatus = FloppyBluetoothStatus.Idle,
    val devices: List<FloppyBluetoothDevice> = emptyList(),
    val selectedAddress: String? = null,
    val message: String? = null
)

enum class FloppyBluetoothStatus {
    Idle,
    PermissionRequired,
    BluetoothUnavailable,
    BluetoothOff,
    LocationOff,
    Scanning,
    DevicesFound,
    NotFound,
    Connecting,
    Connected,
    ConnectionFailed
}

class FloppyBluetoothController(private val context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter
        get() = bluetoothManager?.adapter

    private val mutableState = MutableStateFlow(FloppyBluetoothState())
    val state: StateFlow<FloppyBluetoothState> = mutableState.asStateFlow()

    private var scanCallback: ScanCallback? = null
    private var discoveryReceiver: BroadcastReceiver? = null
    private var scanTimeoutJob: Job? = null
    private var bleScanStarted = false
    private var classicDiscoveryStarted = false
    private var connectedGatt: BluetoothGatt? = null

    fun refreshReadiness(hasPermissions: Boolean) {
        if (!hasPermissions) {
            stopScan()
            mutableState.value = FloppyBluetoothState(
                status = FloppyBluetoothStatus.PermissionRequired,
                message = "需要蓝牙权限才能搜索附近设备"
            )
            return
        }
        when {
            bluetoothManager == null || bluetoothAdapter == null -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.BluetoothUnavailable,
                    message = "当前设备不支持蓝牙"
                )
            }
            bluetoothAdapter?.isEnabled != true -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.BluetoothOff,
                    message = "请先打开系统蓝牙"
                )
            }
            !isLocationReady() -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.LocationOff,
                    message = "请打开系统定位，否则部分手机会返回空的蓝牙扫描结果"
                )
            }
            else -> {
                if (mutableState.value.status in readinessStatuses) {
                    mutableState.value = FloppyBluetoothState()
                }
            }
        }
    }

    fun markPermissionRequired() {
        stopScan()
        mutableState.value = FloppyBluetoothState(
            status = FloppyBluetoothStatus.PermissionRequired,
            message = "需要蓝牙权限才能搜索附近设备"
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter

        when {
            bluetoothManager == null || adapter == null -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.BluetoothUnavailable,
                    message = "当前设备不支持蓝牙"
                )
                return
            }
            adapter.isEnabled != true -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.BluetoothOff,
                    message = "请先打开系统蓝牙"
                )
                return
            }
            !isLocationReady() -> {
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.LocationOff,
                    message = "请打开系统定位，否则部分手机会返回空的蓝牙扫描结果"
                )
                return
            }
        }

        stopScan()
        mutableState.value = FloppyBluetoothState(status = FloppyBluetoothStatus.Scanning)
        ensureDiscoveryReceiverRegistered()
        recordBondedDevices(adapter)

        val startedClassicDiscovery = startClassicDiscovery(adapter)
        val startedBleScan = startBleScan(adapter)
        if (!startedBleScan && !startedClassicDiscovery && mutableState.value.devices.isEmpty()) {
            mutableState.value = FloppyBluetoothState(
                status = FloppyBluetoothStatus.ConnectionFailed,
                message = "启动蓝牙搜索失败，请检查蓝牙和定位权限"
            )
            return
        }
        if (!startedBleScan || !startedClassicDiscovery) {
            mutableState.update { current ->
                current.copy(
                    message = when {
                        startedBleScan -> "正在搜索 BLE 设备，经典蓝牙发现启动失败"
                        startedClassicDiscovery -> "正在搜索经典蓝牙设备，BLE 扫描启动失败"
                        else -> current.message
                    }
                )
            }
        }

        scanTimeoutJob = scope.launch {
            delay(SCAN_TIMEOUT_MILLIS)
            finishScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: FloppyBluetoothDevice) {
        val adapter = bluetoothAdapter
        if (adapter?.isEnabled != true) {
            mutableState.value = mutableState.value.copy(
                status = FloppyBluetoothStatus.BluetoothOff,
                selectedAddress = device.address,
                message = "请先打开系统蓝牙"
            )
            return
        }

        stopScan()
        connectedGatt?.close()
        connectedGatt = null
        mutableState.update {
            it.copy(
                status = FloppyBluetoothStatus.Connecting,
                selectedAddress = device.address,
                message = "${device.displayName} 连接中..."
            )
        }

        val remoteDevice = runCatching { adapter.getRemoteDevice(device.address) }.getOrNull()
        if (remoteDevice == null) {
            mutableState.update {
                it.copy(
                    status = FloppyBluetoothStatus.ConnectionFailed,
                    selectedAddress = device.address,
                    message = "无法读取设备地址"
                )
            }
            return
        }

        if (!device.canUseGatt) {
            connectClassicDevice(remoteDevice, device)
            return
        }

        val callback = createGattCallback(device)
        connectedGatt = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                remoteDevice.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
            } else {
                remoteDevice.connectGatt(appContext, false, callback)
            }
        }.getOrElse { error ->
            mutableState.update {
                it.copy(
                    status = FloppyBluetoothStatus.ConnectionFailed,
                    selectedAddress = device.address,
                    message = error.localizedMessage ?: "连接失败，请重试"
                )
            }
            null
        }
    }

    fun release() {
        stopScan()
        unregisterDiscoveryReceiver()
        connectedGatt?.close()
        connectedGatt = null
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        bleScanStarted = false
        classicDiscoveryStarted = false
        val callback = scanCallback
        if (callback != null) {
            scanCallback = null
            runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback) }
        }
        runCatching {
            val adapter = bluetoothAdapter
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
        }
    }

    private fun finishScan() {
        stopScan()
        mutableState.update { current ->
            current.copy(
                status = if (current.devices.isEmpty()) {
                    FloppyBluetoothStatus.NotFound
                } else {
                    FloppyBluetoothStatus.DevicesFound
                },
                message = if (current.devices.isEmpty()) {
                    "暂未发现设备，请确认设备处于配对模式，并打开系统定位"
                } else {
                    "选择一台设备进行连接"
                }
            )
        }
    }

    private fun createScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                recordScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::recordScanResult)
            }

            override fun onScanFailed(errorCode: Int) {
                scanCallback = null
                scanTimeoutJob?.cancel()
                scanTimeoutJob = null
                mutableState.value = FloppyBluetoothState(
                    status = FloppyBluetoothStatus.ConnectionFailed,
                    message = "蓝牙扫描失败：$errorCode"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan(adapter: BluetoothAdapter): Boolean {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        val scanner = adapter.bluetoothLeScanner ?: return false
        val callback = createScanCallback()
        scanCallback = callback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        return runCatching {
            scanner.startScan(null, settings, callback)
            bleScanStarted = true
            true
        }.getOrElse {
            scanCallback = null
            bleScanStarted = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery(adapter: BluetoothAdapter): Boolean {
        runCatching {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        }
        classicDiscoveryStarted = runCatching { adapter.startDiscovery() }.getOrDefault(false)
        return classicDiscoveryStarted
    }

    @SuppressLint("MissingPermission")
    private fun recordBondedDevices(adapter: BluetoothAdapter) {
        runCatching { adapter.bondedDevices.orEmpty() }
            .getOrDefault(emptySet())
            .forEach { device ->
                recordBluetoothDevice(
                    device = device,
                    rssi = null,
                    source = FloppyBluetoothDeviceSource.Bonded,
                    isConnectable = true
                )
            }
    }

    @SuppressLint("MissingPermission")
    private fun recordScanResult(result: ScanResult) {
        recordBluetoothDevice(
            device = result.device,
            rssi = result.rssi,
            source = FloppyBluetoothDeviceSource.BleScan,
            isConnectable = result.isConnectable,
            advertisedName = result.scanRecord?.deviceName
        )
    }

    @SuppressLint("MissingPermission")
    private fun recordBluetoothDevice(
        device: BluetoothDevice,
        rssi: Int?,
        source: FloppyBluetoothDeviceSource,
        isConnectable: Boolean,
        advertisedName: String? = null
    ) {
        val address = runCatching { device.address }.getOrNull() ?: return
        val discoveredDevice = FloppyBluetoothDevice(
            address = address,
            name = advertisedName ?: runCatching { device.name }.getOrNull(),
            rssi = rssi,
            isConnectable = isConnectable,
            source = source,
            bluetoothType = runCatching { device.type }.getOrDefault(BluetoothDevice.DEVICE_TYPE_UNKNOWN),
            bondState = runCatching { device.bondState }.getOrDefault(BluetoothDevice.BOND_NONE)
        )
        mutableState.update { current ->
            val merged = (current.devices.filterNot { it.address == address } + discoveredDevice)
                .sortedWith(
                    compareByDescending<FloppyBluetoothDevice> { if (it.isBonded) 1 else 0 }
                        .thenByDescending { if (it.name?.isNotBlank() == true) 1 else 0 }
                        .thenByDescending { if (it.canUseGatt || it.isConnectable) 1 else 0 }
                        .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                )
            current.copy(
                status = FloppyBluetoothStatus.Scanning,
                devices = merged,
                message = "正在搜索，已发现 ${merged.size} 台设备"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectClassicDevice(remoteDevice: BluetoothDevice, device: FloppyBluetoothDevice) {
        if (device.isBonded) {
            openSystemBluetoothSettings(device)
            return
        }

        val pairingStarted = runCatching { remoteDevice.createBond() }.getOrDefault(false)
        mutableState.update {
            it.copy(
                status = if (pairingStarted) {
                    FloppyBluetoothStatus.Connecting
                } else {
                    FloppyBluetoothStatus.ConnectionFailed
                },
                selectedAddress = device.address,
                message = if (pairingStarted) {
                    "${device.displayName} 正在发起配对..."
                } else {
                    "${device.displayName} 配对失败，请在系统蓝牙中尝试"
                }
            )
        }
    }

    private fun openSystemBluetoothSettings(device: FloppyBluetoothDevice) {
        runCatching {
            appContext.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        mutableState.update {
            it.copy(
                status = FloppyBluetoothStatus.DevicesFound,
                selectedAddress = device.address,
                message = "${device.displayName} 已配对，请在系统蓝牙页完成连接"
            )
        }
    }

    private fun ensureDiscoveryReceiverRegistered() {
        if (discoveryReceiver != null) return
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.bluetoothDeviceExtra() ?: return
                        val rssi = intent.getShortExtra(
                            BluetoothDevice.EXTRA_RSSI,
                            Short.MIN_VALUE
                        ).takeUnless { it == Short.MIN_VALUE }?.toInt()
                        recordBluetoothDevice(
                            device = device,
                            rssi = rssi,
                            source = FloppyBluetoothDeviceSource.ClassicScan,
                            isConnectable = true
                        )
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device = intent.bluetoothDeviceExtra() ?: return
                        recordBluetoothDevice(
                            device = device,
                            rssi = null,
                            source = FloppyBluetoothDeviceSource.Bonded,
                            isConnectable = true
                        )
                        updateBondState(device)
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        classicDiscoveryStarted = false
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(discoveryReceiver, filter)
        }
    }

    private fun unregisterDiscoveryReceiver() {
        val receiver = discoveryReceiver ?: return
        discoveryReceiver = null
        runCatching { appContext.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission")
    private fun updateBondState(device: BluetoothDevice) {
        val address = runCatching { device.address }.getOrNull() ?: return
        if (mutableState.value.selectedAddress != address) return
        when (runCatching { device.bondState }.getOrDefault(BluetoothDevice.BOND_NONE)) {
            BluetoothDevice.BOND_BONDED -> {
                mutableState.update {
                    it.copy(
                        status = FloppyBluetoothStatus.Connected,
                        selectedAddress = address,
                        message = "${device.name ?: "设备"} 已配对"
                    )
                }
            }
            BluetoothDevice.BOND_NONE -> {
                mutableState.update {
                    it.copy(
                        status = FloppyBluetoothStatus.ConnectionFailed,
                        selectedAddress = address,
                        message = "${device.name ?: "设备"} 配对未完成"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(device: FloppyBluetoothDevice): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when {
                    status != BluetoothGatt.GATT_SUCCESS -> {
                        gatt.close()
                        if (connectedGatt == gatt) {
                            connectedGatt = null
                        }
                        mutableState.update {
                            it.copy(
                                status = FloppyBluetoothStatus.ConnectionFailed,
                                selectedAddress = device.address,
                                message = "${device.displayName} 连接失败"
                            )
                        }
                    }
                    newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                        connectedGatt = gatt
                        runCatching { gatt.discoverServices() }
                        mutableState.update {
                            it.copy(
                                status = FloppyBluetoothStatus.Connected,
                                selectedAddress = device.address,
                                message = "${device.displayName} 已连接"
                            )
                        }
                    }
                    newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                        gatt.close()
                        if (connectedGatt == gatt) {
                            connectedGatt = null
                        }
                        mutableState.update {
                            it.copy(
                                status = FloppyBluetoothStatus.ConnectionFailed,
                                selectedAddress = device.address,
                                message = "${device.displayName} 已断开"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isLocationReady(): Boolean {
        val locationManager = appContext.getSystemService(LocationManager::class.java)
        return locationManager?.isLocationEnabled ?: true
    }

    private companion object {
        const val SCAN_TIMEOUT_MILLIS = 14_000L
        val readinessStatuses = setOf(
            FloppyBluetoothStatus.PermissionRequired,
            FloppyBluetoothStatus.BluetoothUnavailable,
            FloppyBluetoothStatus.BluetoothOff,
            FloppyBluetoothStatus.LocationOff
        )
    }
}

private fun Intent.bluetoothDeviceExtra(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
