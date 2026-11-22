package com.maxik;

import java.util.Locale;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Макс-кристалл Края: ставится на любой блок, взрыв снимает 60 здоровья. */
public class MaxCrystalItem extends Item {
	public static final String TAG = "maxik_crystal";

	public MaxCrystalItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level) {
			BlockPos pos = context.getClickedPos();
			String cmd = String.format(Locale.ROOT,
					"summon minecraft:end_crystal %.2f %d %.2f {ShowBottom:0b,Tags:[\"%s\"]}",
					pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, TAG);
			CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
			level.getServer().getCommands().performPrefixedCommand(source, cmd);

			Player player = context.getPlayer();
			if (player == null || !player.hasInfiniteMaterials()) {
				context.getItemInHand().shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
