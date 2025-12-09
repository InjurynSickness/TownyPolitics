package com.orbismc.townyPolitics.initialization;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.commands.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class CommandInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public CommandInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "CommandInit");
    }

    public void initialize() {
        try {
            // Create command executors for nations - Pass context to distinguish nation vs town
            GovernmentCommand nationGovCommand = new GovernmentCommand(plugin, "nation");
            OverviewCommand nationOverviewCommand = new OverviewCommand(plugin, plugin.getGovManager(),
                    plugin.getAuthorityManager(), plugin.getDecadenceManager());

            // Create command executors for towns - Pass context to distinguish nation vs town
            GovernmentCommand townGovCommand = new GovernmentCommand(plugin, "town");
            TownOverviewCommand townOverviewCommand = new TownOverviewCommand(plugin, plugin.getTownGovManager(),
                    plugin.getTownAuthorityManager(), plugin.getTownDecadenceManager());

            // Create command executors for policies
            PolicyCommand townPolicyCommand = new PolicyCommand(plugin, plugin.getPolicyManager(), "town");
            PolicyCommand nationPolicyCommand = new PolicyCommand(plugin, plugin.getPolicyManager(), "nation");

            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "policy", nationPolicyCommand);

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "overview", townOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "o", townOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "policy", townPolicyCommand);

            // Register vassalage command
            VassalageCommand vassalageCommand = new VassalageCommand(plugin, plugin.getVassalageManager());
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "vassal", vassalageCommand);

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(plugin, plugin.getGovManager());

            logger.info("All commands registered");
        } catch (Exception e) {
            logger.severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}