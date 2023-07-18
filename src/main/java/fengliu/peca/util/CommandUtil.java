package fengliu.peca.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;


public class CommandUtil {

    public interface Arg<T> {
        T get() throws CommandSyntaxException;
    }

    public static <T> T getArgOrDefault(Arg<T> arg, T defaultValue) {
        try{
            return arg.get();
        } catch (IllegalArgumentException | CommandSyntaxException | UnsupportedOperationException e){
            return defaultValue;
        }
    }

    public static void booleanPrintMsg(boolean bool, MutableText text, MutableText errorText, CommandContext<ServerCommandSource> context) {
        if (bool) {
            context.getSource().sendMessage(text);
        } else {
            context.getSource().sendError(errorText);
        }
    }
}
