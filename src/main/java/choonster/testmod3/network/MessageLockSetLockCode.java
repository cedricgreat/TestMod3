package choonster.testmod3.network;

import choonster.testmod3.TestMod3;
import choonster.testmod3.api.capability.lock.ILock;
import choonster.testmod3.capability.lock.CapabilityLock;
import choonster.testmod3.client.gui.GuiLock;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.LockCode;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/**
 * Sent to the server by {@link GuiLock} to set the new lock code.
 *
 * @author Choonster
 */
public class MessageLockSetLockCode implements IMessage {
	private BlockPos pos;
	private boolean hasFacing;
	private EnumFacing facing;
	private String lockCode;

	@SuppressWarnings("unused")
	public MessageLockSetLockCode() {
	}

	public MessageLockSetLockCode(final BlockPos pos, @Nullable final EnumFacing facing, final String lockCode) {
		this.pos = pos;
		this.facing = facing;
		this.lockCode = lockCode;
		this.hasFacing = facing != null;
	}

	/**
	 * Convert from the supplied buffer into your specific message type
	 *
	 * @param buf The buffer
	 */
	@Override
	public void fromBytes(final ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		hasFacing = buf.readBoolean();

		if (hasFacing) {
			facing = EnumFacing.byIndex(buf.readUnsignedByte());
		}

		lockCode = ByteBufUtils.readUTF8String(buf);
	}

	/**
	 * Deconstruct your message into the supplied byte buffer
	 *
	 * @param buf The buffer
	 */
	@Override
	public void toBytes(final ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeBoolean(hasFacing);

		if (hasFacing) {
			buf.writeByte(facing.getIndex());
		}

		ByteBufUtils.writeUTF8String(buf, lockCode);
	}

	public static class Handler implements IMessageHandler<MessageLockSetLockCode, IMessage> {

		/**
		 * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
		 * is needed.
		 *
		 * @param message The message
		 * @param ctx     The message context
		 * @return an optional return message
		 */
		@Nullable
		@Override
		public IMessage onMessage(final MessageLockSetLockCode message, final MessageContext ctx) {
			TestMod3.proxy.getThreadListener(ctx).addScheduledTask(() -> {
				final EntityPlayerMP player = (EntityPlayerMP) TestMod3.proxy.getPlayer(ctx);
				final World world = player.world;

				player.markPlayerActive();

				if (world.isBlockLoaded(message.pos)) {
					final ILock lock = CapabilityLock.getLock(world, message.pos, message.facing);
					if (lock != null) {
						if (lock.isLocked()) {
							player.sendMessage(new TextComponentTranslation("testmod3:lock.already_locked"));
						}
						lock.setLockCode(new LockCode(message.lockCode));
					}
				}
			});

			return null;
		}
	}
}
