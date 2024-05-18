package com.fabiosilva.packconvert.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fabiosilva.packconvert.NetworkUtils
import com.fabiosilva.packconvert.R
import com.fabiosilva.packconvert.databinding.ActivityMainBinding
import com.fabiosilva.packconvert.fragment.AABToApkDialogFragment
import com.fabiosilva.packconvert.fragment.ApkToAABDialogFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adView: AdView? = null
    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAds()
        setupButtons()
    }

    private fun setupAds() {
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adview1)
        adView?.loadAd(AdRequest.Builder().build())
        loadRewardedAd()
    }

    private fun setupButtons() {
        binding.btnAabToApk.setOnClickListener {
            if (NetworkUtils.isConnected(this)) {
                showRewardedAdOrAABToApkDialog()
            } else {
                showNetworkErrorDialog()
            }
        }

        binding.btnApkToAab.setOnClickListener {
            if (NetworkUtils.isConnected(this)) {
                showRewardedAdOrApkToAabDialog()
            } else {
                showNetworkErrorDialog()
            }
        }

    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            "ca-app-pub-2706500450375916/7717116911",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
            })
    }

    private fun showNetworkErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("A rede não está conectada")
            .setMessage("Você precisa estar conectado à internet para jogar.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showRewardedAdOrAABToApkDialog() {
        rewardedAd?.show(this) {
            ApkToAABDialogFragment.newInstance().show(
                supportFragmentManager, ApkToAABDialogFragment::class.java.simpleName
            )
            loadRewardedAd()
        } ?: run {
            ApkToAABDialogFragment.newInstance().show(
                supportFragmentManager, ApkToAABDialogFragment::class.java.simpleName
            )
        }
    }

    private fun showRewardedAdOrApkToAabDialog() {
        rewardedAd?.show(this) {
            ApkToAABDialogFragment.newInstance().show(
                supportFragmentManager, AABToApkDialogFragment::class.java.simpleName
            )
            loadRewardedAd()
        } ?: run {
            ApkToAABDialogFragment.newInstance().show(
                supportFragmentManager, AABToApkDialogFragment::class.java.simpleName
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Adiciona o item do GitHub
        val githubIntent = Intent(Intent.ACTION_VIEW)
        githubIntent.data = Uri.parse("https://github.com/FabioSilva11")
        menu?.add(0, 0, 0, "GitHub")
            ?.setIcon(R.drawable.ic_github)
            ?.setIntent(githubIntent)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        return super.onCreateOptionsMenu(menu)
    }
}
