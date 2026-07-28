package com.ic3dwtf.nametagping;

import com.ic3dwtf.nametagping.config.PingNametagConfigManager;
import net.fabricmc.api.ClientModInitializer;

public final class PingNametagClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PingNametagConfigManager.load();
    }
}