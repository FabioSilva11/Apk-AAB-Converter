package com.fabiosilva.packconvert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.fabiosilva.packconvert.util.Utils
import java.io.File
import java.lang.reflect.ParameterizedType


/**
 * Criado por qingyu em 27/03/2023.
 */
abstract class BaseDialogFragment<T : ViewBinding> : DialogFragment() {
    private var _binding: T? = null
    protected val binding: T
        get() = _binding ?: throw IllegalStateException("O binding está nulo.")

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val superClass = javaClass.genericSuperclass
        val bindingClass = (superClass as ParameterizedType).actualTypeArguments[0] as Class<T>
        val inflateMethod = bindingClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        _binding = inflateMethod.invoke(null, inflater, container, false) as T
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            window!!.setBackgroundDrawable(null)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            val margin = Utils.dpToPx(requireContext(), 25)
            val params = requireView().layoutParams as FrameLayout.LayoutParams
            params.setMargins(margin, margin, margin, margin)
        }
    }

    fun showErrorDialog(error: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Falha ao converter arquivo")
            .setMessage(error)
            .setPositiveButton("Cancelar", null)
            .show()
    }

    fun doFinallyAfterConvert() {
        File(requireContext().cacheDir, "temp").deleteRecursively()
        (_binding?.root as? ViewGroup)?.let {
            (it.getChildAt(0) as ViewGroup).removeViewAt(0)
        }
        isCancelable = true
    }
}
