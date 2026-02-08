package com.chat.lightweight.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * 网络状态监听器
 * 监听网络连接变化，在网络恢复时触发重连
 */
class NetworkStateMonitor private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: NetworkStateMonitor? = null

        fun getInstance(context: Context): NetworkStateMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkStateMonitor(context.applicationContext).also { instance = it }
            }
        }
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var onNetworkAvailable: (() -> Unit)? = null
    private var onNetworkLost: (() -> Unit)? = null

    /**
     * 开始监听网络状态
     */
    fun startMonitoring(
        onNetworkAvailable: () -> Unit,
        onNetworkLost: () -> Unit
    ) {
        this.onNetworkAvailable = onNetworkAvailable
        this.onNetworkLost = onNetworkLost

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("Network available")
                onNetworkAvailable.invoke()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("Network lost")
                onNetworkLost.invoke()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                Timber.d("Network capabilities changed, validated: $isValidated")
            }
        }

        connectivityManager.registerNetworkCallback(
            networkRequest,
            networkCallback!!
        )

        Timber.d("Network monitoring started")
    }

    /**
     * 停止监听网络状态
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            Timber.d("Network monitoring stopped")
        }
        networkCallback = null
        onNetworkAvailable = null
        onNetworkLost = null
    }

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * 获取网络类型
     */
    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * 网络状态Flow
     */
    fun observeNetworkState(): Flow<Boolean> = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    enum class NetworkType {
        NONE, WIFI, CELLULAR, ETHERNET, OTHER
    }
}
