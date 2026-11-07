package com.revive.client;

import com.revive.network.OpenReviveScreenPayload;
import com.revive.network.ReviveRequestPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu listing every player currently in spectator mode. Click one to revive them. */
public class ReviveScreen extends Screen {
    private static final int PER_PAGE = 6;

    private final List<OpenReviveScreenPayload.Entry> players;
    private int page = 0;

    public ReviveScreen(List<OpenReviveScreenPayload.Entry> players) {
        super(Component.translatable("screen.revive.title"));
        this.players = players;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int top = this.height / 2 - 80;

        this.addRenderableWidget(new StringWidget(0, top - 20, this.width, 9, this.title, this.font));

        if (players.isEmpty()) {
            this.addRenderableWidget(new StringWidget(0, top + 20, this.width, 9,
                    Component.translatable("screen.revive.none"), this.font));
        } else {
            int start = page * PER_PAGE;
            int end = Math.min(players.size(), start + PER_PAGE);
            for (int i = start; i < end; i++) {
                OpenReviveScreenPayload.Entry entry = players.get(i);
                int y = top + (i - start) * 24;
                this.addRenderableWidget(Button.builder(Component.literal(entry.name()), button -> {
                    ClientPlayNetworking.send(new ReviveRequestPayload(entry.id()));
                    this.onClose();
                }).bounds(centerX - 100, y, 200, 20).build());
            }

            int navY = top + PER_PAGE * 24 + 4;
            Button prev = Button.builder(Component.translatable("screen.revive.prev"), button -> {
                page--;
                this.init();
            }).bounds(centerX - 100, navY, 98, 20).build();
            prev.active = page > 0;
            this.addRenderableWidget(prev);

            Button next = Button.builder(Component.translatable("screen.revive.next"), button -> {
                page++;
                this.init();
            }).bounds(centerX + 2, navY, 98, 20).build();
            next.active = end < players.size();
            this.addRenderableWidget(next);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("screen.revive.close"), button -> this.onClose())
                .bounds(centerX - 100, top + PER_PAGE * 24 + 30, 200, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
