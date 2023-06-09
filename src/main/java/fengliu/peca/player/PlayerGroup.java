package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fengliu.peca.util.PlayerUtil;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

public class PlayerGroup implements IPlayerGroup {
    private static final List<PlayerGroup> groups = new ArrayList<>();
    public final String groupName;
    protected int groupAmount = 0;
    private final List<EntityPlayerMPFake> bots = new ArrayList<>();

    public PlayerGroup(CommandContext<ServerCommandSource> context, GameMode mode,  Vec3d[] formationPos){
        this.groupName = StringArgumentType.getString(context, "name");
        if (formationPos == null){
            for (int index = 1; index < IntegerArgumentType.getInteger(context, "amount") + 1; index++){
                this.add(PlayerUtil.spawn(this.groupName + "_" + index, context.getSource().getPosition(), mode, context));
            }
        } else {
            for (int index = 1; index < IntegerArgumentType.getInteger(context, "amount") + 1; index++){
                this.add(PlayerUtil.spawn(this.groupName + "_" + index, formationPos[index-1], mode, context));
            }
        }
        PlayerGroup.groups.add(this);
    }

    interface Arg<T>
    {
        T get() throws CommandSyntaxException;
    }

    private static <T> T getArg(Arg<T> arg) throws CommandSyntaxException{
        try{
            return arg.get();
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public static int createGroup(CommandContext<ServerCommandSource> context, Vec3d[] formationPos) throws CommandSyntaxException {
        new PlayerGroup(context, getArg(() -> GameModeArgumentType.getGameMode(context, "gamemode")), formationPos);
        return Command.SINGLE_SUCCESS;
    }

    public static int createGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        new PlayerGroup(context, getArg(() -> GameModeArgumentType.getGameMode(context, "gamemode")), null);
        return Command.SINGLE_SUCCESS;
    }

    public static PlayerGroup getGroup(String groupName){
        for (PlayerGroup group: groups) {
            if (!group.getName().equals(groupName)){
                continue;
            }
            return group;
        }
        return null;
    }

    @Override
    public void add(EntityPlayerMPFake bot) {
        if (bot == null){
            return;
        }

        this.groupAmount++;
        IPlayerGroup.super.add(bot);
    }

    @Override
    public List<EntityPlayerMPFake> getBots() {
        return this.bots;
    }

    @Override
    public void kill() {
        IPlayerGroup.super.kill();
        groups.remove(this);
    }

    @Override
    public String getName() {
        return this.groupName;
    }

    @Override
    public int getAmount() {
        return this.groupAmount;
    }

    public enum FormationType{
        COLUMN((amount, pos, direction, row, formationPos) -> {
            for (int index = 0; index < amount; index++){
                formationPos[index] = pos.offset(direction, index);
            }
            return formationPos;
        }),

        ROW(((amount, pos, direction, row, formationPos) -> {
           if (direction == Direction.NORTH){
               for (int index = 0; index < amount; index++){
                   formationPos[index] = pos.offset(Direction.EAST, index);
               }
           } else if (direction == Direction.WEST){
               for (int index = 0; index < amount; index++){
                   formationPos[index] = pos.offset(Direction.NORTH, index);
               }
           } else if (direction == Direction.EAST){
               for (int index = 0; index < amount; index++){
                   formationPos[index] = pos.offset(Direction.SOUTH, index);
               }
           } else{
               for (int index = 0; index < amount; index++){
                   formationPos[index] = pos.offset(Direction.WEST, index);
               }
           }
            return formationPos;
        })),

        QUADRANGLE((amount, pos, direction, row, formationPos) -> {
            int rowSize = (int) Math.ceil((double) (amount / row));
            int rowIn = 0;
            int rowIndex = 0;

            Direction rowDirection;
            if (direction == Direction.NORTH){
                rowDirection = Direction.EAST;
            } else if (direction == Direction.WEST){
                rowDirection = Direction.NORTH;
            } else if (direction == Direction.EAST){
                rowDirection = Direction.SOUTH;
            } else{
                rowDirection = Direction.WEST;
            }

            for (int index = 0; index < amount; index++){
                if (index % rowSize == 0){
                    rowIn++;
                    rowIndex = 0;
                }

                formationPos[index] = pos.offset(rowDirection, rowIndex).offset(direction, rowIn);
                rowIndex++;
            }
            return formationPos;
        });

        private interface Formation{
            Vec3d[] get(int amount, Vec3d pos, Direction direction, int row, Vec3d[] formationPos);
        }

        private final Formation formation;

        FormationType(Formation formation){
            this.formation = formation;
        }

        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context, Direction direction, int row){
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return this.formation.get(amount, context.getSource().getPosition(), direction, row, new Vec3d[amount]);
        }

        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context, Direction direction){
            return this.getFormationPos(context, direction, 0);
        }
    }
}
