package com.raisoft.grabi.di

import com.raisoft.grabi.data.vpn.TunnelBackend
import com.raisoft.grabi.data.vpn.WireGuardBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VpnModule {

    @Binds
    @Singleton
    abstract fun bindTunnelBackend(impl: WireGuardBackend): TunnelBackend
}