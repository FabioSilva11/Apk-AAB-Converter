package com.fabiosilva.packconvert.fragment

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.lifecycle.lifecycleScope
import com.android.bundle.Config
import com.fabiosilva.packconvert.adapter.MetaDataAdapter
import com.fabiosilva.packconvert.convert.ApkToAABConverter
import com.fabiosilva.packconvert.convert.Logger
import com.fabiosilva.packconvert.databinding.DialogApkToAabBinding
import com.fabiosilva.packconvert.dialog.AddMetaFileDialog
import com.fabiosilva.packconvert.extension.contentResolver
import com.fabiosilva.packconvert.extension.runOnUiThread
import com.fabiosilva.packconvert.extension.toast
import com.fabiosilva.packconvert.model.MetaData
import com.fabiosilva.packconvert.util.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

//ToDo: implement default gradle configs fully
class ApkToAABDialogFragment : BaseDialogFragment<DialogApkToAabBinding>() {

    private lateinit var mTempDir: Path
    private lateinit var mTempInputPath: Path
    private lateinit var mTempOutputPath: Path
    private lateinit var mConfigPath: Path
    private lateinit var mMetaDataPath: Path
    private var mAABUri: Uri? = null
    private var mApkUri: Uri? = null
    private var mConfigUri: Uri? = null
    private var mMetaDataUri: Uri? = null
    private val mMetaData: MutableList<MetaData> = ArrayList()
    private var mLogger: Logger? = null

    // Valores padrão usados pelo AGP
    private val mFilesNotToCompress = listOf(
        "**.3[gG]2",
        "**.3[gG][pP]",
        "**.3[gG][pP][pP]",
        "**.3[gG][pP][pP]2",
        "**.[aA][aA][cC]",
        "**.[aA][mM][rR]",
        "**.[aA][wW][bB]",
        "**.[gG][iI][fF]",
        "**.[iI][mM][yY]",
        "**.[jJ][eE][tT]",
        "**.[jJ][pP][eE][gG]",
        "**.[jJ][pP][gG]",
        "**.[mM]4[aA]",
        "**.[mM]4[vV]",
        "**.[mM][iI][dD]",
        "**.[mM][iI][dD][iI]",
        "**.[mM][kK][vV]",
        "**.[mM][pP]2",
        "**.[mM][pP]3",
        "**.[mM][pP]4",
        "**.[mM][pP][eE][gG]",
        "**.[mM][pP][gG]",
        "**.[oO][gG][gG]",
        "**.[oO][pP][uU][sS]",
        "**.[pP][nN][gG]",
        "**.[rR][tT][tT][tT][lL]",
        "**.[sS][mM][fF]",
        "**.[tT][fF][lL][iI][tT][eE]",
        "**.[wW][aA][vV]",
        "**.[wW][eE][bB][mM]",
        "**.[wW][eE][bB][pP]",
        "**.[wW][mM][aA]",
        "**.[wW][mM][vV]",
        "**.[xX][mM][fF]"
    ).shuffled()

    private val mResultLauncherSelectApk = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        if (it != null) {
            val name: String = Utils.queryName(contentResolver, it)
            if (name.endsWith(".apk")) {
                mApkUri = it
                binding.tietApkPath.setText(name)
            } else {
                toast("O nome do arquivo deve terminar com .apk ou um arquivo com o mesmo nome já existe")
            }
        }
    }
    private val mResultLauncherSelectAABPath =
        registerForActivityResult(CreateDocument("application/octet-stream")) {
            if (it != null) {
                val name: String = Utils.queryName(contentResolver, it)
                if (name.endsWith(".aab")) {
                    mAABUri = it
                    binding.tietAabPath.setText(name)
                } else {
                    toast("O arquivo selecionado não é um arquivo AAB (.aab)")
                }
            }
        }
    private val mResultLauncherSelectConfig =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val name: String = Utils.queryName(contentResolver, it)
                if (name.endsWith(".json")) {
                    mConfigUri = it
                    binding.tietConfigPath.setText(name)
                } else {
                    toast("O arquivo selecionado não é um arquivo JSON")
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dirPath = requireContext().cacheDir.absolutePath + "/temp"
        mTempDir = Paths.get(dirPath)
        Files.createDirectories(mTempDir)
        mTempInputPath = Paths.get(dirPath, "input.apk")
        mTempOutputPath = Paths.get(dirPath, "output.aab")
        mConfigPath = Paths.get(dirPath, "BundleConfig.json")
        mMetaDataPath = Paths.get(dirPath, "meta-data")
        binding.rvMetaFiles.adapter = MetaDataAdapter(mMetaData)
        binding.btnApkToAab.setOnClickListener {
            when {
                mAABUri == null -> toast("A entrada não pode estar vazia")
                mApkUri == null -> toast("A saída não pode estar vazia")
                else -> startApkToAAB()
            }
        }

        binding.tilAabPath.setEndIconOnClickListener {
            var name = if (mApkUri == null) {
                "desconhecido.???"
            } else {
                Utils.queryName(contentResolver, mApkUri!!)
            }
            name = name.substring(0, name.lastIndexOf("."))
            mResultLauncherSelectAABPath.launch("$name.aab")
        }

        binding.tilApkPath.setEndIconOnClickListener {
            mResultLauncherSelectApk.launch("*/*")
        }

        binding.tilConfigPath.setEndIconOnClickListener {
            mResultLauncherSelectConfig.launch("*/*")
        }
        binding.btnAddMetaFile.setOnClickListener {
            AddMetaFileDialog.newInstance()
                .show(childFragmentManager, AddMetaFileDialog::class.simpleName)
        }
    }

    fun addMetaData(metaData: MetaData) {
        mMetaData.add(metaData)
        binding.rvMetaFiles.adapter!!.notifyItemInserted(mMetaData.size - 1)
        toast(metaData.toString())
    }

    private fun startApkToAAB() {
        val errorHandler =
            CoroutineExceptionHandler { _, throwable ->
                mLogger!!.add(throwable.toString())
                showErrorDialog(throwable.toString())
                doFinallyAfterConvert()
            }
        lifecycleScope.launch(errorHandler) {
            isCancelable = false
            ((binding.root.getChildAt(0) as ViewGroup)).apply {
                removeAllViews()
                addView(ProgressBar(requireContext()))
                val logTv = TextView(requireContext())
                addView(logTv)
                mLogger = Logger()
                mLogger!!.setLogListener { log ->
                    runOnUiThread { logTv.append(log + "\n") }
                }
            }
            convert()
            toast("Conversão de APK para AAB concluída com sucesso")
            doFinallyAfterConvert()
        }
    }

    private suspend fun convert() = withContext(Dispatchers.Default) {
        Utils.copy(requireContext(), mApkUri!!, mTempInputPath)
        mConfigUri?.let { Utils.copy(requireContext(), it, mConfigPath) }
        mMetaDataUri?.let { Utils.copy(requireContext(), it, mMetaDataPath) }
        ApkToAABConverter.Builder(
            requireContext(),
            mTempInputPath,
            mTempOutputPath
        ).apply {
            setLogger(mLogger)
            setVerbose(binding.cbVerbose.isChecked)
            setBundleConfig(getBundleConfig())
            mMetaData.forEach(this::addMetaData)
            val signOptionsFragment =
                childFragmentManager.findFragmentByTag("SignOptionsFragment") as SignOptionsFragment
            setSigningCertInfo(signOptionsFragment.getSigningCertInfo())
            build().start()
            Utils.copy(requireContext(), mTempOutputPath, mAABUri!!)
        }
    }

    private suspend fun getBundleConfig() = withContext(Dispatchers.IO) {
        var bundleConfig: Config.BundleConfig? = null
        mConfigUri?.let { uri ->
            contentResolver.openInputStream(uri).use {
                bundleConfig = Config.BundleConfig.parseFrom(it)
            }
        }
        if (bundleConfig == null) bundleConfig = Config.BundleConfig.newBuilder().build()
        if (binding.cbDefaultGradleConfig.isChecked) {
            val compressionBuilder = Config.Compression.newBuilder()
            val compression = compressionBuilder.addAllUncompressedGlob(mFilesNotToCompress).build()
            bundleConfig = bundleConfig!!.toBuilder().mergeCompression(compression).build()
        }
        bundleConfig

    }

    companion object {
        @JvmStatic
        fun newInstance(): ApkToAABDialogFragment = ApkToAABDialogFragment()
    }
}