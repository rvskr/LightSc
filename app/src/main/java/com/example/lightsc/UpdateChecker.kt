package com.example.lightsc

import GitHubRelease
import GitHubService
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateChecker(private val context: Context) {

    fun checkForUpdates() {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(httpClient)
            .build()

        val service = retrofit.create(GitHubService::class.java)
        val call = service.getLatestRelease("rvskr", "LightSc")

        call.enqueue(object : retrofit2.Callback<GitHubRelease> {
            override fun onResponse(
                call: retrofit2.Call<GitHubRelease>,
                response: retrofit2.Response<GitHubRelease>
            ) {
                if (response.isSuccessful) {
                    val latestRelease = response.body()
                    latestRelease?.let {
                        val latestVersion = it.tag_name
                        val currentVersion = context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).versionName

                        logVersions(currentVersion, latestVersion)

                        if (isNewVersionAvailable(currentVersion, latestVersion)) {
                            showUpdateDialog(it.assets.first().browser_download_url)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch latest release")
                }
            }

            override fun onFailure(call: retrofit2.Call<GitHubRelease>, t: Throwable) {
                Log.e(TAG, "Error: ${t.message}")
            }
        })
    }

    private fun isNewVersionAvailable(currentVersion: String, latestVersion: String): Boolean {
        // Compare version numbers to determine if a new version is available
        val currentVersionParts = currentVersion.split(".")
        val latestVersionParts = latestVersion.split(".")

        for (i in currentVersionParts.indices) {
            val currentPart = currentVersionParts[i].toIntOrNull() ?: 0
            val latestPart = latestVersionParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestPart > currentPart) {
                return true
            } else if (latestPart < currentPart) {
                return false
            }
        }
        return false
    }

    private fun showUpdateDialog(downloadUrl: String) {
        AlertDialog.Builder(context).apply {
            setTitle("Update Available")
            setMessage("A new version of the app is available. Would you like to update now?")
            setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                context.startActivity(intent)
            }
            setNegativeButton("No", null)
            create()
            show()
        }
    }

    private fun logVersions(currentVersion: String, latestVersion: String) {
        Log.d(TAG, "Current app version: $currentVersion")
        Log.d(TAG, "Latest version on GitHub: $latestVersion")
    }

    companion object {
        private const val TAG = "UpdateChecker"
    }
}

