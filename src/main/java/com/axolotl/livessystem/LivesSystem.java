package com.axolotl.livessystem;

import com.axolotl.livessystem.commands.*;
import com.axolotl.livessystem.gui.ConfigGUI;
import com.axolotl.livessystem.gui.RecipeEditorGUI;
import com.axolotl.livessystem.gui.ReviveGUI;
import com.axolotl.livessystem.listeners.CraftItemListener;
import com.axolotl.livessystem.listeners.InventoryClickListener;
import com.axolotl.livessystem.listeners.PlayerDeathListener;
import com.axolotl.livessystem.listeners.PlayerJoinListener;
import com.axolotl.livessystem.listeners.PlayerInteractListener;
import com.axolotl.livessystem.managers.ItemManager;
import com.axolotl.livessystem.managers.LivesManager;
import com.axolotl.livessystem.managers.TabManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LivesSystem extends JavaPlugin {

    private static LivesSystem instance;
    private LivesManager     livesManager;
    private ItemManager      itemManager;
    private TabManager       tabManager;
    private ReviveGUI        reviveGUI;
    private RecipeEditorGUI  recipeEditorGUI;
    private CraftItemListener craftItemListener;
    private ConfigGUI         configGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.livesManager      = new LivesManager(this);
        this.itemManager       = new ItemManager(this);
        this.tabManager        = new TabManager(this);
        this.reviveGUI         = new ReviveGUI(this);
        this.recipeEditorGUI   = new RecipeEditorGUI(this);
        this.craftItemListener = new CraftItemListener(this);
        this.configGUI         = new ConfigGUI(this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this),    this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this),     this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(craftItemListener,                this);

        getCommand("lives").setExecutor(new LivesCommand(this));
        getCommand("setlives").setExecutor(new SetLivesCommand(this));
        getCommand("addlives").setExecutor(new AddLivesCommand(this));
        getCommand("removelives").setExecutor(new RemoveLivesCommand(this));
        getCommand("revivebook").setExecutor(new ReviveBookCommand(this));
        getCommand("lifetoken").setExecutor(new LifeTokenCommand(this));
        getCommand("livesreload").setExecutor(new ReloadCommand(this));
        getCommand("livesreset").setExecutor(new ResetCommand(this));
        WithdrawLifeCommand withdrawLifeCommand = new WithdrawLifeCommand(this);
        getCommand("withdrawlife").setExecutor(withdrawLifeCommand);
        getCommand("withdrawlife").setTabCompleter(withdrawLifeCommand);
        getCommand("lsconfig").setExecutor(new ConfigCommand(this));
        getCommand("editrevivebookrecipe").setExecutor(new EditRecipeCommand(this, "revivebook"));
        getCommand("editlifetokenrecipe").setExecutor(new EditRecipeCommand(this, "lifetoken"));

        getLogger().info("LivesSystem v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (livesManager != null) livesManager.saveData();
        getLogger().info("LivesSystem disabled.");
    }

    public void reload() {
        reloadConfig();
        livesManager.reload();
        itemManager.reload();
        tabManager.reload();
        craftItemListener.reloadRecipes();
    }

    public static LivesSystem getInstance()          { return instance; }
    public LivesManager getLivesManager()            { return livesManager; }
    public ItemManager  getItemManager()             { return itemManager; }
    public TabManager   getTabManager()              { return tabManager; }
    public ReviveGUI    getReviveGUI()               { return reviveGUI; }
    public RecipeEditorGUI getRecipeEditorGUI()      { return recipeEditorGUI; }
    public CraftItemListener getCraftItemListener()  { return craftItemListener; }
    public ConfigGUI         getConfigGUI()           { return configGUI; }
}
