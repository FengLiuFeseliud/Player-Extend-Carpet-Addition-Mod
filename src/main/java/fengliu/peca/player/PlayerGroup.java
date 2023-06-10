package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fengliu.peca.util.PlayerUtil;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
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

    public PlayerGroup(CommandContext<ServerCommandSource> context, Vec3d[] formationPos) throws CommandSyntaxException {
        this.groupName = StringArgumentType.getString(context, "name");
        GameMode mode = getArgOrDefault(() -> GameModeArgumentType.getGameMode(context, "gamemode"), null);

        if (formationPos == null){
            Vec3d pos = getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, ""), context.getSource().getPosition());
            for (int index = 1; index < IntegerArgumentType.getInteger(context, "amount") + 1; index++){
                this.add(PlayerUtil.spawn(this.groupName + "_" + index, pos, mode, context));
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

    private static <T> T getArgOrDefault(Arg<T> arg, T defaultValue) throws CommandSyntaxException{
        try{
            return arg.get();
        } catch (IllegalArgumentException e){
            return defaultValue;
        }
    }

    public static int createGroup(CommandContext<ServerCommandSource> context, Vec3d[] formationPos) throws CommandSyntaxException {
        new PlayerGroup(context, formationPos);
        return Command.SINGLE_SUCCESS;
    }

    public static int createGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        new PlayerGroup(context, null);
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
        COLUMN("column", (amount, pos, direction, row, interstice, formationPos) -> {
            for (int index = 0; index < amount; index++){
                formationPos[index] = pos.offset(direction, addInterstice(index, interstice));
            }
            return formationPos;
        }),

        COLUMN_FOLD("columnFold", (amount, pos, direction, row, interstice, formationPos) -> {
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            for (int index = 0; index < amount; index++){
                if (index % rowSize == 0){
                    rowIndex = 0;
                }
                formationPos[index] = pos.offset(direction, addInterstice(rowIndex, interstice));
                rowIndex++;
            }
            return formationPos;
        }),

        ROW("row", ((amount, pos, direction, row, interstice, formationPos) -> {
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++){
                formationPos[index] = pos.offset(rowDirection, addInterstice(index, interstice));
            }
            return formationPos;
        })),

        ROW_FOLD("rowFold", ((amount, pos, direction, row, interstice, formationPos) -> {
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++){
                if (index % rowSize == 0){
                    rowIndex = 0;
                }
                formationPos[index] = pos.offset(rowDirection, addInterstice(rowIndex, interstice));
                rowIndex++;
            }
            return formationPos;
        })),

        QUADRANGLE("quadrangle", (amount, pos, direction, row, interstice, formationPos) -> {
            int rowIn = 0;
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++){
                if (index % rowSize == 0){
                    rowIn++;
                    rowIndex = 0;
                }

                formationPos[index] = pos.offset(rowDirection, addInterstice(rowIndex, interstice)).offset(direction, addInterstice(rowIn, interstice));
                rowIndex++;
            }
            return formationPos;
        });

        private static Direction getRowDirection(Direction direction){
            if (direction == Direction.NORTH){
                return Direction.EAST;
            } else if (direction == Direction.WEST){
                return Direction.NORTH;
            } else if (direction == Direction.EAST){
                return Direction.SOUTH;
            } else{
                return Direction.WEST;
            }
        }

        private static int addInterstice(int index, int interstice){
            return index + (interstice * index);
        }

        private interface Formation{
            Vec3d[] get(int amount, Vec3d pos, Direction direction, int row, int interstice, Vec3d[] formationPos);
        }

        public final String name;
        private final Formation formation;

        FormationType(String name, Formation formation){
            this.name = name;
            this.formation = formation;
        }

        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context, Direction direction) throws CommandSyntaxException {
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return this.formation.get(amount,
                getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, "position"), context.getSource().getPosition()),
                direction,
                getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "row"), 0),
                getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "length"), 0),
                new Vec3d[amount]);
        }

        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            PlayerEntity player = context.getSource().getPlayer();
            if (player == null){
                return null;
            }

            return this.getFormationPos(context, Direction.getLookDirectionForAxis(player, Direction.Axis.X));
        }
    }
}
