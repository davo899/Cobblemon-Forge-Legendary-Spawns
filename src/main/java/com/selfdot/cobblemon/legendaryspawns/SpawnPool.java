package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.SpawnLocationSelector;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.SpawnSafetyCondition;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static com.selfdot.cobblemon.legendaryspawns.ChatColourUtils.formattedAnnouncement;

public class SpawnPool {

    private static final List<String> REQUIRED_MEMBERS = List.of(
        ConfigKey.DISPLAY_NAME,
        ConfigKey.SPAWN_ANNOUNCEMENT,
        ConfigKey.CAPTURE_ANNOUNCEMENT,
        ConfigKey.SPAWN_CHANCE,
        ConfigKey.SPAWN_LIST_FILENAME
    );

    private String displayName;
    private String spawnAnnouncement;
    private String captureAnnouncement;
    private float spawnChance;
    private List<LegendarySpawn> spawns;
    private ObservableSubscription<PokemonCapturedEvent> captureListener;

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public static SpawnPool loadSpawnPool(JsonObject jsonObject) {
        if (REQUIRED_MEMBERS.stream().anyMatch(required -> !jsonObject.has(required))) {
            LogUtils.getLogger().error("Missing property in spawn pool");
            return null;
        }

        SpawnPool spawnPool = new SpawnPool();
        String spawnListFilename;
        try {
            spawnPool.displayName = jsonObject.get(ConfigKey.DISPLAY_NAME).getAsString();
            spawnPool.spawnAnnouncement = jsonObject.get(ConfigKey.SPAWN_ANNOUNCEMENT).getAsString();
            spawnPool.captureAnnouncement = jsonObject.get(ConfigKey.CAPTURE_ANNOUNCEMENT).getAsString();
            spawnPool.spawnChance = jsonObject.get(ConfigKey.SPAWN_CHANCE).getAsFloat();
            spawnListFilename = jsonObject.get(ConfigKey.SPAWN_LIST_FILENAME).getAsString();

        } catch (ClassCastException e) {
            LogUtils.getLogger().error("Incorrectly formatted property in spawn pool");
            e.printStackTrace();
            return null;
        }

        spawnPool.spawns = loadSpawnList("config/legendaryspawns/" + spawnListFilename);
        return spawnPool;
    }

    private static List<LegendarySpawn> loadSpawnList(String filename) {
        List<LegendarySpawn> spawnList = new ArrayList<>();
        try {
            File legendarySpawnListFile = new File(filename);
            Scanner configReader = new Scanner(legendarySpawnListFile);
            while (configReader.hasNextLine()) {
                String line = configReader.nextLine();
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                Species species = PokemonSpecies.INSTANCE.getByName(parts[0]);
                if (species == null) continue;

                int level;
                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                spawnList.add(new LegendarySpawn(species, level));
            }
            configReader.close();

            if (spawnList.isEmpty()) LogUtils.getLogger().warn("Spawn list " + filename + " is empty");
            return spawnList;

        } catch (FileNotFoundException e) {
            LogUtils.getLogger().error("Spawn list " + filename + " not found");
            return new ArrayList<>();
        }
    }

    private static void logSkippingSpawn(String reason) {
        LogUtils.getLogger().warn("Skipping spawn: " + reason);
    }

    public void startCaptureListener(MinecraftServer server) {
        captureListener = CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, (pokemonCapturedEvent) -> {
            Pokemon caught = pokemonCapturedEvent.component1();
            ServerPlayer player = pokemonCapturedEvent.component2();
            if (spawns.stream().anyMatch(spawn -> spawn.species == caught.getSpecies())) {
                server.getPlayerList().broadcastSystemMessage(
                    formattedAnnouncement(captureAnnouncement, this, caught, player),
                    false
                );
            }
            return Unit.INSTANCE;
        });
    }

    public void stopCaptureListener() {
        captureListener.unsubscribe();
    }

    public void attemptSpawn(
        MinecraftServer server,
        SpawnLocationSelector spawnLocationSelector,
        List<SpawnSafetyCondition> spawnSafetyConditions,
        int minimumRequiredPlayers,
        int maximumSpawnAttempts,
        int shinyOdds
    ) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < minimumRequiredPlayers) return;

        // Spawn chance increases by 2% for each player above the minimum required players
        if (Math.random() > (spawnChance + (0.02f * (server.getPlayerCount() - minimumRequiredPlayers)))) return;

        Optional<LegendarySpawn> chosenLegendaryOpt = spawns.stream()
            .skip((int) (spawns.size() * Math.random()))
            .findFirst();

        LegendarySpawn chosenLegendary;
        if (chosenLegendaryOpt.isPresent())
            chosenLegendary = chosenLegendaryOpt.get();
        else {
            logSkippingSpawn("Empty spawn list");
            return;
        }

        int attemptedSpawns = 0;
        Level spawnLevel;
        Vec3 spawnPos;
        ServerPlayer chosenPlayer;

        while (true) {
            if (++attemptedSpawns > maximumSpawnAttempts) {
                logSkippingSpawn("Could not find safe spawn location after " + maximumSpawnAttempts + " attempts");
                return;
            }

            Optional<ServerPlayer> chosenPlayerOpt = players.stream()
                .filter(player -> player.level.dimension() == Level.OVERWORLD)
                .skip((int) (players.size() * Math.random()))
                .findFirst();

            if (chosenPlayerOpt.isPresent()) {
                chosenPlayer = chosenPlayerOpt.get();
                final Level chosenPlayerSpawnLevel = chosenPlayer.level;
                final Vec3 spawnLocation = spawnLocationSelector.getSpawnLocation(
                    chosenPlayer.level, chosenPlayer.getPosition(0f)
                );
                if (spawnLocation == null) continue;
                BlockPos finalSpawnPos = new BlockPos(spawnLocation);
                if (spawnSafetyConditions.stream().anyMatch(condition -> !condition.isSafe(chosenPlayerSpawnLevel, finalSpawnPos))) continue;
                spawnPos = spawnLocation;
                spawnLevel = chosenPlayerSpawnLevel;
                break;
            }
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(chosenLegendary.species);
        pokemon.setLevel(chosenLegendary.level);
        pokemon.initializeMoveset(true);
        if (Math.random() < (1d / shinyOdds)) pokemon.setShiny(true);

        PokemonEntity pokemonEntity = new PokemonEntity(spawnLevel, pokemon, CobblemonEntities.POKEMON.get());
        pokemonEntity.setDespawner(LegendaryDespawner.getInstance());
        pokemonEntity.setPos(spawnPos);
        LightingStriker.getInstance().addTracked(pokemonEntity);
        spawnLevel.getChunkAt(new BlockPos(spawnPos));
        spawnLevel.addFreshEntity(pokemonEntity);

        server.getPlayerList().broadcastSystemMessage(
            formattedAnnouncement(spawnAnnouncement, this, pokemonEntity.getPokemon(), chosenPlayer), false
        );
    }

}
