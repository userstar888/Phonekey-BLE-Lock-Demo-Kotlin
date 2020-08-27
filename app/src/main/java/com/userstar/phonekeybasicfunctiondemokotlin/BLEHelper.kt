package com.userstar.phonekeybasicfunctiondemokotlin

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.*
import android.content.Context
import android.widget.Toast
import com.userstar.phonekeyblelock.AbstractPhonekeyBLEHelper
import timber.log.Timber
import java.util.*

class BLEHelper : AbstractPhonekeyBLEHelper() {

    companion object {
        @JvmStatic
        private var instance: BLEHelper? = null
        fun getInstance() : BLEHelper {
            if (instance ==null) {
                instance = BLEHelper()
            }
            return instance as BLEHelper
        }

        private val UUID_SERVICE: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_WRITE: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_NOTIFY: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
    }

    private var adapter: BluetoothAdapter? = null
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var scanCallback: ScanCallback
    fun startScan(
        context: Context,
        nameFilter: Array<String>?,
        callback: ScanCallback
    ) {
        adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter==null || !adapter!!.isEnabled) {
            Toast.makeText(context, "DEVICE NOT SUPPORT BLE!!!", Toast.LENGTH_LONG).show()
            return
        } else {
            bluetoothLeScanner = adapter!!.bluetoothLeScanner
            scanCallback = callback
            if (nameFilter==null) {
                Timber.i("start scan")
                bluetoothLeScanner.startScan(scanCallback)
            } else {
                val scanFilterArray = List<ScanFilter>(nameFilter.size) { index ->
                    ScanFilter.Builder().apply {
                        setDeviceName(nameFilter[index])
                    }.build()
                }
                val setting = ScanSettings.Builder().apply {
                    setReportDelay(0)
                    setScanMode(SCAN_MODE_LOW_LATENCY)
                }.build()
                Timber.i("start scan with filter")
                bluetoothLeScanner.startScan(scanFilterArray, setting, callback)
            }
        }
    }

    fun stopScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    var bluetoothGatt: BluetoothGatt? = null
    var gattCharacteristicWrite: BluetoothGattCharacteristic? = null
    var gattCharacteristicNotify: BluetoothGattCharacteristic? = null
    fun connectBLE(
        context: Context,
        device: BluetoothDevice,
        connectedCallback: ()->Unit,
        disconnectedCallback: ()->Unit
    ) {
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Timber.i("Connected to GATT server.")
                        Timber.i("Attempting to start service discovery: ${bluetoothGatt?.discoverServices()}")
                        stopScan()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Timber.w("Disconnected from GATT server.")
                        Timber.w("device disconnected!!!")
                        disconnectedCallback()
                    }
                }
            }

            // New services discovered
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Timber.i("services discovered: $status")
                bluetoothGatt = gatt

                val gattService = bluetoothGatt!!.getService(UUID_SERVICE)
                if (gattService == null) {
                    Timber.e("UUID_SERVICE service didn't find")
                    return
                }

                gattCharacteristicWrite = gattService.getCharacteristic(UUID_CHARACTERISTIC_WRITE)
                if (gattCharacteristicWrite==null) {
                    Timber.e("Characteristic for writing didn't find")
                    return
                }

                gattCharacteristicNotify = gattService.getCharacteristic(UUID_CHARACTERISTIC_NOTIFY)
                if (gattCharacteristicNotify==null) {
                    Timber.e("Characteristic for notifying didn't find")
                    return
                }

                if (!bluetoothGatt!!.setCharacteristicNotification(gattCharacteristicNotify, true)) {
                    Timber.e("Notification error")
                    return
                }

                connectedCallback()
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                if (characteristic!=null) {
                    callback(characteristic)
                } else {
                    Timber.w("characteristic null")
                }
            }
        })
    }

    override lateinit var callback: (BluetoothGattCharacteristic) -> Unit
    override fun write(data: ByteArray, callback: (BluetoothGattCharacteristic) -> Unit) {
        this.callback = callback
        gattCharacteristicWrite!!.value = data
        Timer().schedule(object : TimerTask() {
            override fun run() {
                bluetoothGatt!!.writeCharacteristic(gattCharacteristicWrite!!)
            }
        }, 100)
    }

    fun disConnectBLE() {
        bluetoothGatt!!.disconnect()
        bluetoothGatt!!.close()
        bluetoothGatt = null
    }
}