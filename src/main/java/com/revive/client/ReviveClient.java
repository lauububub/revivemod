package com.revive.client;

import com.revive.network.OpenReviveScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ReviveClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenReviveScreenPayload.TYPE,
                (payload, context) -> context.client().gui.setScreen(new ReviveScreen(payload.players())));
    }
}
