package dev.example.ticketbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;
import java.util.Set;

public class TicketBot implements ClientModInitializer {
    static final String CLAIM_TEXT = "claim";     // ANPASSEN: Text des Claim-Buttons
    static final String REFRESH_TEXT = "refresh";
    static final String SCREEN_TITLE = "players help players";

    static boolean enabled = false;
    static int cooldown = 0;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("ticketbot", "main"));
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.ticketbot.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat));
        KeyBinding dump = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.ticketbot.dump", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggle.wasPressed()) {
                enabled = !enabled;
                if (mc.player != null)
                    mc.player.sendMessage(Text.literal("TicketBot " + (enabled ? "AN" : "AUS")), true);
            }
            while (dump.wasPressed()) {
                if (mc.player == null) continue;
                if (mc.currentScreen == null) {
                    mc.player.sendMessage(Text.literal("[TicketBot] Kein Menue offen"), false);
                    continue;
                }
                Screen s = mc.currentScreen;
                mc.player.sendMessage(Text.literal("[TicketBot] Titel: " + s.getTitle().getString()), false);
                for (ClickableWidget w : widgetsOf(s))
                    mc.player.sendMessage(Text.literal(" - '" + w.getMessage().getString()
                        + "' aktiv=" + w.active), false);
            }
            if (!enabled || mc.currentScreen == null) return;
            if (cooldown-- > 0) return;

            Screen screen = mc.currentScreen;
            if (!screen.getTitle().getString().toLowerCase().contains(SCREEN_TITLE)) return;

            Set<ClickableWidget> widgets = widgetsOf(screen);

            ClickableWidget claim = find(widgets, CLAIM_TEXT);
            if (claim != null) {
                click(claim);
                cooldown = 10;
                return;
            }
            ClickableWidget refresh = find(widgets, REFRESH_TEXT);
            if (refresh != null) {
                click(refresh);
                cooldown = 40;
            }
        });
    }

    static Set<ClickableWidget> widgetsOf(Screen screen) {
        Set<ClickableWidget> out = new LinkedHashSet<>();
        out.addAll(Screens.getButtons(screen));
        collect(screen, out);
        return out;
    }

    static void collect(Element e, Set<ClickableWidget> out) {
        if (e instanceof ClickableWidget w) out.add(w);
        if (e instanceof ParentElement p)
            for (Element child : p.children()) collect(child, out);
    }

    static ClickableWidget find(Set<ClickableWidget> list, String text) {
        for (ClickableWidget w : list)
            if (w.active && w.getMessage().getString().toLowerCase().contains(text))
                return w;
        return null;
    }

    static void click(ClickableWidget w) {
        double x = w.getX() + w.getWidth() / 2.0;
        double y = w.getY() + w.getHeight() / 2.0;
        Click c = new Click(x, y, new MouseInput(0, 0));
        w.mouseClicked(c, false);
        w.mouseReleased(c);
    }
}
