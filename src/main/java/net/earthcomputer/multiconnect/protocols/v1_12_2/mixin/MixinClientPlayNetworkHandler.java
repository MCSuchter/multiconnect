package net.earthcomputer.multiconnect.protocols.v1_12_2.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.CommandDispatcher;
import net.earthcomputer.multiconnect.api.Protocols;
import net.earthcomputer.multiconnect.impl.ConnectionInfo;
import net.earthcomputer.multiconnect.protocols.v1_12_2.*;
import net.earthcomputer.multiconnect.protocols.v1_12_2.command.Commands_1_12_2;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.*;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.command.CommandSource;
import net.minecraft.tag.RegistryTagContainer;
import net.minecraft.tag.RegistryTagManager;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Shadow private MinecraftClient client;

    @Shadow public abstract void onSynchronizeTags(SynchronizeTagsS2CPacket packet);

    @Shadow public abstract void onSynchronizeRecipes(SynchronizeRecipesS2CPacket packet);

    @Shadow public abstract void onCommandTree(CommandTreeS2CPacket packet);

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_12_2) {
            RegistryTagManager tagManager = new RegistryTagManager();
            //noinspection ConstantConditions
            IRegistryTagManager iTagManager = (IRegistryTagManager) tagManager;
            Protocol_1_12_2 protocol = (Protocol_1_12_2) ConnectionInfo.protocol;
            toTagContainer(iTagManager.getBlocks(), protocol.getBlockTags());
            toTagContainer(iTagManager.getItems(), protocol.getItemTags());
            toTagContainer(iTagManager.getFluids(), protocol.getFluidTags());
            toTagContainer(iTagManager.getEntityTypes(), protocol.getEntityTypeTags());
            onSynchronizeTags(new SynchronizeTagsS2CPacket(tagManager));

            List<Recipe<?>> recipes = new ArrayList<>();
            List<RecipeInfo<?>> recipeInfos = protocol.getCraftingRecipes();
            for (int i = 0; i < recipeInfos.size(); i++) {
                recipes.add(recipeInfos.get(i).create(new Identifier(String.valueOf(i))));
            }
            onSynchronizeRecipes(new SynchronizeRecipesS2CPacket(recipes));

            CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
            Commands_1_12_2.register(dispatcher, null);
            onCommandTree(new CommandTreeS2CPacket(dispatcher.getRoot()));
            TabCompletionManager.requestCommandList();
        }
    }

    @SuppressWarnings("unchecked")
    @Unique
    private <T> void toTagContainer(RegistryTagContainer<T> container, Multimap<Tag<T>, T> tags) {
        ImmutableMap.Builder<Identifier, Tag<T>> map = new ImmutableMap.Builder<>();
        for (Map.Entry<Tag<T>, Collection<T>> entry : tags.asMap().entrySet()) {
            Identifier id = entry.getKey().getId();
            Tag.Builder<T> tag = Tag.Builder.create();
            entry.getValue().forEach(tag::add);
            map.put(id, tag.build(id));
        }
        ((ITagContainer<T>) container).multiconnect_setEntries(map.build());
    }

    @Inject(method = "onBlockEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/packet/BlockEntityUpdateS2CPacket;getBlockEntityType()I"))
    private void onOnBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_12_2) {
            assert client.world != null;
            BlockEntity be = client.world.getBlockEntity(packet.getPos());
            if (packet.getBlockEntityType() == 5 && be instanceof FlowerPotBlockEntity) {
                be.fromTag(packet.getCompoundTag());
            }
        }
    }

    @Inject(method = "onCommandSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onOnCommandSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_12_2) {
            if (TabCompletionManager.handleCommandList(packet))
                ci.cancel();
        }
    }

    @Inject(method = "onEntityStatus", at = @At("RETURN"))
    private void onOnEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (ConnectionInfo.protocolVersion <= Protocols.V1_12_2) {
            assert MinecraftClient.getInstance().world != null;
            if (packet.getEntity(MinecraftClient.getInstance().world) == MinecraftClient.getInstance().player
                    && packet.getStatus() >= 24 && packet.getStatus() <= 28) {
                TabCompletionManager.requestCommandList();
            }
        }
    }

}
