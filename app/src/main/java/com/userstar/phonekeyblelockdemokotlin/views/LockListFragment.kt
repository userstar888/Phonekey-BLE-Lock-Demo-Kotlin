package com.userstar.phonekeyblelockdemokotlin.views

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.userstar.phonekeyblelockdemokotlin.BLEHelper
import com.userstar.phonekeyblelockdemokotlin.R
import kotlinx.android.synthetic.main.lock_list_fragment.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class LockListFragment : Fragment() {

    private lateinit var lockListRecyclerViewAdapter: LockListRecyclerViewAdapter
    private lateinit var lockListRecyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.lock_list_fragment, container, false)

        lockListRecyclerView = view.findViewById(R.id.lock_list_recyclerView)
        lockListRecyclerView.layoutManager = LinearLayoutManager(context)
        lockListRecyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.HORIZONTAL
            )
        )

        lockListRecyclerViewAdapter = LockListRecyclerViewAdapter()
        lockListRecyclerView.adapter = lockListRecyclerViewAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lockListRecyclerViewAdapter.scanResultList.clear()
        lockListRecyclerViewAdapter.notifyDataSetChanged()

        auto_connect_Button.setOnClickListener {
            autoConnect("BKBFMLNAFBI")
        }

        BLEHelper.getInstance().startScan(requireContext(), null, object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Timber.i("failed: $errorCode")
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (isNotConnected) {
                    if (result != null && result.device.name!=null) {
                        Timber.i("discover name: ${result.device.name}, address: ${result.device.address}, rssi: ${result.rssi}")
                        var isNewDevice = true
                        for (position in 0 until lockListRecyclerViewAdapter.scanResultList.size) {
                            if (lockListRecyclerViewAdapter.scanResultList[position].device.name == result.device.name) {
                                lockListRecyclerViewAdapter.updateRssi(position, result)
                                isNewDevice = false
                                break
                            }
                        }
                        if (isNewDevice) {
                            // Add new locks
                            lockListRecyclerViewAdapter.updateList(result)
                        }
                    }
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Timber.i(results.toString())
            }
        })

        isNotConnected = true
    }

    inner class LockListRecyclerViewAdapter : RecyclerView.Adapter<LockListRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.lock_list_recycler_view_holder, parent, false))

        var scanResultList = CopyOnWriteArrayList<ScanResult>()
        fun updateList(result: ScanResult) {
            Timber.i("add lock ${result.device.name}")
            requireActivity().runOnUiThread {
                scanResultList.add(result)
                notifyDataSetChanged()
            }
        }

        fun updateRssi(position: Int, result: ScanResult) {
            Timber.i("update ${result.device.name} rssi: ${result.rssi} ")
            requireActivity().runOnUiThread {
                scanResultList[position] = result
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = scanResultList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.lockNameTextView.text = scanResultList[position].device.name
            holder.lockMacTextView.text = scanResultList[position].device.address
            holder.lockRSSITextView.text = scanResultList[position].rssi.toString()
            holder.itemView.setOnClickListener {
                connect(scanResultList[position])
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val lockNameTextView: TextView = view.findViewById(R.id.lock_name_textView)
            val lockMacTextView: TextView = view.findViewById(R.id.lock_mac_textView)
            val lockRSSITextView: TextView = view.findViewById(R.id.lock_rssi_textView)
        }
    }

    private fun connect(result: ScanResult) {

        Timber.i("Try to connect: ${result.device.name},  ${result.device.address}")
        var isPushed = false
        val callbackConnected = {
            val destination =
                LockListFragmentDirections.actionLockListFragmentToLockFragment(result)
            findNavController().navigate(destination)
            isPushed = true
        }

        val callbackDisconnected = {
            if (isPushed) {
                findNavController().popBackStack()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireActivity(), "Lock disconnected", Toast.LENGTH_LONG).show()
                }
            }
        }

        BLEHelper.getInstance().connectBLE(
            requireContext(),
            result.device,
            callbackConnected,
            callbackDisconnected)
    }

    private var isNotConnected = true
    private fun autoConnect(lockName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            while (isNotConnected) {
                for (result in lockListRecyclerViewAdapter.scanResultList) {
                    if (result.device.name == lockName) {
                        connect(result)
                        isNotConnected = false
                        break
                    }
                }
            }
        }
    }
}