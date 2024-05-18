package com.fabiosilva.packconvert.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fabiosilva.packconvert.NetworkUtils
import com.fabiosilva.packconvert.R
import com.fabiosilva.packconvert.databinding.ActivityMainBinding
import com.fabiosilva.packconvert.fragment.AABToApkDialogFragment
import com.fabiosilva.packconvert.fragment.ApkToAABDialogFragment
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adView: AdView? = null
    private lateinit var customProgressDialog: CustomProgressDialog
    private var actions = "5"
    private var rewardedAd: RewardedAd? = null
    private final var TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        customProgressDialog = CustomProgressDialog(this)
        setupAds()
        setupButtons()
    }

    private fun setupAds() {
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adview1)
        adView?.loadAd(AdRequest.Builder().build())
        loadRewardedAd()
    }


    private fun loadRewardedAd() {
        // Cria uma solicitação de anúncio
        val adRequest = AdRequest.Builder().build()
        // Carrega o anúncio recompensado
        RewardedAd.load(this,"ca-app-pub-2706500450375916/7717116911", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // O carregamento do anúncio falhou
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                // O anúncio foi carregado com sucesso
                Log.d(TAG, "O anúncio foi carregado.")
                rewardedAd = ad
                customProgressDialog.dismiss()
                // Configura os callbacks para o conteúdo em tela cheia do anúncio
                rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdClicked() {
                        // O usuário clicou no anúncio
                        Log.d(TAG, "O anúncio foi clicado.")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        // O conteúdo em tela cheia do anúncio foi dispensado
                        Log.d(TAG, "O conteúdo em tela cheia do anúncio foi dispensado.")
                        rewardedAd = null
                        recompensa() // Executa a função de recompensa
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        // Falha ao exibir o conteúdo em tela cheia do anúncio
                        Log.e(TAG, "Falha ao exibir o conteúdo em tela cheia do anúncio.")
                        rewardedAd = null
                    }

                    override fun onAdImpression() {
                        // O anúncio registrou uma impressão
                        Log.d(TAG, "O anúncio registrou uma impressão.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        // O conteúdo em tela cheia do anúncio foi exibido
                        Log.d(TAG, "O conteúdo em tela cheia do anúncio foi exibido.")
                    }
                }
            }
        })
    }


    private fun setupButtons() {
        binding.btnAabToApk.setOnClickListener {
            actions = "0"
            adsCheck()
        }

        binding.btnApkToAab.setOnClickListener {
            actions = "1"
            adsCheck()
        }

    }



    private fun recompensa() {
        loadRewardedAd()
        if (!NetworkUtils.isConnected(this)) {
            // Exibir um toast quando não houver conexão com a internet
            Toast.makeText(this, "Sem conexão com a internet", Toast.LENGTH_SHORT).show()
            return
        }
        // Manipular a recompensa.
        Log.d(TAG, "Usuário ganhou a recompensa.")

        if (actions.contains("1")) {
            actions = "0"
            AABToApkDialogFragment.newInstance().show(
                supportFragmentManager, AABToApkDialogFragment::class.java.simpleName
            )
        } else {
            actions = "0"
            ApkToAABDialogFragment.newInstance().show(
                supportFragmentManager, ApkToAABDialogFragment::class.java.simpleName
            )
        }
    }

    private fun adsCheck(){
        if (rewardedAd == null) {
            customProgressDialog.show("Carregando anúncio... Aguarde.")
        } else {
            rewardedAd?.show(this, object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(rewardItem: RewardItem) {
                    // Método vazio, não faz nada com a recompensa
                }
            })
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
