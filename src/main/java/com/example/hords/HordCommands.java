package com.example.hords;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class HordCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("hord")
            .requires(source -> source.hasPermissionLevel(2)) // Benötigt OP
            .then(CommandManager.literal("spawn")
                .then(CommandManager.argument("type", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("zombie");
                        builder.suggest("skeleton");
                        builder.suggest("mixed");
                        builder.suggest("zombified");
                        builder.suggest("piglin");
                        builder.suggest("magmacube");
                        builder.suggest("fortress");
                        builder.suggest("nethermixed");
                        builder.suggest("soulsand");
                        builder.suggest("ocean");
                        builder.suggest("end");
                        return builder.buildFuture();
                    })
                    .then(CommandManager.argument("size", IntegerArgumentType.integer(5, 50))
                        .executes(HordCommands::spawnHordWithSize)
                    )
                    .executes(HordCommands::spawnHordDefault)
                )
            )
            .then(CommandManager.literal("test")
                .executes(HordCommands::testHord)
            )
        );
    }
    
    private static int spawnHordWithSize(CommandContext<ServerCommandSource> context) {
        String type = StringArgumentType.getString(context, "type");
        int size = IntegerArgumentType.getInteger(context, "size");
        return executeSpawnHord(context, type, size);
    }
    
    private static int spawnHordDefault(CommandContext<ServerCommandSource> context) {
        String type = StringArgumentType.getString(context, "type");
        return executeSpawnHord(context, type, 15); // Standard-Größe
    }
    
    private static int testHord(CommandContext<ServerCommandSource> context) {
        return executeSpawnHord(context, "mixed", 10);
    }
    
    private static int executeSpawnHord(CommandContext<ServerCommandSource> context, String type, int size) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            BlockPos playerPos = player.getBlockPos();
            
            // Spawn-Position 20 Blöcke entfernt finden
            BlockPos spawnPos = findNearbySpawnPosition(player.getWorld(), playerPos);
            
            if (spawnPos == null) {
                source.sendFeedback(() -> Text.literal("Keine geeignete Spawn-Position gefunden!")
                    .formatted(Formatting.RED), false);
                return 0;
            }
            
            // Horden-Typ bestimmen
            HordsSpawner.HordType hordType;
            switch (type.toLowerCase()) {
                case "zombie":
                    hordType = HordsSpawner.HordType.ZOMBIE_HORD;
                    break;
                case "skeleton":
                    hordType = HordsSpawner.HordType.SKELETON_HORD;
                    break;
                case "mixed":
                    hordType = HordsSpawner.HordType.MIXED_HORD;
                    break;
                case "zombified":
                    hordType = HordsSpawner.HordType.ZOMBIFIED_HORD;
                    break;
                case "piglin":
                    hordType = HordsSpawner.HordType.PIGLIN_HORD;
                    break;
                case "magmacube":
                    hordType = HordsSpawner.HordType.MAGMACUBE_HORD;
                    break;
                case "fortress":
                    hordType = HordsSpawner.HordType.NETHERFORTRESS_HORD;
                    break;
                case "nethermixed":
                    hordType = HordsSpawner.HordType.NETHER_MIXED_HORD;
                    break;
                case "soulsand":
                    hordType = HordsSpawner.HordType.SOULSAND_HORD;
                    break;
                case "ocean":
                    hordType = HordsSpawner.HordType.OCEAN_HORD;
                    break;
                case "end":
                    hordType = HordsSpawner.HordType.END_HORD;
                    break;
                default:
                    source.sendFeedback(() -> Text.literal("Unknown hord type! Use: zombie, skeleton, mixed, zombified, piglin, magmacube, fortress, nethermixed, soulsand, ocean, end")
                        .formatted(Formatting.RED), false);
                    return 0;
            }
            
            // Hord spawnen
            HordsSpawner.spawnSpecificHord((net.minecraft.server.world.ServerWorld) player.getWorld(), spawnPos, hordType, size);
            
            // Erfolgsmeldung
            source.sendFeedback(() -> Text.literal("Hord spawned! Type: " + type + ", Size: " + size + ", Position: " + 
                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ())
                .formatted(Formatting.GREEN), true);
                
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Error spawning hord: " + e.getMessage())
                .formatted(Formatting.RED), false);
            return 0;
        }
    }
    
    private static BlockPos findNearbySpawnPosition(net.minecraft.server.world.ServerWorld world, BlockPos center) {
        // Prüfe welche Dimension wir haben
        if (world.getRegistryKey() == net.minecraft.world.World.NETHER) {
            return findNetherSpawnPositionForCommands(world, center.toCenterPos());
        }
        
        // Standard-Spawn für Overworld und End
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = Math.random() * 2 * Math.PI;
            int distance = 15 + (int)(Math.random() * 20); // 15-35 Blöcke entfernt
            
            int x = center.getX() + (int)(Math.cos(angle) * distance);
            int z = center.getZ() + (int)(Math.sin(angle) * distance);
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                return pos;
            }
        }
        return null;
    }
    
    private static BlockPos findNetherSpawnPositionForCommands(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.Vec3d playerPos) {
        for (int attempts = 0; attempts < 30; attempts++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = 15 + Math.random() * 20;
            
            int x = (int) (playerPos.x + Math.cos(angle) * distance);
            int z = (int) (playerPos.z + Math.sin(angle) * distance);
            
            // Nether-spezifische Y-Koordinaten-Suche (vermeidet Netherdecke)
            for (int y = 10; y < 120; y++) { // Zwischen Y=10 und Y=120 (vermeidet Lava-Seen unten und Netherdecke oben)
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos posAbove = pos.up();
                BlockPos posAbove2 = pos.up(2);
                
                // Prüfe: Boden solide, 3 Blöcke Luft darüber, nicht zu nah an Lava
                if (world.getBlockState(pos).isSolidBlock(world, pos) && 
                    world.getBlockState(posAbove).isAir() && 
                    world.getBlockState(posAbove2).isAir() &&
                    world.getBlockState(posAbove2.up()).isAir() &&
                    !isNearLavaForCommands(world, posAbove) &&
                    y < 115) { // Unter Y=115 um Netherdecke zu vermeiden
                    
                    return posAbove; // Spawne 1 Block über dem Boden
                }
            }
        }
        return null;
    }
    
    private static boolean isNearLavaForCommands(net.minecraft.server.world.ServerWorld world, BlockPos pos) {
        // Prüfe 3x3 Bereich um Position auf Lava
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).getBlock() == net.minecraft.block.Blocks.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}