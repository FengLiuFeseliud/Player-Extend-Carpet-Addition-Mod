package fengliu.peca.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;


public class CommandUtil {

    public interface Arg<T> {
        T get() throws CommandSyntaxException;
    }

    public static <T> T getArgOrDefault(Arg<T> arg, T defaultValue) {
        try{
            return arg.get();
        } catch (IllegalArgumentException | CommandSyntaxException e){
            return defaultValue;
        }
    }
}
