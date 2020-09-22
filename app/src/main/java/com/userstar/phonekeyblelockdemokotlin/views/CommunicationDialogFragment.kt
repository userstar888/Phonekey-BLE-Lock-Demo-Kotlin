package com.userstar.phonekeyblelockdemokotlin.views


import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.userstar.phonekeyblelockdemokotlin.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetTextI18n")
class CommunicationDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    private lateinit var titleTextView: TextView
    private lateinit var dataTextView: TextView
    private lateinit var closeButton: Button
    var isDisconnected = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.communication_dialog_fragment, container, false)

        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        titleTextView = view.findViewById(R.id.title_TextView)
        dataTextView = view.findViewById(R.id.data_TextView)
        view.findViewById<Button>(R.id.close_Button).setOnClickListener {
            dialog?.hide()
            isShowing = false
            dataTextView.text = ""
            if (isDisconnected) {
                findNavController().popBackStack()
            }
        }
        closeButton = view.findViewById(R.id.close_Button)

        return view
    }

    override fun onResume() {
        super.onResume()
        didCreatedCallback()
    }

    private lateinit var manager: FragmentManager
    private lateinit var didCreatedCallback: () -> Unit
    fun create(manager: FragmentManager, didCreatedCallback: () -> Unit) {
        this.didCreatedCallback = didCreatedCallback
        this.manager = manager
        show(this.manager, "")
        isShowing = true
    }

    var isShowing = false
    fun show(title: String) {
        GlobalScope.launch(Dispatchers.Main) {
            closeButton.isEnabled = false
            titleTextView.text = title
            dialog?.show()
            isShowing = true
        }
    }

    fun addLine(string: String, isFinal: Boolean = false) {
        GlobalScope.launch(Dispatchers.Main) {
            dataTextView.text = "${dataTextView.text}$string\n"
            if (isFinal) {
                closeButton.isEnabled = true
            }
        }
    }
}