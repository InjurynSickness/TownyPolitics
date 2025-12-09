package com.orbismc.townyPolitics.utils;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;
import com.palmergames.bukkit.towny.event.statusscreen.StatusScreenEvent;

public class EventHelper {

    public static void addComponentToScreen(StatusScreenEvent event, String componentName, Component component) {
        event.getStatusScreen().addComponentOf(componentName, component);
    }

    public static Component createHoverComponent(String text, Component hoverText, NamedTextColor color) {
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component mainText = Component.text(text).color(color);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        return Component.empty()
                .append(openBracket)
                .append(mainText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));
    }

    public static Component buildHoverText(String title, String... lines) {
        Component component = Component.text(title).color(NamedTextColor.DARK_GREEN);
        component = component.append(Component.newline()).append(Component.newline());

        for (String line : lines) {
            component = component.append(Component.text(line).color(NamedTextColor.GREEN))
                    .append(Component.newline());
        }

        return component;
    }
}